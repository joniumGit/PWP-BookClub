package pwp.communicator.test

import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.*
import pwp.communicator.getDao
import pwp.communicator.test.support.DBTest
import pwp.communicator.test.support.random
import pwp.communicator.using
import pwp.generated.tables.daos.*
import pwp.generated.tables.pojos.*
import pwp.generated.tables.references.BOOKS
import randomHandle
import randomId

@Tag("books")
class TestBook : DBTest(
    populateClubs = false,
    populateUsers = false
) {

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

    @Test
    @DisplayName("Testing book linking to discussion")
    fun linksDisc() {
        using<DiscussionBookLinkDao> {
            val dDao = getDao<DiscussionsDao>()
            val books = HashSet(bookDao.findAll())
            val book = books.random()
            books.remove(book)
            val book2 = books.random()
            val disc1 = Discussions(
                id = randomId(),
                topic = randomHandle()
            )
            val disc2 = Discussions(
                id = randomId(),
                topic = randomHandle()
            )
            assertDoesNotThrow("Threw on inserting valid records") {
                dDao.insert(disc1, disc2)
                this.insert(
                    DiscussionBookLink(
                        discussionId = disc1.id,
                        bookId = book.id
                    ),
                    DiscussionBookLink(
                        discussionId = disc2.id,
                        bookId = book2.id
                    ),
                    DiscussionBookLink(
                        discussionId = disc1.id,
                        bookId = book2.id
                    )
                )
            }
            assertThrows<DataAccessException>("Inserted duplicate record") {
                this.insert(
                    DiscussionBookLink(
                        discussionId = disc1.id,
                        bookId = book.id
                    )
                )
            }
            assertDoesNotThrow {
                bookDao.delete(book2)
            }
            assert(this.count() == 1L) {
                "Count failed 1"
            }
            assertDoesNotThrow("Threw on a valid query") {
                this.delete(this.fetchByBookId(book.id!!).first())
            }
            assert(this.count() == 0L) {
                "Count failed 2"
            }
        }
    }

    @Test
    @DisplayName("Testing book linking to clubs")
    fun linksClub() {
        using<ClubBookLinkDao> {
            val club = Clubs(
                id = randomId(),
                handle = randomHandle(),
                description = randomHandle() + randomHandle()
            )
            using<ClubsDao> {
                insert(club)
                assert(count() == 1L) {
                    "Club count failed. Maybe DB is not empty?"
                }
            }
            val dupe = assertDoesNotThrow("Threw on a valid insert") {
                val books = HashSet(bookDao.findAll())
                val book1 = books.random()
                books.remove(book1)
                val book2 = books.random()
                this.insert(
                    ClubBookLink(
                        clubId = club.id,
                        bookId = book1.id
                    ),
                    ClubBookLink(
                        clubId = club.id,
                        bookId = book2.id
                    )
                )
                assert(this.count() == 2L) {
                    "Count mismatch 1"
                }
                bookDao.delete(book1)
                assert(this.count() == 1L) {
                    "Count mismatch 2"
                }
                val dupe = ClubBookLink(
                    clubId = club.id,
                    bookId = bookDao.random().id
                )
                this.insert(
                    ClubBookLink(
                        clubId = club.id,
                        bookId = bookDao.random().id
                    ),
                    dupe
                )
                assert(this.count() == 3L) {
                    "Count mismatch 3"
                }
                dupe
            }
            assertThrows<DataAccessException>("inserted duplicate") {
                this.insert(dupe)
            }
        }
    }

    @Test
    @DisplayName("Testing user book listings")
    fun listings() {
        using<UserBookListingDao> {
            val user = Users(
                id = randomId(),
                username = randomHandle(),
                description = "Test"
            )
            userDao.insert(
                user
            )
            assert(userDao.count() == 1L) {
                "Count failed. Maybe database is not empty?"
            }
            val book = bookDao.random()
            val book2 = bookDao.random()
            assertDoesNotThrow("Throws while inserting valid records") {
                this.insert(
                    UserBookListing(
                        userId = user.id,
                        bookId = book.id
                    ),
                    UserBookListing(
                        userId = user.id,
                        bookId = book2.id
                    )
                )
            }
            assert(this.count() == 2L) {
                "Count mismatch 1"
            }
            assertThrows<DataAccessException>("Inserted duplicate") {
                this.insert(
                    UserBookListing(
                        userId = user.id,
                        bookId = book.id
                    )
                )
            }
            assert(this.count() == 2L) {
                "Count mismatch 2"
            }
            userDao.delete(user)
            assert(this.count() == 0L) {
                "Failed to cascade user deletion to ${this::class.simpleName}"
            }
        }
    }


}