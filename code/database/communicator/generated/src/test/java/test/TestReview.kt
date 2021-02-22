package test

import getDao
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.*
import pwp.generated.enums.UserBookListingReadingStatus
import pwp.generated.tables.daos.ReviewsDao
import pwp.generated.tables.daos.UserBookListingDao
import pwp.generated.tables.pojos.Reviews
import pwp.generated.tables.pojos.UserBookListing
import pwp.generated.tables.references.BOOKS_STATISTICS
import pwp.generated.tables.references.BOOKS_TOP_ACTIVITY
import pwp.generated.tables.references.BOOKS_TOP_RATING
import pwp.generated.tables.references.USER_BOOK_LISTING
import randomId
import java.math.BigDecimal

@Tag("reviews")
class TestReview : DBTest(populateClubs = false) {

    /**
     *  Tests review related database features
     */
    @Test
    @DisplayName("Review posting, constraints and view updates")
    fun makeReview(context: DSLContext) {
        val reviewsDao = getDao<ReviewsDao>()
        val user = userDao.findAll().first()
        val book = bookDao.findAll().first()
        val ublDao = getDao<UserBookListingDao>()
        ublDao.insert(
            UserBookListing(
                userId = user.id,
                bookId = book.id,
                readingStatus = UserBookListingReadingStatus.complete,
                reviewed = 0.toByte()
            )
        )
        assertDoesNotThrow("Inserting a review threw an exception") {
            reviewsDao.insert(
                Reviews(
                    id = randomId(),
                    userId = user.id,
                    bookId = book.id,
                    stars = 2.toByte(),
                    "test",
                    content = "test"
                )
            )
        }
        val ubl = USER_BOOK_LISTING
        assert(
            context.selectFrom(ubl)
                .where(ubl.USER_ID.eq(user.id), ubl.BOOK_ID.eq(book.id))
                .and(ubl.REVIEWED.eq(1.toByte()))
                .count() == 1
        ) {
            "Book was not marked reviewed after inserting a review"
        }
        assertThrows<DataAccessException>("Database didn't prevent duplicate review") {
            reviewsDao.insert(
                Reviews(
                    id = randomId(),
                    userId = user.id,
                    bookId = book.id,
                    stars = 2.toByte(),
                    "test2",
                    content = "test2"
                )
            )
        }
        assert(
            context.selectFrom(BOOKS_TOP_ACTIVITY).limit(1).first().handle == book.handle
        ) {
            "Failed to update top activity"
        }
        var rating = BigDecimal.ZERO
        var handle = ""
        assertDoesNotThrow("Failed to update book stats") {
            val first = context.selectFrom(
                BOOKS_STATISTICS
            ).where(
                BOOKS_STATISTICS.HANDLE.eq(book.handle)
            ).fetch().first()
            rating = first.rating!!
            handle = first.handle!!
            context.selectFrom(BOOKS_TOP_RATING).limit(1).fetch().first().apply {
                assert(rating.intValueExact() == this.rating!!.intValueExact()) {
                    "Failed view integrity - Incorrect stars\nexpected: ${rating.toPlainString()}\nfound: ${this.rating!!.toPlainString()}"
                }
                assert(handle == this.handle) {
                    "Failed view integrity - Incorrect handle"
                }
            }
        }
        assert(rating.intValueExact() == 4) {
            "Incorrect stars"
        }
        assert(handle == book.handle) {
            "Incorrect book"
        }
        assert(reviewsDao.findAll().size == 1) {
            "Incorrect database state"
        }
        assert(reviewsDao.findAll()[0].userId == user.id) {
            "Incorrect database state"
        }
        userDao.delete(user)
        assert(reviewsDao.findAll().size == 1) {
            "Incorrect database state"
        }
        assert(reviewsDao.findAll()[0].userId == null) {
            "User deletion failed to set reviewer to NULL"
        }
        userDao.insert(user)
        context.connection {
            val save = it.setSavepoint("reviews")
            try {
                assertDoesNotThrow {
                    reviewsDao.insert(
                        Reviews(
                            id = randomId(),
                            userId = user.id,
                            bookId = book.id,
                            stars = 2.toByte(),
                            "test",
                            content = "test"
                        )
                    )
                    reviewsDao.insert(
                        Reviews(
                            id = randomId(),
                            userId = user.id,
                            bookId = bookDao.findAll().asSequence().drop(1).first().id,
                            stars = 2.toByte(),
                            "test",
                            content = "test"
                        )
                    )
                }
            } finally {
                it.rollback(save)
            }
            assert(reviewsDao.findAll().size == 1)
        }

    }

}