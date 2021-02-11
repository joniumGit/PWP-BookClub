import org.jooq.DSLContext
import org.jooq.impl.TableImpl
import org.jooq.types.ULong
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pwp.generated.BookClub
import pwp.generated.tables.daos.BooksDao
import pwp.generated.tables.pojos.Books
import java.math.BigInteger
import java.sql.Connection
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


class TestClass {

    /**
     *  Just to see if everything is working
     */
    @Test
    @DBInject
    @Order(0)
    fun simpleQuery(c: Connection) {
        c.prepareStatement("SELECT 1").use {
            it.executeQuery().forEach { rs ->
                assert(rs.getInt(1) == 1)
            }
        }
    }

    /**
     *  Reflectively call a select to all tables to check that simple selects work
     */
    @Test
    @DBInject
    @Order(1)
    fun queryTables(context: DSLContext) {
        BookClub::class.memberProperties.filter {
            it.visibility == KVisibility.PUBLIC && TableImpl::class.java.isAssignableFrom(it.getter.returnType.jvmErasure.java)
        }.map {
            it.get(BookClub.BOOK_CLUB) as TableImpl<*>
        }.forEach {
            assertDoesNotThrow {
                val query = context.selectFrom(it).query
                assert(query.fetch().size == 0)
            }
        }
    }

    class QueryTables {

        private fun Books.same(other: Books): Boolean = this.id == other.id && this.handle == other.handle

        @Test
        @DBInject
        fun populate(context: DSLContext, c: Connection) {
            val id1 = BigInteger("18446744073709551615")
            val id2: Long = 1
            val b1 = Books(ULong.valueOf(id1), "Book 1", "Full Name Book 1")
            val b2 = Books(ULong.valueOf(id2), "Book 2", "Full Name Book 2")
            val dao = BooksDao(configuration = context.configuration())
            dao.insert(b1, b2)
            dao.fetchById(ULong.valueOf(id1), ULong.valueOf(id2)).forEach {
                assert(b1.same(it) || b2.same(it))
                println("Book: ${it.id} - ${it.handle}")
            }
            val fetch = context.selectFrom(BookClub.BOOK_CLUB.BOOKS).fetch()
            assert(fetch.size == 2)
            fetch.forEach {
                println("Book: ${it.id} - ${it.handle}")
            }
            c.rollback()
            assert(context.selectFrom(BookClub.BOOK_CLUB.BOOKS).fetch().size == 0)
        }

    }

}