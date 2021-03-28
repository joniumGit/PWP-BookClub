package pwp.database

import org.jooq.DSLContext
import java.security.SecureRandom

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
 *  Runs stuff with savepoint
 */
fun <T> DSLContext.safe(block: DSLContext.() -> T): T {
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
        return result!!
    }
}
