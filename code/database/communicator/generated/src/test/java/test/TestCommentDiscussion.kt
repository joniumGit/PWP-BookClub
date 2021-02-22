package test

import Persist
import getDao
import log
import org.jooq.DSLContext
import org.jooq.exception.DataAccessException
import org.junit.jupiter.api.*
import pwp.generated.BookClub
import pwp.generated.tables.daos.*
import pwp.generated.tables.pojos.*
import randomHandle
import randomId

@Tag("comments")
@Persist
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TestCommentDiscussion : DBTest() {

    private val discussionDao = getDao<DiscussionsDao>()
    private val commentDao = getDao<CommentsDataDao>()
    private val cclDao = getDao<CommentCommentLinkDao>()
    private val dclDao = getDao<DiscussionCommentLinkDao>()
    private val user by lazy { userDao.findAll().random().log() }
    private val club by lazy {
        clubDao.fetchOneById(
            getDao<ClubUserLinkDao>().fetchByUserId(user.id!!).random().clubId!!
        )!!
    }
    private val book by lazy { bookDao.findAll().random() }
    private val discussion by lazy {
        Discussions(
            id = randomId(),
            topic = randomHandle(),
        )
    }
    private val discussion2 by lazy {
        Discussions(
            id = randomId(),
            topic = randomHandle(),
        )
    }
    private val comment by lazy {
        CommentsData(
            id = randomId(),
            userId = user.id,
            content = randomHandle()
        )
    }
    private val comment2 by lazy {
        CommentsData(
            id = randomId(),
            userId = userDao.findAll()[2].id,
            content = randomHandle()
        )
    }

    @Test
    @DisplayName("Create discussions")
    @Order(1)
    fun discussion() {
        assertDoesNotThrow {
            discussionDao.insert(discussion, discussion2)
        }
        assertThrows<DataAccessException> {
            discussionDao.insert(discussion2)
        }
        assertDoesNotThrow {
            getDao<DiscussionBookLinkDao>().insert(
                DiscussionBookLink(
                    discussionId = discussion.id,
                    bookId = book.id
                )
            )
        }
        assertThrows<DataAccessException> {
            getDao<DiscussionBookLinkDao>().insert(
                DiscussionBookLink(
                    discussionId = discussion.id,
                    bookId = book.id
                )
            )
        }
    }

    @Test
    @DisplayName("Link club and book")
    @Order(2)
    fun club() {
        assertDoesNotThrow {
            getDao<ClubDiscussionLinkDao>().insert(
                ClubDiscussionLink(clubId = club.id, discussionId = discussion.id),
                ClubDiscussionLink(clubId = club.id, discussionId = discussion2.id)
            )
        }
        assertThrows<DataAccessException> {
            getDao<ClubDiscussionLinkDao>().insert(
                ClubDiscussionLink(clubId = club.id, discussionId = discussion.id)
            )
        }
    }

    @Test
    @DisplayName("Comment posting")
    @Order(3)
    fun post() {
        assertDoesNotThrow {
            commentDao.insert(comment, comment2)
        }
    }

    @Test
    @DisplayName("Test views")
    @Order(4)
    fun vies(context: DSLContext) {
        assert(
            context.selectFrom(BookClub.BOOK_CLUB.COMMENTS).count() == 0
        ) {
            "Pending status update failed to show in view"
        }
        assertDoesNotThrow("Failed comment update") {
            commentDao.update(comment.copy(pending = 0.toByte()))
        }
        assert(
            context.selectFrom(BookClub.BOOK_CLUB.COMMENTS).count() == 1
        ) {
            "Pending status update failed to show in view"
        }
    }

    @Test
    @DisplayName("Testing comment links")
    @Order(5)
    fun linkComments(context: DSLContext) {
        assertDoesNotThrow("Failed to insert comment link") {
            dclDao.insert(DiscussionCommentLink(discussionId = discussion.id, commentId = comment.id))
        }
        assertThrows<DataAccessException>("Failed to prevent linking comment to multiple discussions") {
            dclDao.insert(DiscussionCommentLink(discussionId = discussion2.id, commentId = comment.id))
        }
        assertDoesNotThrow("Failed to insert child comment") {
            cclDao.insert(CommentCommentLink(parentId = comment.id, childId = comment2.id))
        }
        assertThrows<DataAccessException>("Failed to prevent child master") {
            cclDao.insert(CommentCommentLink(parentId = comment2.id, childId = comment.id))
        }
        assertThrows<DataAccessException>("Failed to prevent self child") {
            cclDao.insert(CommentCommentLink(parentId = comment.id, childId = comment.id))
        }
    }

}