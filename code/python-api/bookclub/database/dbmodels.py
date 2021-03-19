# THIS FILE IS GENERATED
#
# flask-sqlacodegen mysql+pymysql://root:test@localhost:6969/book_club --flask --outfile dbmodels.py --noinflect --notables --noviews
#
# ONLY THIS HEADER HAS BEEN ADDED ON 19.3.2021
#
# DO NOT MODIFY
#
# coding: utf-8
from flask_sqlalchemy import SQLAlchemy


db = SQLAlchemy()



class Books(db.Model):
    __tablename__ = 'books'

    id = db.Column(db.BigInteger, primary_key=True)
    handle = db.Column(db.String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    full_name = db.Column(db.String(256, 'utf8mb4_unicode_ci'), nullable=False)
    description = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    pages = db.Column(db.Integer, index=True)
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    clubs = db.relationship('Clubs', secondary='club_book_link', backref='bookss')
    discussions = db.relationship('Discussions', secondary='discussion_book_link', backref='bookss')



class ClubBookLink(db.Model):
    __tablename__ = 'club_book_link'

    club_id = db.Column(db.ForeignKey('clubs.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)

    book = db.relationship('Books', primaryjoin='ClubBookLink.book_id == Books.id', backref='club_book_links')
    club = db.relationship('Clubs', primaryjoin='ClubBookLink.club_id == Clubs.id', backref='club_book_links')



class ClubDiscussionLink(db.Model):
    __tablename__ = 'club_discussion_link'

    club_id = db.Column(db.ForeignKey('clubs.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    discussion_id = db.Column(db.ForeignKey('discussions.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)

    club = db.relationship('Clubs', primaryjoin='ClubDiscussionLink.club_id == Clubs.id', backref='club_discussion_links')
    discussion = db.relationship('Discussions', primaryjoin='ClubDiscussionLink.discussion_id == Discussions.id', backref='club_discussion_links')



class ClubUserLink(db.Model):
    __tablename__ = 'club_user_link'

    club_id = db.Column(db.ForeignKey('clubs.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    user_id = db.Column(db.ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)

    club = db.relationship('Clubs', primaryjoin='ClubUserLink.club_id == Clubs.id', backref='club_user_links')
    user = db.relationship('Users', primaryjoin='ClubUserLink.user_id == Users.id', backref='club_user_links')



class Clubs(db.Model):
    __tablename__ = 'clubs'

    id = db.Column(db.BigInteger, primary_key=True)
    owner_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    handle = db.Column(db.String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    description = db.Column(db.String(2048, 'utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    owner = db.relationship('Users', primaryjoin='Clubs.owner_id == Users.id', backref='users_clubss')
    discussions = db.relationship('Discussions', secondary='club_discussion_link', backref='clubss')
    users = db.relationship('Users', secondary='club_user_link', backref='users_clubss_0')



class CommentCommentLink(db.Model):
    __tablename__ = 'comment_comment_link'
    __table_args__ = (
        db.Index('idx_ccl_child_parent', 'child_id', 'parent_id'),
    )

    parent_id = db.Column(db.ForeignKey('comments.id', onupdate='CASCADE'), primary_key=True, nullable=False)
    child_id = db.Column(db.ForeignKey('comments.id', onupdate='CASCADE'), primary_key=True, nullable=False)

    child = db.relationship('Comments', primaryjoin='CommentCommentLink.child_id == Comments.id', backref='comments_comment_comment_links')
    parent = db.relationship('Comments', primaryjoin='CommentCommentLink.parent_id == Comments.id', backref='comments_comment_comment_links_0')



class Comments(db.Model):
    __tablename__ = 'comments'

    id = db.Column(db.BigInteger, primary_key=True)
    user_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    content = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    pending = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime, index=True, server_default=db.FetchedValue())

    user = db.relationship('Users', primaryjoin='Comments.user_id == Users.id', backref='commentss')
    parents = db.relationship(
        'Comments',
        secondary='comment_comment_link',
        primaryjoin='Comments.id == comment_comment_link.c.child_id',
        secondaryjoin='Comments.id == comment_comment_link.c.parent_id',
        backref='commentss'
    )
    discussions = db.relationship('Discussions', secondary='discussion_comment_link', backref='commentss')


class DiscussionCommentLink(Comments):
    __tablename__ = 'discussion_comment_link'

    discussion_id = db.Column(db.ForeignKey('discussions.id', ondelete='CASCADE', onupdate='CASCADE'), nullable=False, index=True)
    comment_id = db.Column(db.ForeignKey('comments.id', onupdate='CASCADE'), primary_key=True)

    discussion = db.relationship('Discussions', primaryjoin='DiscussionCommentLink.discussion_id == Discussions.id', backref='discussion_comment_links')



class DiscussionBookLink(db.Model):
    __tablename__ = 'discussion_book_link'

    discussion_id = db.Column(db.ForeignKey('discussions.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)

    book = db.relationship('Books', primaryjoin='DiscussionBookLink.book_id == Books.id', backref='discussion_book_links')
    discussion = db.relationship('Discussions', primaryjoin='DiscussionBookLink.discussion_id == Discussions.id', backref='discussion_book_links')



class Discussions(db.Model):
    __tablename__ = 'discussions'

    id = db.Column(db.BigInteger, primary_key=True)
    owner_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    topic = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    owner = db.relationship('Users', primaryjoin='Discussions.owner_id == Users.id', backref='discussionss')



class Friends(db.Model):
    __tablename__ = 'friends'
    __table_args__ = (
        db.Index('idx_friends_reverse', 'to_id', 'from_id'),
    )

    from_id = db.Column(db.ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    to_id = db.Column(db.ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)

    _from = db.relationship('Users', primaryjoin='Friends.from_id == Users.id', backref='users_friendss')
    to = db.relationship('Users', primaryjoin='Friends.to_id == Users.id', backref='users_friendss_0')



class FriendsRequest(db.Model):
    __tablename__ = 'friends_request'
    __table_args__ = (
        db.Index('idx_fr_reverse', 'from_id', 'to_id'),
    )

    from_id = db.Column(db.ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    to_id = db.Column(db.ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    status = db.Column(db.Enum('pending', 'confirmed', 'rejected'), nullable=False, server_default=db.FetchedValue())

    _from = db.relationship('Users', primaryjoin='FriendsRequest.from_id == Users.id', backref='users_friends_requests')
    to = db.relationship('Users', primaryjoin='FriendsRequest.to_id == Users.id', backref='users_friends_requests_0')



class Reviews(db.Model):
    __tablename__ = 'reviews'
    __table_args__ = (
        db.Index('reviews_user_id', 'user_id', 'book_id'),
    )

    id = db.Column(db.BigInteger, primary_key=True)
    user_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'))
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), index=True)
    stars = db.Column(db.Integer, nullable=False, server_default=db.FetchedValue())
    title = db.Column(db.String(128, 'utf8mb4_unicode_ci'), nullable=False)
    content = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    book = db.relationship('Books', primaryjoin='Reviews.book_id == Books.id', backref='reviewss')
    user = db.relationship('Users', primaryjoin='Reviews.user_id == Users.id', backref='reviewss')



class UserBookListing(db.Model):
    __tablename__ = 'user_book_listing'
    __table_args__ = (
        db.Index('idx_ubl_reverse', 'book_id', 'user_id'),
        db.Index('idx_ubl_reviewed', 'user_id', 'reviewed')
    )

    user_id = db.Column(db.ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    reading_status = db.Column(db.Enum('pending', 'reading', 'complete'), index=True)
    reviewed = db.Column(db.Integer, nullable=False, server_default=db.FetchedValue())
    ignored = db.Column(db.Integer, nullable=False, server_default=db.FetchedValue())
    like_status = db.Column(db.Enum('liked', 'disliked'))
    current_page = db.Column(db.Integer)

    book = db.relationship('Books', primaryjoin='UserBookListing.book_id == Books.id', backref='user_book_listings')
    user = db.relationship('Users', primaryjoin='UserBookListing.user_id == Users.id', backref='user_book_listings')



class Users(db.Model):
    __tablename__ = 'users'

    id = db.Column(db.BigInteger, primary_key=True)
    username = db.Column(db.String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    description = db.Column(db.String(256, 'utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)
    password_hash = db.Column(db.String(64, 'ascii_bin'))
    icon_url = db.Column(db.String(collation='utf8mb4_unicode_ci'))
    external_id = db.Column(db.String(128, 'utf8mb4_unicode_ci'), unique=True)

    tos = db.relationship(
        'Users',
        secondary='friends',
        primaryjoin='Users.id == friends.c.from_id',
        secondaryjoin='Users.id == friends.c.to_id',
        backref='userss'
    )
