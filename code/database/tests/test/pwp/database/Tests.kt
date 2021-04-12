package pwp.database

import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.jooq.exception.NoDataFoundException
import org.junit.jupiter.api.*
import pwp.database.generated.enums.UserBooksReadingStatus
import pwp.database.generated.tables.records.BooksRecord
import pwp.database.generated.tables.records.BooksStatisticsRecord
import pwp.database.generated.tables.records.UserBooksRecord
import pwp.database.generated.tables.records.UsersRecord
import pwp.database.generated.tables.references.*
import kotlin.test.assertEquals
import kotlin.test.assertNull


@DBInject
@DisplayName("Users tests")
class UsersTests {

    @Test
    @DisplayName("Create user")
    fun createUser(ctx: DSLContext) {
        val name = handle()
        ctx.add(USERS) {
            username = name
        }
    }

    @Test
    @DisplayName("Duplicate username")
    fun duplicateUser(ctx: DSLContext) {
        val name = handle()
        ctx.add(USERS) {
            username = name
        }
        assertThrows<DataAccessException>(DUPLICATE) {
            ctx.newRecord(USERS).apply {
                username = name
            }.insert()
        }
    }

    @Test
    @DisplayName("Set deleted")
    fun deleteUser(ctx: DSLContext) {
        val name = handle()
        val user = ctx.add(USERS) {
            username = name
        }
        assertEquals(name, user.username)
        user.deleted = 1
        user.update()
        assertEquals(1, ctx.selectFrom(USERS).where(USERS.USERNAME.eq(name)).fetchSingle().deleted)
    }

}

@DBInject
@DisplayName("Books tests")
class BooksTests {

    @Test
    @DisplayName("Create book")
    fun createBook(ctx: DSLContext) {
        val name = handle()
        ctx.add(BOOKS) {
            handle = name
            fullName = handle() + "-" + name
        }
    }

    @Test
    @DisplayName("Duplicate book")
    fun duplicateBook(ctx: DSLContext) {
        val name = handle()
        val prefix = handle()
        ctx.add(BOOKS) {
            handle = name
            fullName = "$prefix-$name"
        }
        assertDoesNotThrow(FAILED_VALID) {
            ctx.add(BOOKS) {
                handle = handle()
                fullName = "$prefix-$name"
            }
        }
        assertThrows<DataAccessException>(DUPLICATE) {
            ctx.newRecord(BOOKS).apply {
                handle = name
                fullName = "$prefix-$name"
            }.insert()
        }
    }

    @Test
    @DisplayName("Set deleted")
    fun deleteBook(ctx: DSLContext) {
        val name = handle()
        val book = ctx.add(BOOKS) {
            handle = name
            fullName = name
        }
        book.deleted = 1
        book.update()
        assertEquals(1, ctx.selectFrom(BOOKS).where(BOOKS.HANDLE.eq(name)).fetchSingle().deleted)
    }

}

@DBInject
@DisplayName("Comment tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Comments {

    private lateinit var user: UsersRecord

    @Persist
    @BeforeAll
    fun createUser(ctx: DSLContext) {
        val name = handle()
        user = ctx.add(USERS) { username = name }
    }

    @Test
    @DisplayName("Create comment")
    fun create(ctx: DSLContext) {
        ctx.add(COMMENTS) {
            userId = user.id
            content = handle()
        }
    }

    @Test
    @DisplayName("FG user")
    fun fg(ctx: DSLContext) {
        val record = ctx.add(COMMENTS) {
            userId = user.id
            content = handle()
        }
        user.delete()
        record.refresh()
        assertNull(record.userId)
    }

}

