import random as rnd
import string

import pytest
from sqlalchemy.orm import Session

# noinspection PyUnresolvedReferences
from api_test import db
from bookclub.database import data_access as internal
from bookclub.resources import models as external
from bookclub.utils import *


@pytest.fixture()
def handle() -> str:
    return ''.join([rnd.choice(string.ascii_letters + string.digits) for x in range(0, rnd.randint(1, 60))])


@pytest.fixture(name='book')
def create_book(handle: str, db: Session) -> str:
    ext_book = external.Book(
        handle=handle,
        full_name=handle * 2,
        pages=rnd.randint(1, 10000),
        description="A test book",
    )
    return internal.create_book(ext_book, db)


@pytest.fixture(name='user')
def create_user(handle: str, db: Session) -> str:
    ext_user = external.User(
        username=handle,
        description="A test user"
    )
    return internal.create_user(ext_user, db)


def test_book_create(handle: str, db: Session):
    ext_book = external.Book(
        handle=handle,
        full_name=handle * 2,
        pages=rnd.randint(1, 10000),
        description="A test book",
    )
    nh = internal.create_book(ext_book, db)
    book = internal.get_book(nh, db)
    assert book == ext_book
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            internal.create_book(ext_book, db)


def test_user_create(handle: str, db: Session):
    ext_user = external.User(
        username=handle,
        description="A test user"
    )
    uname = internal.create_user(ext_user, db)
    user = internal.get_user(uname, db)
    assert user == ext_user
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            internal.create_user(ext_user, db)


@pytest.fixture(name='club')
def create_club(handle: str, user: str, db: Session):
    ext_club = external.Club(
        handle=handle,
        description=handle * 2,
        owner=user
    )
    return internal.create_club(ext_club, db)


def test_club_create(handle: str, user: str, db: Session):
    ext_club = external.Club(
        handle=handle,
        description=handle * 2,
        owner=user
    )
    ch = internal.create_club(ext_club, db)
    club = internal.get_club(ch, db)
    assert club == ext_club
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            internal.create_club(ext_club, db)


@pytest.fixture(name='ubl')
def user_book(user: str, book: str, db: Session) -> external.UserBook:
    db.commit()
    internal.store_user_book(
        external.UserBookIncomingModel(
            user=user,
            handle=book,
            reading_status=external.StatusEnum.pending,
            ignored=False,
            liked=True,
            reviewed=False,
            current_page=1
        ),
        db
    )
    return internal.get_book(book, db, user=user)


def test_ubl_create(ubl: external.UserBook, db: Session):
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            internal.store_user_book(external.UserBookIncomingModel(**ubl.dict()), db)
    internal.store_user_book(external.UserBookIncomingModel(**ubl.dict()), db, overwrite=True)
    assert ubl is not None


#
# GET
#

def test_book_get(ubl: external.UserBook, db: Session):
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


#
# DELETE
#

def test_delete_book(book: str, db: Session):
    bb = internal.get_book(book, db)
    internal.delete_book(bb, db)
    with pytest.raises(NotFound):
        internal.delete_club(book, db)


def test_delete_user(user: str, db: Session):
    uu = internal.get_user(user, db)
    internal.delete_user(uu, db)
    with pytest.raises(NotFound):
        internal.delete_user(user, db)


def test_delete_ubl(ubl: external.UserBook, db: Session):
    internal.modify_user_book_ignore_status(db, ubl=ubl)
    assert internal.get_book(ubl.handle, db, user=ubl.user).ignored
    internal.modify_user_book_ignore_status(db, user=ubl.user, book=ubl.handle, ignored=False)
    assert not internal.get_book(ubl.handle, db, user=ubl.user).ignored


def test_delete_club(club: str, db: Session):
    c = internal.get_club(club, db)
    internal.delete_club(c, db)
    with pytest.raises(NotFound):
        internal.delete_club(club, db)
