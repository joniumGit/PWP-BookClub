from typing import Union, Tuple

from sqlalchemy import text
from sqlalchemy.engine import Connection, Row
from sqlalchemy.exc import NoResultFound
from sqlalchemy.orm import Session

from . import db_models as internal
from ..resources import models as external
from ..utils import *


#
# BOOK
#
def create_book(book: external.Book, db: Session) -> external.Book:
    if db.query(internal.Book).where(internal.Book.handle == book.handle).count() != 0:
        raise AlreadyExists(f"{book.handle} already exists")
    else:
        try:
            with db.begin_nested():
                new_book = internal.Book(**book.dict(exclude_none=True))
                db.add(new_book)
            return external.Book.from_orm(new_book)
        except Exception as e:
            logging.exception("What just happened with book creation?", exc_info=e)
            raise InternalError(f"Failed to create book {book.handle} because of an internal server error")


def update_book(book: external.Book, db: Session, overwrite: bool = False) -> external.Book:
    existing = db.query(internal.Book).where(internal.Book.handle == book.handle).first()
    if not overwrite:
        raise AlreadyExists(f"Book with handle {book.handle} already exists")
    try:
        with db.begin_nested():
            if existing is not None:
                for k in book.dict():
                    setattr(k, existing, getattr(book, k))
            else:
                existing = internal.Book(**book.dict(exclude_none=True))
                db.add(existing)
        return external.Book.from_orm(existing)
    except Exception as e:
        logging.exception("Uh,oh! Book update failed ._.", exc_info=e)
        raise InternalError(f"Failed to update book {book.handle}")


def get_book(handle: str, db: Session, stats: bool = False, user: Union[str, external.User] = None) -> Union[
    external.Book,
    external.UserBook,
    external.StatBook,
    external.StatUserBook
]:
    try:
        c: Connection = db.connection()
        if user is not None:
            username: str
            if isinstance(user, external.User):
                username = user.username
            else:
                username = user
            if db.query(internal.User).where(internal.User.username == username).count() != 1:
                raise NotFound(f"User not found {username}")
            if stats:
                s = text(
                    f"""
                    SELECT *, :uname as user FROM books b 
                    RIGHT JOIN user_books ubl ON ubl.book_id=b.id 
                    RIGHT JOIN books_statistics bs ON bs.handle=b.handle
                    WHERE b.handle=:handle
                    AND b.deleted != TRUE
                    AND ubl.user_id=(SELECT id FROM users WHERE username=:uname)
                    """
                )
                r: Row = c.execute(s, handle=handle, uname=username).one()
                return external.StatUserBook(**{k: v for k, v in dict(r).items() if v is not None})
            else:
                s = text(
                    f"""
                    SELECT *, :uname as user FROM books b 
                    RIGHT JOIN user_books ubl ON ubl.book_id=b.id
                    WHERE b.handle=:handle
                    AND b.deleted != TRUE
                    AND ubl.user_id=(SELECT id FROM users WHERE username=:uname)
                    """
                )
                r: Row = c.execute(s, handle=handle, uname=username).one()
                return external.UserBook(**{k: v for k, v in dict(r).items() if v is not None})
        else:
            if stats:
                s = text(
                    f"""
                    SELECT * FROM books b
                    RIGHT JOIN books_statistics bs ON bs.handle=b.handle
                    WHERE b.handle=:handle
                    AND b.deleted != TRUE
                    """
                )
                r: Row = c.execute(s, handle=handle).one()
                return external.StatBook(**{k: v for k, v in dict(r).items() if v is not None})
            else:
                result = db.query(internal.Book).where(
                    internal.Book.handle == handle
                ).where(

                    internal.Book.deleted != 1
                ).one()
                return external.Book.from_orm(result)
    except NoResultFound:
        raise NotFound(f"Failed to find data for book {handle}")
    except HTTPException as e:
        raise e
    except Exception as e:
        logging.exception("Oh no, the manual queries failed ._. fuck", exc_info=e)
        raise InternalError(f"Failed to find book with handle {handle}")