@DBInject
@DisplayName("Review tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Reviews {

    private lateinit var user: UsersRecord
    private lateinit var book: BooksRecord

    @Persist
    @BeforeAll
    fun createUserAndBook(ctx: DSLContext) {
        user = ctx.add(USERS) {
            username = handle()
        }
        book = ctx.add(BOOKS) {
            handle = handle()
            fullName = this.handle
        }
    }

    @Test
    @DisplayName("Create review")
    fun review(ctx: DSLContext) {
        ctx.add(REVIEWS) {
            userId = user.id
            bookId = book.id
            title = handle()
            content = handle()
        }
    }

    @Test
    @DisplayName("Duplicate review")
    fun duplicate(ctx: DSLContext) {
        ctx.add(REVIEWS) {
            userId = user.id
            bookId = book.id
            title = handle()
            content = handle()
        }
        assertThrows<DataAccessException>(DUPLICATE) {
            ctx.newRecord(REVIEWS).apply {
                userId = user.id
                bookId = book.id
                title = handle()
                content = handle()
            }.insert()
        }
    }

    @Test
    @DisplayName("Add comment")
    fun comment(ctx: DSLContext) {
        val review = ctx.add(REVIEWS) {
            userId = user.id
            bookId = book.id
            title = handle()
            content = handle()
        }
        val cmt = ctx.add(COMMENTS) {
            userId = user.id
            content = handle()
        }
        ctx.newRecord(REVIEW_COMMENT_LINK).apply {
            commentId = cmt.id
            reviewId = review.id
            insert()
        }
    }

    @Test
    @DisplayName("FG user")
    fun user(ctx: DSLContext) {
        val review = ctx.add(REVIEWS) {
            userId = user.id
            bookId = book.id
            title = handle()
            content = handle()
        }
        user.delete()
        review.refresh()
        assertNull(review.userId)
    }

    @Test
    @DisplayName("FG book")
    fun book(ctx: DSLContext) {
        val review = ctx.add(REVIEWS) {
            userId = user.id
            bookId = book.id
            title = handle()
            content = handle()
        }
        book.delete()
        assertThrows<NoDataFoundException>(FOUND_DELETED) {
            review.refresh()
        }
    }

}

@DBInject
@DisplayName("User books tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserBooksTests {

    private lateinit var user: UsersRecord
    private lateinit var book: BooksRecord

    @Persist
    @BeforeAll
    fun createUserAndBook(ctx: DSLContext) {
        user = ctx.add(USERS) {
            username = handle()
        }
        book = ctx.add(BOOKS) {
            handle = handle()
            fullName = this.handle
        }
    }

    private lateinit var ubl: UserBooksRecord

    @Test
    @Order(0)
    @Persist
    @DisplayName("Create record")
    fun createListing(ctx: DSLContext) {
        ubl = ctx.newRecord(USER_BOOKS).apply {
            userId = user.id
            bookId = book.id
            insert()
        }
    }

    @Test
    @Order(1)
    @Persist
    @DisplayName("Duplicate record")
    fun createDuplicate(ctx: DSLContext) {
        assertThrows<DataAccessException>(DUPLICATE) {
            ctx.newRecord(USER_BOOKS).apply {
                userId = user.id
                bookId = book.id
                insert()
            }
        }
    }

    @Test
    @Order(2)
    @Persist
    @DisplayName("Reading status")
    fun status(ctx: DSLContext) {
        ubl.readingStatus = UserBooksReadingStatus.pending
        ubl.store()
        ubl.readingStatus = UserBooksReadingStatus.reading
        ubl.store()
        ubl.readingStatus = UserBooksReadingStatus.complete
        ubl.store()
        assertThrows<DataAccessException>(INVALID) {
            ctx.execute(
                "UPDATE user_books " +
                        "SET reading_status='test' " +
                        "WHERE user_id=${user.id} " +
                        "AND book_id=${book.id}"
            )
        }
        assertDoesNotThrow(FAILED_VALID) {
            ctx.execute(
                "UPDATE user_books " +
                        "SET reading_status=NULL " +
                        "WHERE user_id=${user.id} " +
                        "AND book_id=${book.id}"
            )
        }
        ubl.refresh()
        assertNull(ubl.readingStatus)
    }

    @Test
    @Order(3)
    @Persist
    @DisplayName("Reviewed trigger")
    fun trigger(ctx: DSLContext) {
        ctx.add(REVIEWS) {
            userId = user.id
            bookId = book.id
            title = handle()
            content = handle()
        }
    }

    @Test
    @Order(4)
    @DisplayName("FG user")
    fun fgUser() {
        assertDoesNotThrow {
            ubl.refresh()
        }
        user.delete()
        assertThrows<NoDataFoundException>(FOUND_DELETED) {
            ubl.refresh()
        }
    }

    @Test
    @Order(5)
    @DisplayName("FG book")
    fun fgBook() {
        assertDoesNotThrow {
            ubl.refresh()
        }
        book.delete()
        assertThrows<NoDataFoundException>(FOUND_DELETED) {
            ubl.refresh()
        }
    }

}

