@file:Suppress("unused")

package pwp.communicator

import org.jooq.DSLContext
import org.jooq.impl.DAOImpl
import pwp.generated.tables.daos.*
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

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
                create<UserBookListingDao>(),
                // Link
                create<ClubUserLinkDao>(),
                create<ClubBookLinkDao>(),
                create<ClubDiscussionLinkDao>(),
                create<DiscussionCommentLinkDao>(),
                create<DiscussionBookLinkDao>(),
                create<CommentCommentLinkDao>(),
                // Proto
                create<FriendsDao>(),
                create<FriendsRequestDao>(),
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




