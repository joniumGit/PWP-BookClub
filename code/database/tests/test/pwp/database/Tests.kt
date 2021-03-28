package pwp.database

import org.jooq.DSLContext
import org.jooq.UpdatableRecord
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import pwp.database.generated.tables.records.ClubsRecord
import pwp.database.generated.tables.records.UsersRecord
import pwp.database.generated.tables.references.CLUBS
import pwp.database.generated.tables.references.CLUB_USER_LINK
import pwp.database.generated.tables.references.USERS
import kotlin.test.assertEquals

inline fun <T : UpdatableRecord<*>> add(
    range: IntRange,
    crossinline block: DSLContext.(Int) -> T
) = DB.context.safe {
    range.map { block(it) }
        .toList()
        .let {
            batchInsert(it).execute()
        }
}

@DBInject
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClubsTests {

    lateinit var users: List<UsersRecord>
    lateinit var clubs: List<ClubsRecord>

    @BeforeAll
    fun before(ctx: DSLContext) {
        add((0 until 20)) {
            val record = this.newRecord(USERS)
            record.username = handle()
            record.description = "test"
            record
        }
        users = ctx.fetch(USERS).toList()
        assertEquals(20, ctx.fetchCount(USERS))
        add((0 until 4)) {
            val record = this.newRecord(CLUBS)
            record.handle = handle()
            record
        }
        clubs = ctx.fetch(CLUBS).toList()
        assertEquals(4, ctx.fetchCount(CLUBS))
    }

    @Test
    fun club(ctx: DSLContext) {
        val club = clubs.random()
        add(users.indices) {
            val record = this.newRecord(CLUB_USER_LINK)
            record.userId = users[it].id
            record.clubId = club.id
            record
        }
        assertEquals(
            users.size,
            ctx.fetchCount(USERS.join(CLUB_USER_LINK).onKey().where(CLUB_USER_LINK.CLUB_ID.eq(club.id)))
        )
    }

}