@DBInject
@DisplayName("Clubs tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClubsTests {

    private lateinit var users: List<UsersRecord>
    private lateinit var books: List<BooksRecord>

    @Persist
    @BeforeAll
    fun before(ctx: DSLContext) {
        add((0 until 20)) {
            val record = this.newRecord(USERS)
            record.username = handle()
            record.description = "test"
            record
        }
        users = ctx.fetch(USERS).toList()
        add((0 until 20)) {
            val record = this.newRecord(BOOKS)
            record.handle = handle()
            record.fullName = "test-book ${record.handle}"
            record
        }
        books = ctx.fetch(BOOKS).toList()
    }

    @Test
    @DisplayName("Create club")
    fun club(ctx: DSLContext) {
        ctx.add(CLUBS) {
            handle = handle()
        }
    }

    @Test
    @DisplayName("Duplicate club")
    fun duplicate(ctx: DSLContext) {
        val name = handle()
        ctx.add(CLUBS) {
            handle = name
        }
        assertThrows<DataAccessException>(DUPLICATE) {
            ctx.newRecord(CLUBS).apply {
                handle = name
                insert()
            }
        }
    }

    @Test
    @DisplayName("Add users")
    fun addUsers(ctx: DSLContext) {
        val club = ctx.add(CLUBS) { handle = handle() }
        ctx.batchInsert(users.map {
            ctx.newRecord(CLUB_USER_LINK).apply {
                clubId = club.id
                userId = it.id
            }
        })
    }

    @Test
    @DisplayName("Add books")
    fun addBooks(ctx: DSLContext) {
        val club = ctx.add(CLUBS) { handle = handle() }
        ctx.batchInsert(books.map {
            ctx.newRecord(CLUB_BOOK_LINK).apply {
                clubId = club.id
                bookId = it.id
            }
        })
    }

    @Test
    @DisplayName("FG user (owner)")
    fun owner(ctx: DSLContext) {
        val club = ctx.add(CLUBS) { handle = handle() }
        val owner = users.random()
        club.ownerId = owner.id
        club.store()
        club.refresh()
        assertEquals(owner.id, club.ownerId)
        owner.delete()
        club.refresh()
        assertNull(club.ownerId)
    }

}

@DBInject
@Persist
@DisplayName("Stats tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Views {

    private lateinit var book: BooksRecord
    private lateinit var ubl: UserBooksRecord

    private val expectedAverage = 7

    private fun DSLContext.stat() = this.selectFrom(BOOKS_STATISTICS)
        .where(BOOKS_STATISTICS.HANDLE.eq(book.handle))
        .fetchSingle()

    private fun DSLContext.check(s: UserBooksReadingStatus, block: (BooksStatisticsRecord) -> Long?) {
        val start = this.stat().let(block = block) ?: 0
        ubl.readingStatus = s
        ubl.update()
        assertEquals(start + 1, this.stat().let(block = block))
    }

    @BeforeAll
    fun createUserAndBookAndUBLAndMockReview(ctx: DSLContext) {
        book = ctx.add(BOOKS) {
            handle = handle()
            fullName = this.handle
        }
        ctx.add(USERS) { username = handle() }.let {
            ctx.add(REVIEWS) {
                userId = it.id
                bookId = book.id
                title = handle()
                content = handle()
                stars = 4
            }
        }
        ctx.add(USERS) { username = handle() }.let {
            ubl = ctx.newRecord(USER_BOOKS).apply {
                userId = it.id
                bookId = book.id
                insert()
            }
            ctx.add(REVIEWS) {
                userId = it.id
                bookId = book.id
                title = handle()
            }
        }

    }

    @Test
    @Order(0)
    @DisplayName("Pending count")
    fun pending(ctx: DSLContext) = ctx.check(UserBooksReadingStatus.pending) { it.pending }

    @Test
    @Order(1)
    @DisplayName("Reading count")
    fun reading(ctx: DSLContext) = ctx.check(UserBooksReadingStatus.reading) { it.readers }

    @Test
    @Order(2)
    @DisplayName("Rating")
    fun rating(ctx: DSLContext) {
        assertEquals(expectedAverage, ctx.stat().rating!!.intValueExact())
    }

    @Test
    @Order(3)
    @DisplayName("Complete count")
    fun complete(ctx: DSLContext) = ctx.check(UserBooksReadingStatus.complete) { it.completed }

    @Test
    @Order(4)
    @DisplayName("Like and dislike count")
    fun ldl(ctx: DSLContext) {
        var start = ctx.stat().liked ?: 0
        ubl.liked = 1
        ubl.update()
        assertEquals(start + 1, ctx.stat().liked)
        ubl.liked = null
        ubl.update()
        assertEquals(start, ctx.stat().liked)
        start = ctx.stat().disliked ?: 0
        ubl.liked = 0
        ubl.update()
        assertEquals(start + 1, ctx.stat().disliked)
    }

}
