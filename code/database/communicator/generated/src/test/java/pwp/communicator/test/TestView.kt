package pwp.communicator.test

import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import pwp.communicator.test.support.DBTest
import pwp.generated.tables.records.ClubsUserListingRecord
import java.sql.SQLException

class TestView : DBTest(populateUsers = false, populateBooks = false, populateClubs = false) {

    /**
     * Testing a views and if it works we assume they all work
     *
     * Complex views can't be updated anyway
     *
     * Stat views are not used currently so no tests for them
     */
    @Test
    @DisplayName("Testing user listing")
    fun userList(context: DSLContext) {
        try {
            context.executeInsert(
                ClubsUserListingRecord(
                    clubHandle = "test",
                    username = "test"
                )
            )
        } catch (e: DataAccessException) {
            assert(e.getCause(SQLException::class.java).message!!.contains("lack rights"))
        }
    }

}