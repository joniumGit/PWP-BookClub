package pwp.database.support


/**
 * Utility functions and useful stuff for tests
 *
 * Makes life much more simple
 */


import org.jooq.DAO
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DAOImpl
import org.jooq.impl.DSL
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import pwp.database.generated.tables.daos.*
import pwp.database.generated.tables.pojos.Books
import pwp.database.generated.tables.pojos.Clubs
import pwp.database.generated.tables.pojos.Users
import java.lang.annotation.Inherited
import java.lang.reflect.Method
import java.security.SecureRandom
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.system.measureTimeMillis

/**
 *  Simple holder for important stuff
 */
object Helpers {

    /**
     *  Holds dao instances
     */
    lateinit var daoMap: Map<KClass<out DAOImpl<*, *, *>>, DAOImpl<*, *, *>>
        private set

    /**
     *  Using the context to make us Dao instances
     */
    private inline fun <reified T : DAOImpl<*, *, *>> DSLContext.create(): Pair<KClass<T>, T> {
        val dao = T::class.createInstance()
        dao.setConfiguration(this.configuration())
        return Pair(T::class, dao)
    }

    /**
     *  Initializes all Dao instances for easy access with [getDao]
     */
    fun initialize(ctx: DSLContext) {
        with(ctx) {
            daoMap = mapOf(
                create<UsersDao>(),
                create<ClubsDao>(),
                create<DiscussionsDao>(),
                create<BooksDao>(),
                create<CommentsDao>(),
                create<ReviewsDao>(),
                create<UserBooksDao>(),
                // Link
                create<ClubUserLinkDao>(),
                create<ClubBookLinkDao>(),
                create<ClubDiscussionLinkDao>(),
                create<DiscussionCommentLinkDao>(),
                create<DiscussionBookLinkDao>(),
            )
        }
    }
}

/**
 *  Shorthand for getting a Dao from [Helpers.daoMap] holder.
 */
inline fun <reified T : DAOImpl<*, *, *>> getDao(): T = Helpers.daoMap[T::class] as T

/**
 * Running stuff with Dao
 */
inline fun <reified T : DAOImpl<*, *, *>> using(block: T.() -> Unit) = getDao<T>().block()

/**
 *  Running stuff with Dao
 */
inline fun <reified T : DAOImpl<*, *, *>, R> using(block: T.() -> R): R = getDao<T>().block()

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
 *  Runs stuff with savepoint
 */
fun <T> DSLContext.safe(block: () -> T?): T? {
    var exception: Exception? = null
    var result: T? = null
    this.connection {
        val sp = it.setSavepoint()
        try {
            result = block()
        } catch (e: Exception) {
            it.rollback(sp)
            exception = e
        }
    }
    if (exception != null) {
        throw exception!!
    } else {
        return result
    }
}


/**
 *  For verbose test output, i.e. everything gets spammed into std.out
 */
private val verbose: Boolean by lazy {
    System.getProperty("verbose")?.let { true } ?: false
}

/**
 *  Prints
 */
fun log(s: String, err: Boolean = false) {
    if (verbose) {
        if (err) {
            System.err.println(s)
        } else {
            println(s)
        }
    }
}

/**
 * Just prints
 */
fun <T> T.log(): T {
    log(this.toString())
    return this
}

/**
 *  Random Int
 */
fun id(): Int = SecureRandom().nextInt()

/**
 *  "Random" handle
 */
fun handle(): String = id().toBigInteger().pow(16).toString(16).let {
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

/**
 * Inefficient method for a random entry
 */
fun <T> DAO<*, T, *>.random(): T = this.findAll().random()

/**
 * Inefficiently fetches a first value
 */
fun <T> DAO<*, T, *>.first(): T = this.findAll().first()

internal inline fun <P> DAOImpl<*, P, *>.insertPrint(
    range: IntRange = (0..99),
    block: (Int) -> P
): Collection<P> {
    val data = (range).map(block).map { it.log() }.toList()
    this.insert(data)
    assert(this.findAll().size == data.size) {
        "Failed to assert that all inserted data was found"
    }
    return data
}

private inline fun <reified T : DAOImpl<*, R, *>, R> add(count: Int, block: (Int) -> R): Collection<R> {
    return if (count > 0) {
        using<T, Collection<R>> {
            insertPrint(range = (0 until count), block = block)
        }
    } else {
        emptyList()
    }
}

fun addUsers(count: Int): Collection<Users> {
    return add<UsersDao, Users>(count) {
        val name = handle().substring(0..8)
        Users(
            id = id(),
            username = name,
            description = "Test"
        )
    }
}

fun addBooks(count: Int): Collection<Books> {
    return add<BooksDao, Books>(count) {
        val name = handle()
        Books(
            id = id(),
            handle = name,
            fullName = name + handle(),
            pages = Random.nextInt(10, 1000)
        )
    }
}

fun addClubs(count: Int): Collection<Clubs> {
    return add<ClubsDao, Clubs>(count) {
        val name = handle()
        Clubs(
            id = id(),
            handle = name
        )
    }
}
