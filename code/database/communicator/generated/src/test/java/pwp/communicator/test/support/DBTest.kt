package pwp.communicator.test.support

import DB
import DBInject
import log
import org.jooq.DSLContext
import org.junit.jupiter.api.*
import pwp.communicator.Helpers
import pwp.communicator.getDao
import pwp.generated.tables.daos.*
import pwp.generated.tables.pojos.ClubBookLink
import pwp.generated.tables.pojos.ClubUserLink
import java.sql.Connection
import java.sql.Savepoint

@Tag("database")
@Disabled("Root class")
@DBInject
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class DBTest(
    private val populateUsers: Boolean = true,
    private val populateBooks: Boolean = true,
    private val populateClubs: Boolean = true
) {

    @Suppress("unused")
    companion object CONSTANTS {
        private const val DEFAULT_COUNT = 100
        const val USER_COUNT: Int = DEFAULT_COUNT
        const val BOOK_COUNT: Int = DEFAULT_COUNT
        const val CLUB_MEMBER_COUNT: Int = 10
        const val CLUB_COUNT: Int = USER_COUNT / CLUB_MEMBER_COUNT

        init {
            Helpers.initialize(DB.context)
        }

    }

    protected val userDao = getDao<UsersDao>()
    protected val bookDao = getDao<BooksDao>()
    protected val clubDao = getDao<ClubsDao>()

    private lateinit var savepoint: Savepoint


    /**
     *  Populates some users, clubs and books and links them all in a predictable way
     */
    @BeforeAll
    fun populate(context: DSLContext, c: Connection) {
        savepoint = c.setSavepoint("db-test-begin")
        if (populateUsers) {
            println("Generating users:")
            addUsers(USER_COUNT)
            assert(userDao.findAll().size == USER_COUNT) {
                "Failed user count"
            }
        }
        if (populateBooks) {
            println("\nGenerating Books:")
            addBooks(BOOK_COUNT)
            assert(bookDao.findAll().size == BOOK_COUNT) {
                "Failed book count"
            }
        }
        if (populateClubs) {
            println("\nGenerating Clubs:")
            addClubs(CLUB_COUNT)
            assert(clubDao.findAll().size == CLUB_COUNT) {
                "Failed club count"
            }
        }
        if (populateClubs && (populateBooks || populateUsers)) {
            println("\nGenerating Links")
        }
        if (populateBooks && populateClubs) {
            val cblDao = getDao<ClubBookLinkDao>()
            (0 until CLUB_COUNT).forEach { i ->
                bookDao.findAll().asSequence().drop(i).first().let {
                    cblDao.insert(
                        ClubBookLink(
                            clubId = clubDao.findAll().asSequence().drop(i).first().id,
                            bookId = it.id
                        ).log()
                    )
                }
            }
            println()
            assert(cblDao.findAll().size == CLUB_COUNT) {
                "Failed to insert correct amount"
            }
        }
        val userIter = userDao.findAll().iterator()
        if (populateUsers && populateClubs) {
            val linkDao = getDao<ClubUserLinkDao>()
            (0 until CLUB_COUNT).map { i ->
                linkDao.insert(
                    clubDao.findAll().asSequence().drop(i).first().let { club ->
                        (0 until CLUB_MEMBER_COUNT).map {
                            userIter.next().let { user ->
                                ClubUserLink(
                                    clubId = club.id,
                                    userId = user.id
                                ).log()
                            }
                        }
                    }
                )
            }
            assert(linkDao.findAll().size == USER_COUNT) {
                "Failed to insert correct amount"
            }
        }
    }

    /**
     *  Reverts any changes made during the run of the test class
     */
    @AfterAll
    fun revert(connection: Connection) = connection.rollback(savepoint)

}