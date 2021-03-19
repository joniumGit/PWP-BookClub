package pwp.communicator.test.support

import log
import org.jooq.DAO
import org.jooq.impl.DAOImpl
import pwp.communicator.using
import pwp.generated.tables.daos.BooksDao
import pwp.generated.tables.daos.ClubsDao
import pwp.generated.tables.daos.UsersDao
import pwp.generated.tables.pojos.Books
import pwp.generated.tables.pojos.Clubs
import pwp.generated.tables.pojos.Users
import randomHandle
import randomId
import kotlin.random.Random

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
        val name = randomHandle().substring(0..8)
        Users(
            id = randomId(),
            username = name,
            description = "Test"
        )
    }
}

fun addBooks(count: Int): Collection<Books> {
    return add<BooksDao, Books>(count) {
        val name = randomHandle()
        Books(
            id = randomId(),
            handle = name,
            fullName = name + randomHandle(),
            pages = Random.nextInt(10, 1000)
        )
    }
}

fun addClubs(count: Int): Collection<Clubs> {
    return add<ClubsDao, Clubs>(count) {
        val name = randomHandle()
        Clubs(
            id = randomId(),
            handle = name
        )
    }
}
