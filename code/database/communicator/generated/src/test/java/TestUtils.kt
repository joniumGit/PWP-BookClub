/**
 * Utility functions and useful stuff for tests
 *
 * Makes life much more simple
 */
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DAOImpl
import org.jooq.impl.DSL
import org.jooq.types.ULong
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import pwp.generated.tables.daos.*
import java.lang.annotation.Inherited
import java.lang.reflect.Method
import java.math.BigInteger
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.system.measureTimeMillis

private val verbose: Boolean by lazy {
    System.getProperty("verbose")?.let { true } ?: false
}

fun log(s: String, err: Boolean = false) {
    if (verbose) {
        if (err) {
            System.err.println(s)
        } else {
            println(s)
        }
    }
}

fun <T> T.log(): T {
    log(this.toString())
    return this
}


/**
 *  Convenience function for [ResultSet]
 */
inline fun <T : ResultSet> T.forEach(block: (T) -> Unit) {
    this.use {
        while (this.next()) {
            block(this)
        }
    }
}

/**
 *  Random [ULong]
 */
fun randomId(): ULong {
    val bytes = ByteArray(8)
    SecureRandom().nextBytes(bytes)
    return ULong.valueOf(BigInteger(1, bytes))
}

/**
 *  "Random" handle
 */
fun randomHandle(): String = randomId().toBigInteger().pow(16).toString(16).let {
    if (it.length > 63) it.substring((0..63)) else it
}

/**
 *  Holder object for database stuff
 */
object DB {

    /**
     * Raw [Connection] to the Docker database
     */
    val rawConnection: Connection by lazy {
        val user = "root"
        val pass = "test"
        val url = "jdbc:mariadb://localhost:6969/book_club"
        val conn = DriverManager.getConnection(url, user, pass)
        conn.autoCommit = false
        conn
    }

    /**
     *  Instance of [DSLContext] used across all test
     */
    val context: DSLContext by lazy { DSL.using(rawConnection, SQLDialect.MARIADB) }

    fun test() {
        rawConnection.prepareStatement("SELECT 1").use {
            it.executeQuery().forEach { rs ->
                assert(rs.getInt(1) == 1)
            }
        }
    }
}

/**
 *  Holder object for all Dao objects
 */
@Suppress("unused")
object TestDao {
    private inline fun <reified T : DAOImpl<*, *, *>> create(): T {
        val dao = T::class.createInstance()
        dao.setConfiguration(DB.context.configuration())
        return dao
    }

    val users by lazy { create<UsersDao>() }
    val clubs by lazy { create<ClubsDao>() }
    val discussions by lazy { create<DiscussionsDao>() }
    val books by lazy { create<BooksDao>() }
    val comments by lazy { create<CommentsDataDao>() }
    val reviews by lazy { create<ReviewsDao>() }
    val bookListing by lazy { create<UserBookListingDao>() }

    object Link {
        val clubUsers by lazy { create<ClubUserLinkDao>() }
        val clubBooks by lazy { create<ClubBookLinkDao>() }
        val clubDiscussions by lazy { create<ClubDiscussionLinkDao>() }
        val discussionComments by lazy { create<DiscussionCommentLinkDao>() }
        val discussionBooks by lazy { create<DiscussionBookLinkDao>() }
        val commentComments by lazy { create<CommentCommentLinkDao>() }
    }

    object Proto {
        val friends by lazy { create<FriendsDao>() }
        val friendRequest by lazy { create<FriendsRequestDao>() }
        val usersIcon by lazy { create<UserIconDao>() }
        val usersExternal by lazy { create<UsersExternalDao>() }
        val usersPassword by lazy { create<UserPasswordDao>() }
    }
}

/**
 *  Shorthand for reflectively getting a Dao from [TestDao] holder.
 */
inline fun <reified T : DAOImpl<*, *, *>> getDao(): T {
    return TestDao::class.declaredMemberProperties.firstOrNull {
        it.returnType.isSubtypeOf(T::class.starProjectedType)
    }?.get(TestDao)?.let { it as T } ?: TestDao.Link::class.declaredMemberProperties.firstOrNull {
        it.returnType.isSubtypeOf(T::class.starProjectedType)
    }?.get(TestDao.Link)?.let { it as T } ?: TestDao.Proto::class.declaredMemberProperties.firstOrNull {
        it.returnType.isSubtypeOf(T::class.starProjectedType)
    }?.get(TestDao.Proto) as T
}


/**
 * This annotation serves [DSLContext][DB.context] and [Raw DB connection][DB.rawConnection]
 * and marks classes/methods for [invocation interception][DBInterceptor]
 */
@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@ExtendWith(DSLProvider::class, RawProvider::class, DBInterceptor::class)
annotation class DBInject

@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Persist

/**
 *  Intercepts calls for [DBInject] annotated classes/methods
 */
class DBInterceptor : InvocationInterceptor {

    /**
     *  Creates and restores save points and measures execution time for test methods
     */
    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>,
        invocationContext: ReflectiveInvocationContext<Method>,
        extensionContext: ExtensionContext
    ) {
        val timed = measureTimeMillis {
            log("Setting savepoint for: ${invocationContext.executable.name}...")
            if (
                invocationContext.executable.isAnnotationPresent(Persist::class.java)
                || invocationContext.targetClass.isAnnotationPresent(Persist::class.java)
            ) {
                log("Persisting...")
                invocation.proceed()
            } else {
                val save = DB.rawConnection.setSavepoint("current-method")
                try {
                    invocation.proceed()
                } finally {
                    log("Restoring savepoint...")
                    DB.rawConnection.rollback(save)
                }
            }
        }
        if (timed > 500) {
            println(
                "Method took long to execute: $timed" +
                        "\nMethod: ${invocationContext.executable.name} " +
                        "at ${invocationContext.executable.declaringClass}"
            )
        }
    }
}

/**
 *  Resolves and tests [DB.context]
 */
class DSLProvider : TypeBasedParameterResolver<DSLContext>() {

    override fun resolveParameter(p0: ParameterContext?, p1: ExtensionContext?): DSLContext {
        DB.test()
        return DB.context
    }

}


/**
 *  Resolves and tests [DB.rawConnection]
 */
class RawProvider : TypeBasedParameterResolver<Connection>() {
    override fun resolveParameter(p0: ParameterContext?, p1: ExtensionContext?): Connection {
        DB.test()
        return DB.rawConnection
    }
}