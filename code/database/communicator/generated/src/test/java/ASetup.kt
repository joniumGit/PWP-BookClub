import org.jooq.DSLContext
import org.jooq.impl.TableImpl
import org.junit.jupiter.api.*
import pwp.generated.BookClub
import java.sql.Connection
import kotlin.reflect.KVisibility
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure


@Tags(Tag("setup"), Tag("database"))
class ASetup {

    /**
     *  Just to see if everything is working
     */
    @Test
    @DBInject
    @Order(0)
    fun simpleQuery(c: Connection) {
        c.prepareStatement("SELECT 1").use {
            it.executeQuery().forEach { rs ->
                assert(rs.getInt(1) == 1) {
                    "Database did not answer"
                }
            }
        }
    }

    /**
     *  Reflectively call a select to all tables to check that simple selects work
     *  And that the database is empty.
     */
    @Test
    @DBInject
    @Order(1)
    fun queryTables(context: DSLContext) {
        BookClub::class.memberProperties.filter {
            it.visibility == KVisibility.PUBLIC && TableImpl::class.isSuperclassOf(it.getter.returnType.jvmErasure)
        }.map {
            it.get(BookClub.BOOK_CLUB) as TableImpl<*>
        }.forEach {
            assertDoesNotThrow {
                log("Testing table: ${it.name}")
                val query = context.selectFrom(it).query
                val size = query.fetch().size
                assert(size == 0) {
                    "Table ${it.name} contained records: $size"
                }
            }
        }
    }
}