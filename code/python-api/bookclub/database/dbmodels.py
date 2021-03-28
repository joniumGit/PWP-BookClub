# THIS FILE IS GENERATED
#
# flask-sqlacodegen mysql+pymysql://root:test@localhost:6969/book_club --flask --outfile dbmodels.py --noinflect --notables
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

    id = db.Column(db.Integer, primary_key=True)
    handle = db.Column(db.String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    full_name = db.Column(db.String(256, 'utf8mb4_unicode_ci'), nullable=False)
    description = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    pages = db.Column(db.Integer, index=True)
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    clubs = db.relationship('Clubs', secondary='club_book_link', backref='bookss')
    discussions = db.relationship('Discussions', secondary='discussion_book_link', backref='bookss')



class BooksStatistics(db.Model):
    __tablename__ = 'books_statistics'

    handle = db.Column(db.String(64))
    rating = db.Column(db.Numeric(7, 2), server_default=db.FetchedValue())
    readers = db.Column(db.BigInteger, server_default=db.FetchedValue())
    completed = db.Column(db.BigInteger, server_default=db.FetchedValue())
    pending = db.Column(db.BigInteger, server_default=db.FetchedValue())
    liked = db.Column(db.BigInteger, server_default=db.FetchedValue())
    disliked = db.Column(db.BigInteger, server_default=db.FetchedValue())



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

    id = db.Column(db.Integer, primary_key=True)
    owner_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    handle = db.Column(db.String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    description = db.Column(db.String(2048, 'utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    owner = db.relationship('Users', primaryjoin='Clubs.owner_id == Users.id', backref='users_clubss')
    discussions = db.relationship('Discussions', secondary='club_discussion_link', backref='clubss')
    users = db.relationship('Users', secondary='club_user_link', backref='users_clubss_0')



class Comments(db.Model):
    __tablename__ = 'comments'

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    content = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime, index=True, server_default=db.FetchedValue())

    user = db.relationship('Users', primaryjoin='Comments.user_id == Users.id', backref='commentss')
    discussions = db.relationship('Discussions', secondary='discussion_comment_link', backref='commentss')
    reviews = db.relationship('Reviews', secondary='review_comment_link', backref='commentss')


class DiscussionCommentLink(Comments):
    __tablename__ = 'discussion_comment_link'

    discussion_id = db.Column(db.ForeignKey('discussions.id', ondelete='CASCADE', onupdate='CASCADE'), index=True)
    comment_id = db.Column(db.ForeignKey('comments.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True)

    discussion = db.relationship('Discussions', primaryjoin='DiscussionCommentLink.discussion_id == Discussions.id', backref='discussion_comment_links')



class DiscussionBookLink(db.Model):
    __tablename__ = 'discussion_book_link'

    discussion_id = db.Column(db.ForeignKey('discussions.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)

    book = db.relationship('Books', primaryjoin='DiscussionBookLink.book_id == Books.id', backref='discussion_book_links')
    discussion = db.relationship('Discussions', primaryjoin='DiscussionBookLink.discussion_id == Discussions.id', backref='discussion_book_links')



class Discussions(db.Model):
    __tablename__ = 'discussions'

    id = db.Column(db.Integer, primary_key=True)
    owner_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    topic = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    owner = db.relationship('Users', primaryjoin='Discussions.owner_id == Users.id', backref='discussionss')



class ReviewCommentLink(db.Model):
    __tablename__ = 'review_comment_link'

    review_id = db.Column(db.ForeignKey('reviews.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    comment_id = db.Column(db.ForeignKey('comments.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)

    comment = db.relationship('Comments', primaryjoin='ReviewCommentLink.comment_id == Comments.id', backref='review_comment_links')
    review = db.relationship('Reviews', primaryjoin='ReviewCommentLink.review_id == Reviews.id', backref='review_comment_links')



class Reviews(db.Model):
    __tablename__ = 'reviews'
    __table_args__ = (
        db.Index('reviews_user_id', 'user_id', 'book_id'),
    )

    id = db.Column(db.Integer, primary_key=True)
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



class UserBooks(db.Model):
    __tablename__ = 'user_books'
    __table_args__ = (
        db.Index('idx_ubl_reviewed', 'user_id', 'reviewed'),
        db.Index('idx_ubl_reverse', 'book_id', 'user_id')
    )

    user_id = db.Column(db.ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    reading_status = db.Column(db.Enum('pending', 'reading', 'complete'), index=True)
    reviewed = db.Column(db.Integer, nullable=False, server_default=db.FetchedValue())
    ignored = db.Column(db.Integer, nullable=False, server_default=db.FetchedValue())
    liked = db.Column(db.Integer)
    current_page = db.Column(db.Integer)

    book = db.relationship('Books', primaryjoin='UserBooks.book_id == Books.id', backref='user_bookss')
    user = db.relationship('Users', primaryjoin='UserBooks.user_id == Users.id', backref='user_bookss')



class Users(db.Model):
    __tablename__ = 'users'

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    password_hash = db.Column(db.String(64, 'ascii_bin'))
    description = db.Column(db.String(256, 'utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)
