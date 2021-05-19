package pwp.database


/**
 * Utility functions and useful stuff for tests
 *
 * Makes life much more simple
 */


import org.jooq.DSLContext
import org.junit.jupiter.api.extension.*
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.junit.platform.commons.util.AnnotationUtils
import java.lang.annotation.Inherited
import java.lang.reflect.Method
import java.sql.Connection
import kotlin.system.measureTimeMillis


/**
 * This annotation serves [DSLContext][DB.context] and [Raw DB connection][DB.rawConnection]
 * and marks classes/methods for [invocation interception][DBInterceptor]
 */
@Inherited
@Target(AnnotationTarget.CLASS)
@ExtendWith(
    DSLProvider::class,
    RawProvider::class,
    DBInterceptor::class,
    DBFinalizer::class
)
annotation class DBInject

/**
 * Prevents rollback after invocation
 */
@Inherited
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
annotation class Persist

class DBFinalizer : TestInstancePostProcessor {

    /**
     *  Rollback database after tests in a container
     */
    override fun postProcessTestInstance(instance: Any, context: ExtensionContext) {
        DB.rawConnection.rollback()
    }
}

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
        val timed: Long
        if (
            AnnotationUtils.isAnnotated(invocationContext.executable, Persist::class.java)
            || AnnotationUtils.isAnnotated(invocationContext.targetClass, Persist::class.java)
        ) {
            log("Persisting...")
            timed = measureTimeMillis {
                invocation.proceed()
            }
        } else {
            log("Setting savepoint for: ${invocationContext.executable.name}...")
            val save = DB.rawConnection.setSavepoint()
            try {
                timed = measureTimeMillis {
                    invocation.proceed()
                }
            } finally {
                log("Restoring savepoint...")
                DB.rawConnection.rollback(save)
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
