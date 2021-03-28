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

    id = db.Column(db.Integer, primary_key=True)
    handle = db.Column(db.String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    full_name = db.Column(db.String(256, 'utf8mb4_unicode_ci'), nullable=False)
    description = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    pages = db.Column(db.Integer, index=True)
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime)

    clubs = db.relationship('Clubs', secondary='club_book_link', backref='bookss')



class ClubBookLink(db.Model):
    __tablename__ = 'club_book_link'

    club_id = db.Column(db.ForeignKey('clubs.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)

    book = db.relationship('Books', primaryjoin='ClubBookLink.book_id == Books.id', backref='club_book_links')
    club = db.relationship('Clubs', primaryjoin='ClubBookLink.club_id == Clubs.id', backref='club_book_links')



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
    users = db.relationship('Users', secondary='club_user_link', backref='users_clubss_0')



class Comments(db.Model):
    __tablename__ = 'comments'

    id = db.Column(db.Integer, primary_key=True)
    uuid = db.Column(db.BigInteger, nullable=False, unique=True, server_default=db.FetchedValue())
    user_id = db.Column(db.ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    content = db.Column(db.Text(collation='utf8mb4_unicode_ci'))
    deleted = db.Column(db.Integer, nullable=False, index=True, server_default=db.FetchedValue())
    created_at = db.Column(db.DateTime, server_default=db.FetchedValue())
    updated_at = db.Column(db.DateTime, index=True, server_default=db.FetchedValue())

    user = db.relationship('Users', primaryjoin='Comments.user_id == Users.id', backref='commentss')
    reviews = db.relationship('Reviews', secondary='review_comment_link', backref='commentss')



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
    book_id = db.Column(db.ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), nullable=False, index=True)
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
        db.Index('idx_ubl_reverse', 'book_id', 'user_id'),
        db.Index('idx_ubl_reviewed', 'user_id', 'reviewed')
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
