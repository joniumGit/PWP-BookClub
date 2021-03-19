package pwp.communicator.test

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pwp.communicator.getDao
import pwp.communicator.test.support.DBTest
import pwp.generated.BookClub
import pwp.generated.enums.FriendsRequestStatus
import pwp.generated.tables.daos.FriendsDao
import pwp.generated.tables.daos.FriendsRequestDao
import pwp.generated.tables.pojos.FriendsRequest

@Tag("users")
class TestUser : DBTest(populateClubs = false, populateBooks = false) {

    @Test
    @DisplayName("Asserting user count")
    fun userCount(context: DSLContext) {
        assert(context.selectFrom(BookClub.BOOK_CLUB.USERS).count() == USER_COUNT) {
            "Failed user count"
        }
    }

    @Test
    @DisplayName("Friends test")
    fun friends() {
        val user1 = userDao.findAll().random().id
        val user2 = userDao.findAll().filter { it.id != user1 }.random().id
        // You can friend yourself so nobody is without friends :)
        assertDoesNotThrow {
            getDao<FriendsRequestDao>().insert(
                FriendsRequest(fromId = user1, toId = user1)
            )
        }
        val fr1 = FriendsRequest(fromId = user1, toId = user2)
        assertDoesNotThrow {
            getDao<FriendsRequestDao>().insert(
                FriendsRequest(fromId = user2, toId = user1, status = FriendsRequestStatus.rejected),
                fr1
            )
        }
        fr1.status = FriendsRequestStatus.confirmed
        assertDoesNotThrow {
            getDao<FriendsRequestDao>().update(fr1)
        }
        assert(getDao<FriendsDao>().findAll().size == 1)
        assert(getDao<FriendsRequestDao>().findAll().size == 3)
        assertDoesNotThrow {
            getDao<FriendsDao>().findAll().first().let {
                getDao<FriendsDao>().delete(it)
            }
        }
        assert(getDao<FriendsRequestDao>().findAll().size == 1)
    }

}