import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import java.lang.annotation.Inherited
import java.lang.reflect.Method
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.system.measureTimeMillis

inline fun <T : ResultSet> T.forEach(block: (T) -> Unit) {
    this.use {
        while (this.next()) {
            block(this)
        }
    }
}

object DB {

    val rawConnection: Connection by lazy {
        val user = "jooq"
        val pass = "jooq"
        val url = "jdbc:mariadb://localhost:6969/book_club"
        val conn = DriverManager.getConnection(url, user, pass)
        conn.autoCommit = false
        conn
    }

    val context: DSLContext by lazy {
        DSL.using(rawConnection, SQLDialect.MARIADB)
    }

    fun test() {
        rawConnection.prepareStatement("SELECT 1").use {
            it.executeQuery().forEach { rs ->
                assert(rs.getInt(1) == 1)
            }
        }
    }

    fun reset() {
        rawConnection.rollback()
    }

}

@Inherited
@ExtendWith(DSLProvider::class, RawProvider::class, DBInterceptor::class)
annotation class DBInject

class DBInterceptor : InvocationInterceptor {

    override fun interceptTestMethod(
        invocation: InvocationInterceptor.Invocation<Void>?,
        invocationContext: ReflectiveInvocationContext<Method>?,
        extensionContext: ExtensionContext?
    ) {
        val timed = measureTimeMillis {
            try {
                invocation?.proceed()
            } finally {
                DB.reset()
            }
        }
        if (timed > 500) {
            println(
                "Method took long to execute: $timed" +
                        "\nMethod: ${invocationContext?.executable?.name} " +
                        "at ${invocationContext?.executable?.declaringClass}"
            )
        }
    }
}

class DSLProvider : TypeBasedParameterResolver<DSLContext>() {

    override fun resolveParameter(p0: ParameterContext?, p1: ExtensionContext?): DSLContext {
        DB.test()
        return DB.context
    }

}

class RawProvider : TypeBasedParameterResolver<Connection>() {
    override fun resolveParameter(p0: ParameterContext?, p1: ExtensionContext?): Connection {
        DB.test()
        return DB.rawConnection
    }
}