package pwp.database

import org.jooq.DSLContext
import org.jooq.Table
import org.jooq.UpdatableRecord
import java.security.SecureRandom
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 *  For verbose test output, i.e. everything gets spammed into std.out
 */
private val verbose: Boolean by lazy {
    System.getProperty("verbose")?.let { true } ?: false
}

const val FAILED_VALID = "Failed to insert a valid entry"
const val DUPLICATE = "Allowed to insert a duplicate entry"
const val INVALID = "Allowed to insert an invalid entry"
const val FOUND_DELETED = "Found a non-existent record"

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

/**
 *  Helper for batches
 */
inline fun <T : UpdatableRecord<*>> add(
    range: IntRange,
    crossinline block: DSLContext.(Int) -> T
) = DB.context.safe {
    range.map { block(it) }
        .toList()
        .let {
            batchInsert(it).execute()
        }
}

/**
 *  Helper for inserts
 */
inline fun <T : UpdatableRecord<*>> DSLContext.add(table: Table<T>, block: T.() -> Unit): T {
    return this.newRecord(table)
        .apply(block = block)
        .let {
            assertNull(it.get("id"))
            it.insert()
            assertNotNull(it.get("id"))
            it.log()
        }
}