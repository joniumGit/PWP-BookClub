import pytest
from sqlalchemy.orm import Session

# noinspection PyUnresolvedReferences
from api_test import db
from bookclub.database import data_access as internal
from bookclub.resources import models as external
from bookclub.utils import *


def create_user(db: Session) -> external.User:
    return internal.get_user(internal.create_user(external.User(username="test", description="test"), db), db)


def create_book(db: Session) -> external.Book:
    return internal.get_book(internal.create_book(external.Book(handle="book", full_name="full-name"), db), db)


def test_create_book(db: Session):
    assert create_book(db) is not None
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            create_book(db)


def test_create_user(db: Session):
    assert create_user(db) is not None
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            create_user(db)


def test_user_book(db: Session):
    user = create_user(db)
    book = create_book(db)
    db.commit()
    ubl = internal.store_user_book(
        external.UserBookIncomingModel(
            user=user.username,
            handle=book.handle,
            reading_status='pending'
        ),
        db
    )
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            internal.store_user_book(external.UserBookIncomingModel(**ubl.dict()), db)
    assert ubl is not None
    return ubl


def test_get_book(db: Session):
    ubl = test_user_book(db)
    book = internal.get_book(ubl.handle, db)
    assert book is not None
    book_with_stats = internal.get_book(ubl.handle, db, stats=True)
    assert isinstance(book_with_stats, external.StatBook)
    assert book_with_stats.pending == 1
    ubl_and_stats = internal.get_book(ubl.handle, db, stats=True, user=ubl.user)
    assert ubl_and_stats.reading_status == 'pending'
    assert ubl_and_stats.pending == 1
    ubl_book = internal.get_book(ubl.handle, db, stats=False, user=ubl.user)
    assert ubl_book.reading_status == 'pending'
    with pytest.raises(NotFound):
        db.delete(internal._get_user(ubl.user, db))
        internal.get_book(ubl.handle, db, stats=False, user=ubl.user)


def test_delete_book(db: Session):
    book = create_book(db)
    internal.delete_book(book, db)
    with pytest.raises(NotFound):
        internal.get_book(book.handle, db)
