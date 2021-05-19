import random as rnd
import string

import pytest
from sqlalchemy.orm import Session

# noinspection PyUnresolvedReferences
from api_test import db
from bookclub import data as da
from bookclub.utils import *


# TODO:
# Needed:
#   - User: update
#   - Book: get, update
#   - Club: update
#   - Review: create, get, update, delete
#   - Comment: create, get, update, delete
#   - UBL:

@pytest.fixture()
def handle() -> str:
    return ''.join([rnd.choice(string.ascii_letters + string.digits) for _ in range(0, rnd.randint(1, 60))])


@pytest.fixture(name='book')
def create_book(handle: str, db: Session) -> str:
    ext_book = da.NewBook(
        handle=handle,
        full_name=handle * 2,
        pages=rnd.randint(1, 10000),
        description="A test book",
    )
    return da.create_book(ext_book, db)


@pytest.fixture(name='user')
def create_user(handle: str, db: Session) -> str:
    ext_user = da.NewUser(
        username=handle,
        description="A test user"
    )
    return da.create_user(ext_user, db)


def test_book_create(handle: str, db: Session):
    ext_book = da.NewBook(
        handle=handle,
        full_name=handle * 2,
        pages=rnd.randint(1, 10000),
        description="A test book",
    )
    nh = da.create_book(ext_book, db)
    book = da.get_book(nh, db)
    assert book.handle == ext_book.handle
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            da.create_book(ext_book, db)


def test_user_create(handle: str, db: Session):
    ext_user = da.NewUser(
        username=handle,
        description="A test user"
    )
    uname = da.create_user(ext_user, db)
    user = da.get_user(uname, db)
    assert user.username == ext_user.username
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            da.create_user(ext_user, db)


@pytest.fixture(name='club')
def create_club(handle: str, user: str, db: Session):
    ext_club = da.NewClub(
        handle=handle,
        description=handle * 2,
        owner=user
    )
    return da.create_club(ext_club, db)


def test_club_create(handle: str, user: str, db: Session):
    ext_club = da.NewClub(
        handle=handle,
        description=handle * 2,
        owner=user
    )
    ch = da.create_club(ext_club, db)
    club = da.get_club(ch, db)
    assert club.handle == ext_club.handle
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            da.create_club(ext_club, db)


@pytest.fixture(name='ubl')
def user_book(user: str, book: str, db: Session) -> da.UserBook:
    db.commit()
    da.store_user_book(
        da.NewUserBook(
            user=user,
            handle=book,
            reading_status=da.StatusEnum.pending,
            ignored=False,
            liked=True,
            reviewed=False,
            current_page=1
        ),
        db
    )
    return da.get_book(book, db, user=user)


def test_ubl_create(ubl: da.NewUserBook, db: Session):
    with pytest.raises(AlreadyExists):
        with db.begin_nested():
            da.store_user_book(da.NewUserBook(**ubl.dict()), db)
    da.store_user_book(da.NewUserBook(**ubl.dict()), db, overwrite=True)
    assert ubl is not None


#
# GET
#

def test_book_get(ubl: da.NewUserBook, db: Session):
    book = da.get_book(ubl.handle, db)
    assert book is not None
    book_with_stats = da.get_book(ubl.handle, db, stats=True)
    assert isinstance(book_with_stats, da.StatBook)
    assert book_with_stats.pending == 1
    ubl_and_stats = da.get_book(ubl.handle, db, stats=True, user=ubl.user)
    assert ubl_and_stats.reading_status == 'pending'
    assert ubl_and_stats.pending == 1
    ubl_book = da.get_book(ubl.handle, db, stats=False, user=ubl.user)
    assert ubl_book.reading_status == 'pending'
    with pytest.raises(NotFound):
        from bookclub.data.data_access import _get_user
        db.delete(_get_user(ubl.user, db))
        da.get_book(ubl.handle, db, stats=False, user=ubl.user)


#
# DELETE
#

def test_delete_book(book: str, db: Session):
    bb = da.get_book(book, db)
    da.delete_book(bb, db)
    with pytest.raises(NotFound):
        da.delete_club(book, db)


def test_delete_user(user: str, db: Session):
    uu = da.get_user(user, db)
    da.delete_user(uu, db)
    with pytest.raises(NotFound):
        da.delete_user(user, db)


def test_delete_ubl(ubl: da.NewUserBook, db: Session):
    da.modify_user_book_ignore_status(db, ubl=ubl)
    assert da.get_book(ubl.handle, db, user=ubl.user).ignored
    da.modify_user_book_ignore_status(db, user=ubl.user, book=ubl.handle, ignored=False)
    assert not da.get_book(ubl.handle, db, user=ubl.user).ignored


def test_delete_club(club: str, db: Session):
    c = da.get_club(club, db)
    da.delete_club(c, db)
    with pytest.raises(NotFound):
        da.delete_club(club, db)
