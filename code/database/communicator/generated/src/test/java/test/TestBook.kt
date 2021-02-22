package test

import org.jooq.DSLContext
import org.junit.jupiter.api.*
import pwp.generated.tables.references.BOOKS

@Tag("books")
class TestBook : DBTest(populateClubs = false, populateUsers = false) {

    private val defaultBook by lazy { bookDao.findAll().random() }

    @Test
    @DisplayName("Asserting book count")
    fun bookCount(context: DSLContext) {
        assert(context.selectFrom(BOOKS).count() == BOOK_COUNT) {
            "Failed book count"
        }
    }

    @AfterEach
    @DisplayName("Integrity check")
    fun integrity() {
        assertDoesNotThrow {
            bookDao.fetchById(defaultBook.id!!).apply {
                assert(this.size == 1) {
                    "Failed to find default book: ${this.size}"
                }
                assert(this.first() == defaultBook) {
                    "Found something else than the default book: ${this.first().id}"
                }
                assert(this.first().deleted == 0.toByte()) {
                    "Default book was deleted: ${this.first().deleted}"
                }
            }
        }
    }

    @Test
    @DisplayName("Testing book deletion")
    fun delete() {
        assertDoesNotThrow("Failed delete operation") {
            defaultBook.deleted = 1.toByte()
            bookDao.update(defaultBook)
        }
        assert(bookDao.fetchOneById(defaultBook.id!!)!!.deleted == 1.toByte()) {
            "Failed to delete book"
        }
        assertDoesNotThrow("Failed undelete operation") {
            defaultBook.deleted = 0.toByte()
            bookDao.update(defaultBook)
        }
        assert(bookDao.fetchOneById(defaultBook.id!!)!!.deleted == 0.toByte()) {
            "Failed to undelete book"
        }
    }
}