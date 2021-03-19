package pwp.communicator.test

import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.jooq.types.ULong
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pwp.communicator.test.support.DBTest
import pwp.communicator.test.support.random
import pwp.communicator.using
import pwp.generated.BookClub
import pwp.generated.tables.daos.ClubsDao
import pwp.generated.tables.daos.UsersDao
import pwp.generated.tables.pojos.Clubs
import pwp.generated.tables.pojos.Users

@Tag("clubs")
class TestClub : DBTest() {

    @Test
    @DisplayName("Testing club member counts")
    fun memberCount(context: DSLContext) {
        context.selectFrom(BookClub.BOOK_CLUB.CLUBS_POPULAR).fetch().forEach {
            assert(it.memberCount == CLUB_MEMBER_COUNT.toLong()) {
                "Failed member check"
            }
        }
    }

    @Test
    @DisplayName("Testing club owner")
    fun owner(context: DSLContext) {
        val club = using<ClubsDao, Clubs> { this.random() }
        val user = using<UsersDao, Users> { this.random() }
        club.ownerId = user.id
        using<ClubsDao> {
            this.update(club)
        }
        val clubs = context.selectFrom(BookClub.BOOK_CLUB.CLUBS).where(
            BookClub.BOOK_CLUB.CLUBS.ID.eq(club.id)
        ).fetch().first()
        assert(clubs.id == club.id) {
            "Failed to find club"
        }
        assert(clubs.ownerId == user.id) {
            "Failed to set owner"
        }
        using<UsersDao> {
            this.delete(user)
        }
        assert(
            context.selectFrom(BookClub.BOOK_CLUB.CLUBS).where(
                BookClub.BOOK_CLUB.CLUBS.ID.eq(club.id)
            ).fetch().first().ownerId == null
        ) {
            "Failed to NULL owner"
        }
        assertThrows<DataAccessException>("Didn't throw for invalid id") {
            using<ClubsDao> {
                club.ownerId = ULong.valueOf(1)
                update(club)
            }
        }
    }

}