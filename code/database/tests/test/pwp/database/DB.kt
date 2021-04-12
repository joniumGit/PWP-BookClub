package pwp.database

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.sql.Connection
import java.sql.DriverManager

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
    val context: DSLContext by lazy {
        DSL.using(
            rawConnection,
            SQLDialect.MARIADB
        )
    }

    fun test() {
        rawConnection.prepareStatement("SELECT 1").use {
            it.executeQuery().use { rs ->
                rs.next()
                assert(rs.getInt(1) == 1)
            }
        }
    }

}