package test

import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import pwp.generated.tables.records.ClubsUserListingRecord
import pwp.generated.tables.records.CommentsRecord
import java.sql.SQLException

class TestView : DBTest(populateUsers = false, populateBooks = false, populateClubs = false) {

    /**
     * Testing a couple views and if they work we assume they all work
     *
     * Complex views can't be updated anyway
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
        try {
            context.executeInsert(
                CommentsRecord(
                    username = "test",
                    content = "test"
                )
            )
        } catch (e: DataAccessException) {
            assert(e.getCause(SQLException::class.java).message!!.contains("lack rights"))
        }
    }

}