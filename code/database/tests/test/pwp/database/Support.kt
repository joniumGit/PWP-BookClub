package pwp.database


/**
 * Utility functions and useful stuff for tests
 *
 * Makes life much more simple
 */


import org.jooq.DSLContext
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import java.lang.annotation.Inherited
import java.lang.reflect.Method
import java.sql.Connection
import kotlin.system.measureTimeMillis


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
