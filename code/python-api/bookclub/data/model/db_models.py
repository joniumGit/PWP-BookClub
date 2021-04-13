# THIS FILE IS GENERATED
#
# sqlacodegen mysql+pymysql://root:test@localhost:6969/book_club
#
# ONLY THIS HEADER HAS BEEN ADDED ON 19.3.2021
#
# DO NOT MODIFY
#
# coding: utf-8
from sqlalchemy import Column, DECIMAL, DateTime, ForeignKey, Index, String, Table, Text, text
from sqlalchemy.dialects.mysql import BIGINT, ENUM, INTEGER, TINYINT, VARCHAR
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()
metadata = Base.metadata


class Book(Base):
    __tablename__ = 'books'

    id = Column(INTEGER(11), primary_key=True)
    handle = Column(String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    full_name = Column(String(256, 'utf8mb4_unicode_ci'), nullable=False)
    description = Column(Text(collation='utf8mb4_unicode_ci'))
    pages = Column(INTEGER(11), index=True)
    deleted = Column(TINYINT(1), nullable=False, index=True, server_default=text("0"))
    created_at = Column(DateTime, server_default=text("current_timestamp()"))
    updated_at = Column(DateTime)

    clubs = relationship('Club', secondary='club_book_link')


t_books_statistics = Table(
    'books_statistics', metadata,
    Column('handle', String(64)),
    Column('rating', DECIMAL(7, 2), server_default=text("'0.00'")),
    Column('readers', BIGINT(21), server_default=text("'0'")),
    Column('completed', BIGINT(21), server_default=text("'0'")),
    Column('pending', BIGINT(21), server_default=text("'0'")),
    Column('liked', BIGINT(21), server_default=text("'0'")),
    Column('disliked', BIGINT(21), server_default=text("'0'"))
)


class User(Base):
    __tablename__ = 'users'

    id = Column(INTEGER(11), primary_key=True)
    username = Column(String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    password_hash = Column(VARCHAR(64))
    description = Column(String(256, 'utf8mb4_unicode_ci'))
    deleted = Column(TINYINT(1), nullable=False, index=True, server_default=text("0"))
    created_at = Column(DateTime, server_default=text("current_timestamp()"))
    updated_at = Column(DateTime)


class Club(Base):
    __tablename__ = 'clubs'

    id = Column(INTEGER(11), primary_key=True)
    owner_id = Column(ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    handle = Column(String(64, 'utf8mb4_unicode_ci'), nullable=False, unique=True)
    description = Column(String(2048, 'utf8mb4_unicode_ci'))
    deleted = Column(TINYINT(1), nullable=False, index=True, server_default=text("0"))
    created_at = Column(DateTime, server_default=text("current_timestamp()"))
    updated_at = Column(DateTime)

    owner = relationship('User')
    users = relationship('User', secondary='club_user_link')


class Comment(Base):
    __tablename__ = 'comments'

    id = Column(INTEGER(11), primary_key=True)
    uuid = Column(BIGINT(20), nullable=False, unique=True, server_default=text("uuid_short()"))
    user_id = Column(ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'), index=True)
    content = Column(Text(collation='utf8mb4_unicode_ci'))
    deleted = Column(TINYINT(1), nullable=False, index=True, server_default=text("0"))
    created_at = Column(DateTime, server_default=text("current_timestamp()"))
    updated_at = Column(DateTime, index=True, server_default=text("current_timestamp() ON UPDATE current_timestamp()"))

    user = relationship('User')
    reviews = relationship('Review', secondary='review_comment_link')


class Review(Base):
    __tablename__ = 'reviews'
    __table_args__ = (
        Index('reviews_user_id', 'user_id', 'book_id', unique=True),
    )

    id = Column(INTEGER(11), primary_key=True)
    user_id = Column(ForeignKey('users.id', ondelete='SET NULL', onupdate='CASCADE'))
    book_id = Column(ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), nullable=False, index=True)
    stars = Column(TINYINT(4), nullable=False, server_default=text("3"))
    title = Column(String(128, 'utf8mb4_unicode_ci'), nullable=False)
    content = Column(Text(collation='utf8mb4_unicode_ci'))
    deleted = Column(TINYINT(1), nullable=False, server_default=text("0"))
    created_at = Column(DateTime, server_default=text("current_timestamp()"))
    updated_at = Column(DateTime)

    book = relationship('Book')
    user = relationship('User')


class UserBook(Base):
    __tablename__ = 'user_books'
    __table_args__ = (
        Index('idx_ubl_reviewed', 'user_id', 'reviewed'),
        Index('idx_ubl_reverse', 'book_id', 'user_id')
    )

    user_id = Column(ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    book_id = Column(ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False)
    reading_status = Column(ENUM('pending', 'reading', 'complete'), index=True)
    reviewed = Column(TINYINT(1), nullable=False, server_default=text("0"))
    ignored = Column(TINYINT(1), nullable=False, server_default=text("0"))
    liked = Column(TINYINT(1))
    current_page = Column(INTEGER(11))

    book = relationship('Book')
    user = relationship('User')


t_club_book_link = Table(
    'club_book_link', metadata,
    Column('club_id', ForeignKey('clubs.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False),
    Column('book_id', ForeignKey('books.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)
)


t_club_user_link = Table(
    'club_user_link', metadata,
    Column('club_id', ForeignKey('clubs.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False),
    Column('user_id', ForeignKey('users.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)
)


t_review_comment_link = Table(
    'review_comment_link', metadata,
    Column('review_id', ForeignKey('reviews.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False),
    Column('comment_id', ForeignKey('comments.id', ondelete='CASCADE', onupdate='CASCADE'), primary_key=True, nullable=False, index=True)
)