def delete_book(book: Union[str, external.Book], db: Session) -> None:
    handle: str
    if isinstance(book, external.Book):
        handle = book.handle
    else:
        handle = book
    try:
        with db.begin_nested():
            db.execute(text(f"UPDATE books SET deleted=TRUE WHERE handle=:handle"), {'handle': handle})
    except Exception as e:
        logging.exception(f"Failed to delete a book {handle}", exc_info=e)
        raise InternalError(f"Failed to delete book with handle {handle}")


#
# COMMENT
#

def update_comment(comment: external.Comment, db: Session) -> None:
    pass


def get_comment(uuid: int, db: Session) -> external.Comment:
    pass


def delete_comment(comment: Union[int, external.Comment]) -> None:
    pass


#
# Review
#

def update_review(review: external.Review, db: Session) -> None:
    pass


def get_review(user: Union[str, external.User], book: Union[str, external.Book], db: Session) -> external.Review:
    pass


def delete_review(
        review: Union[external.Review, Tuple[Union[str, external.Book], Union[str, external.User]]],
        db: Session
) -> None:
    pass


#
# Users
#
def create_user(user: external.User, db: Session) -> external.User:
    if db.query(internal.User).where(internal.User.username == user.username).count() != 0:
        raise AlreadyExists(f"Username {user.username} is taken")
    else:
        new_user = internal.User(**user.dict(exclude_none=True))
        try:
            with db.begin_nested():
                db.add(new_user)
            return external.User.from_orm(new_user)
        except Exception as e:
            logging.exception(f"Failed to create user {user.dict()}", exc_info=e)
            raise InternalError(f"Failed to create user {user.dict()}")


def update_user(user: external.User, db: Session) -> external.User:
    pass


def get_user(username: str, db: Session) -> external.User:
    pass


def delete_user(user: Union[str, external.User], db: Session) -> None:
    if user is not None:
        username: str
        if isinstance(user, external.User):
            username = user.username
        else:
            username = user
        try:
            u = db.query(internal.User).where(internal.User.username == username).one()
            db.delete(u)
        except NoResultFound:
            raise NotFound(f"User not found {username}")


#
# Club
#

def update_club(club: external.Club, db: Session) -> None:
    pass


def get_club(handle: str, db: Session) -> external.Club:
    pass


def delete_club(club: Union[str, external.Club]) -> None:
    pass


#
# User book
#
def store_user_book(
        user: Union[str, external.User],
        book: Union[str, external.Book],
        db: Session,
        overwrite: bool = False,
        **kwargs
) -> external.UserBook:
    username: str
    if isinstance(user, external.User):
        username = user.username
    else:
        username = user
    existing_user = db.query(internal.User).where(internal.User.username == username).first()
    if existing_user:
        user_id: int = existing_user.id
        handle: str
        if isinstance(book, str):
            handle = book
        else:
            handle = book.handle
        existing_book = db.query(internal.Book).where(internal.Book.handle == handle).first()
        if existing_book:
            book_id: int = existing_book.id
            existing = db.query(internal.UserBook).where(
                internal.UserBook.user_id == user_id
            ).where(
                internal.UserBook.book_id == book_id
            ).first()
            try:
                new_record = None
                with db.begin_nested():
                    if existing and not overwrite:
                        raise AlreadyExists(f"User {username} already has a record for {handle}")
                    else:
                        if existing:
                            new_record = existing
                            for k in kwargs:
                                setattr(new_record, k, kwargs[k])
                        else:
                            new_record = internal.UserBook(user_id=user_id, book_id=book_id, **kwargs)
                    if not existing:
                        db.add(new_record)
                # Its not dumb if it works, right?
                return external.UserBook(
                    **external.UserBookInternalBase.from_orm(new_record).dict(exclude_none=True),
                    **external.Book.from_orm(new_record.book).dict(exclude_none=True),
                    user=new_record.user.username,
                )
            except HTTPException as e:
                raise e
            except Exception as e:
                logging.exception(f"Failed user book operation ({username}, {handle})", exc_info=e)
                raise InternalError(f"Failed user book operation ({username}, {handle})")
        else:
            raise NotFound(f"Book with handle {handle} not found")
    else:
        raise NotFound(f"User {username} not found")
