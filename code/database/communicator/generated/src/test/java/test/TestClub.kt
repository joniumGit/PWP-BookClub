package test

import org.jooq.DSLContext
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import pwp.generated.BookClub

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

}