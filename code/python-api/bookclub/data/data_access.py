"""
Utilities for interacting with the database

No checking for duplicates is needed, all public function handle it by themselves.
On any error (duplicate, missing etc.) a HTTPError is thrown with an appropriate error code
"""
from typing import Union, Tuple, Optional, Type, Any, TypeVar, Dict, NoReturn, Set

from sqlalchemy import text
from sqlalchemy.engine import Connection, Row
from sqlalchemy.exc import NoResultFound
from sqlalchemy.orm import Session, DeclarativeMeta

from .model import data_models as external
from .model import db_models as internal
from ..utils import *

T = TypeVar('T', bound=DeclarativeMeta)


#
# Common checker functions for getting important values and handle not found states
#

def __get_handle(cls: Type[T], handle: str, db: Session, throw: bool) -> Optional[T]:
    """
    Get any orm class by handle attribute

    :param cls:     ORM Class
    :param handle:  Value of handle to filter by
    :param db:      ORM Session
    :param throw:   Throw a HTTPException on failure
    :return:        Instance or None if not throw
    """
    res = db.query(cls).where(getattr(cls, 'handle') == handle).first()
    if res:
        if res.deleted:
            raise NotFound(f"{cls.__name__} deleted: {handle}")
        return res
    else:
        if throw:
            raise NotFound(f"{cls.__name__} not found: {handle}")
        else:
            return None


def _get_user(username: str, db: Session, throw: bool = True) -> Optional[internal.User]:
    """
    Get a user

    :param username:    Username
    :param db:          ORM Session
    :param throw:       Throw a HTTPException on failure
    :return:            Instance or None if not throw
    """
    res = db.query(internal.User).where(internal.User.username == username).first()
    if res:
        if res.deleted:
            raise NotFound(f"User deleted: {username}")
        return res
    else:
        if throw:
            raise NotFound(f"User not found: {username}")
        else:
            return None


def _get_book(handle: str, db: Session, throw: bool = True) -> Optional[internal.Book]:
    """
    Get a Book

    :param handle:  Book handle
    :param db:      ORM Session
    :param throw:   Throw a HTTPException on failure
    :return:        Instance or None if not throw
    """
    return __get_handle(internal.Book, handle, db, throw)


def _get_club(handle: str, db: Session, throw: bool = True) -> Optional[internal.Club]:
    """
    Get a club

    :param handle:  Club handle
    :param db:      ORM Session
    :param throw:   Throw a HTTPException on failure
    :return:        Instance or None if not throw
    """
    return __get_handle(internal.Club, handle, db, throw)


def _get_comment(uuid: int, db: Session, throw: bool = True) -> Optional[internal.Comment]:
    """
    Get a comment

    :param uuid:    Comment uuid
    :param db:      ORM Session
    :param throw:   Throw a HTTPException on failure
    :return:        Instance or None if not throw
    """
    res = db.query(internal.Comment).where(internal.Comment.uuid == uuid).first()
    if res:
        return res
    else:
        if throw:
            raise NotFound(f"Comment not found: {uuid}")
        else:
            return None


def _get_review(user: str, book: str, db: Session, throw: bool = True) -> Optional[internal.Review]:
    """
    Get a review

    :param user:    Username
    :param book:    Book handle
    :param db:      ORM session
    :param throw:   Throws a HTTPException if not found (default: True)
    :return:        A Review, or None if not throw
    """
    res = db.query(internal.Review).where(internal.Review.user == user).where(internal.Review.book == book).first()
    if res:
        return res
    else:
        if throw:
            raise NotFound(f"Review not found: ({user}, {book})")
        else:
            return None


def _check_handle_available(cls: Type[T], handle: str, db: Session) -> NoReturn:
    """
    Check availability of a handle and throw an error if it already exists

    :param cls:     ORM Class
    :param handle:  Handle to check
    :param db:      ORM Session
    """
    if db.query(cls).where(getattr(cls, 'handle') == handle).count() != 0:
        raise AlreadyExists(f"{cls.__name__} with handle {handle} already exists")


def _add(
        e: Any,
        i: Type[T],
        db: Session,
        exclude: Set[str] = None,
        extra: Dict[str, Any] = None
) -> T:
    """
    Add an ORM entity to session

    :param e:   External entity model
    :param i:   Internal entity model
    :param db:  ORM Session
    :return:    Created internal entity
    """
    if extra is None:
        extra = {}
    with db.begin_nested():
        no = i(**e.dict(exclude_none=True, exclude=exclude), **extra)
        db.add(no)
        return no


def _modify(i: T, d: Dict[str, Any], db: Session) -> bool:
    """
    Modify orm instance in a transaction

    :param i:   Instance to modify
    :param d:   Dict for field references
    :param db:  ORM Session
    :return:    Whether the object changed as a result
    """
    with db.begin_nested():
        changed = False
        for k in d:
            if getattr(i, k) != d[k]:
                changed = True
                setattr(i, k, d[k])
        return changed


#
# BOOK
#
def create_book(book: external.Book, db: Session) -> str:
    """
    Create a Book

    :param book: External book model
    :param db:   ORM Session
    :return:     Handle of the newly created book
    """
    _check_handle_available(internal.Book, book.handle, db)
    return _add(book, internal.Book, db).handle


def update_book(old_handle: str, book: external.Book, db: Session) -> Optional[str]:
    """
    Updates a book

    :param old_handle:  Old handle of the book
    :param book:        External book model
    :param db:          ORM Session
    :return:            Handle of the modified book if the resource changed, else None
    """
    if old_handle != book.handle:
        _check_handle_available(internal.Book, book.handle, db)
    b = _get_book(old_handle, db)
    d = book.dict(exclude_none=True)
    return book.handle if _modify(b, d, db) else None


def get_book(handle: str, db: Session, stats: bool = False, user: Union[str, external.User] = None) -> Union[
    external.Book,
    external.UserBook,
    external.StatBook,
    external.StatUserBook
]:
    """
    Multi-getter for all kinds of book models

    :param handle:  Book handle
    :param db:      ORM Session
    :param stats:   Whether stats should be included (default: false)
    :param user:    User for which book data should be included (default: None)
    :return:        See typehint
    """
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


def delete_book(book: Union[str, external.Book], db: Session) -> NoReturn:
    """
    Soft delete a book

    :param book:    Book (handle or instance)
    :param db:      ORM Session
    """
    handle: str
    if isinstance(book, external.Book):
        handle = book.handle
    else:
        handle = book
    b = _get_book(handle, db)
    with db.begin_nested():
        b.deleted = 1


#
# COMMENT
#
def create_comment(comment: external.NewComment, db: Session) -> int:
    """
    Create a comment

    :param comment: External comment model
    :param db:      ORM Session
    :return:        UUID of the newly created comment
    """
    u = _get_user(comment.user, db)
    return _add(
        comment,
        internal.Comment,
        db,
        exclude={'user'},
        extra={'user_id': u.id}
    ).uuid


def update_comment(comment: external.Comment, db: Session) -> bool:
    """
    Updates a comment (UUID ignored)

    :param comment: External comment model
    :param db:      ORM Session
    :return:        Whether the resource changed as a result
    """
    c = _get_comment(comment.uuid, db)
    if comment.user is not None:
        _get_user(c, db)
    d = comment.dict(exclude_none=True, exclude={'uuid'})
    return _modify(c, d, db)


def get_comment(uuid: int, db: Session) -> external.Comment:
    """
    Get a comment

    :param uuid: UUID of the comment
    :param db:   ORM Session
    :return:     External comment model
    """
    c = _get_comment(uuid, db)
    return external.Comment(
        user=c.user.username if c.user_id is not None else None,
        **external.CommentInternalBase.from_orm(c).dict(exclude_none=True)
    )


def delete_comment(comment: Union[int, external.Comment], db: Session) -> NoReturn:
    """
    Soft delete a comment

    :param comment: Comment (uuid or instance)
    :param db:      ORM Session
    """
    c = _get_comment(comment.uuid, db)
    with db.begin_nested():
        c.deleted = 1


#
# Review
#
def create_review(review: external.Review, db: Session) -> Tuple[str, str]:
    """
    Create a review

    :param review:  External review model
    :param db:      ORM Session
    :return:        Tuple(username, book_handle)
    """
    u = _get_user(review.user, db)
    b = _get_book(review.book, db)
    _add(
        review,
        internal.Review,
        db,
        exclude={'user', 'book'},
        extra={
            'user_id': u.id,
            'book_id': b.id
        }
    )
    return u.username, b.handle


def update_review(review: external.Review, db: Session) -> bool:
    """
    Updates a review (book and user are ignored)

    :param review:  External review model
    :param db:      ORM Session
    :return:        Whether the resource changed as a result
    """
    r = _get_review(review.user, review.book, db)
    d = review.dict(exclude_none=True, exclude={'user', 'book'})
    return _modify(r, d, db)


def get_review(user: Union[str, external.User], book: Union[str, external.Book], db: Session) -> external.Review:
    """
    Get a review

    :param user:    User (username or model)
    :param book:    Book (handle or model)
    :param db:      ORM Session
    :return:        External review model
    """
    u: internal.User
    b: internal.Book

    if isinstance(user, str):
        u = _get_user(user, db)
    else:
        u = _get_user(user.username, db)
    if isinstance(book, str):
        b = _get_book(book, db)
    else:
        b = _get_book(book.handle, db)

    return external.Review(
        user=u.username,
        book=b.handle,
        **external.ReviewInternalBase.from_orm(_get_review(u.username, b.handle, db)).dict(exclude_none=True)
    )


def delete_review(
        review: Union[external.Review, Tuple[Union[str, external.Book], Union[str, external.User]]],
        db: Session
) -> NoReturn:
    """
    Soft deletes a review

    :param review:  External review model or a Tuple(Book, User)
    :param db:      ORM Session
    """
    r: internal.Review
    if isinstance(review, tuple):
        up = review[1]
        u: internal.User
        if isinstance(up, str):
            u = _get_user(up, db)
        else:
            u = _get_user(up.username, db)
        bp = review[0]
        b: internal.Book
        if isinstance(bp, str):
            b = _get_book(bp, db)
        else:
            b = _get_book(bp.handle, db)
        r = _get_review(u.username, b.handle, db)
        with db.begin_nested():
            r.deleted = 1


#
# Users
#
def create_user(user: external.User, db: Session) -> str:
    """
    Create a user

    :param user:    External user model
    :param db:      ORM Session
    :return:        Username of the newly crated user
    """
    if db.query(internal.User).where(internal.User.username == user.username).count() != 0:
        raise AlreadyExists(f"Username {user.username} is taken")
    else:
        return _add(user, internal.User, db).username


def update_user(old_username: str, user: external.User, db: Session) -> Optional[str]:
    """
    Updates a user

    :param old_username:    Old username
    :param user:            External user model
    :param db:              ORM Session
    :return:                Username of the modified resource or None if it didn't change
    """
    if old_username != user.username:
        if db.query(internal.User).where(internal.User.username == user.username).count() != 0:
            raise AlreadyExists(f"Username {user.username} is taken")
    u = _get_user(old_username, db)
    changed = False
    d = user.dict(exclude_none=True)
    with db.begin_nested():
        for k in d:
            if getattr(u, k) != d[k]:
                changed = True
                setattr(u, k, d[k])
    return user.username if changed else None


def get_user(username: str, db: Session) -> external.User:
    """
    Get user

    :param username:    Username
    :param db:          ORM Session
    :return:            External user model
    """
    return external.User.from_orm(_get_user(username, db))


def delete_user(user: Union[str, external.User], db: Session) -> NoReturn:
    """
    Soft delete a user

    :param user:    User (username or model)
    :param db:      ORM Session
    """
    if user is not None:
        username: str
        if isinstance(user, external.User):
            username = user.username
        else:
            username = user
        u = _get_user(username, db)
        with db.begin_nested():
            u.deleted = 1


#
# Club
#
def create_club(club: external.Club, db: Session) -> str:
    """
    Create a club

    :param club:    External club model
    :param db:      ORM Session
    :return:        Newly created club handle
    """
    _check_handle_available(internal.Club, club.handle, db)
    owner = None
    if club.owner is not None:
        owner = _get_user(club.owner, db)
    return _add(
        club,
        internal.Club,
        db,
        exclude={'owner'},
        extra={'owner_id': owner.id if owner is not None else None}
    ).handle


def update_club(old_handle: str, club: external.Club, db: Session) -> Optional[str]:
    """
    Updates a club

    :param old_handle:  Old handle
    :param club:        External club model
    :param db:          ORM Session
    :return:            Handle of modified resource or None if no change happened
    """
    if old_handle != club.handle:
        _check_handle_available(internal.Club, club.handle, db)
    c = _get_club(old_handle, db)
    owner = _get_user(club.owner, db)
    d = club.dict(exclude_none=True, exclude={'owner'})
    with db.begin_nested():
        if c.owner_id != owner.id:
            changed = True
            c.owner_id = owner.id
        changed = changed or _modify(c, d, db)
        return club.handle if changed else None


def get_club(handle: str, db: Session) -> external.Club:
    """
    Get a club

    :param handle:  Club handle
    :param db:      ORM Session
    :return:        External Club model
    """
    c = _get_club(handle, db)
    return external.Club(
        owner=c.owner.username if c.owner_id is not None else None,
        **external.ClubInternalBase.from_orm(c).dict(exclude_none=True)
    )


def delete_club(club: Union[str, external.Club], db: Session) -> NoReturn:
    """
    Soft delete a club

    :param club:    Club
    :param db:      ORM Session
    """
    c: internal.Club
    if isinstance(club, str):
        c = _get_club(club, db)
    else:
        c = _get_club(club.handle, db)
    with db.begin_nested():
        c.deleted = 1


#
# User book
#
def store_user_book(
        model: external.UserBookIncomingModel,
        db: Session,
        overwrite: bool = False
) -> external.UserBook:
    """
    Function for storing user book records

    Mainly intended for creating and updating user book records.

    :param model:       UBL model
    :param db:          ORM Session
    :param overwrite:   Whether to overwrite (update) existing records
    :return:            User book instance
    """
    u = _get_user(model.user, db)
    b = _get_book(model.handle, db)

    existing = db.query(internal.UserBook).where(
        internal.UserBook.user_id == u.id
    ).where(
        internal.UserBook.book_id == b.id
    ).first()

    d = model.dict(exclude_none=True, exclude={'user', 'handle'})
    with db.begin_nested():
        new_record: internal.UserBook
        if existing and not overwrite:
            raise AlreadyExists(f"User {u.username} already has a record for {b.handle}")
        else:
            if existing:
                new_record = existing
                for k in d:
                    setattr(new_record, k, d[k])
            else:
                new_record = internal.UserBook(user_id=u.id, book_id=b.id, **d)
        if not existing:
            with db.begin_nested():
                db.add(new_record)
        db.refresh(new_record)
        # Its not dumb if it works, right?
        return external.UserBook(
            **external.UserBookInternalBase.from_orm(new_record).dict(exclude_none=True),
            **external.Book.from_orm(new_record.book).dict(exclude_none=True),
            user=new_record.user.username,
        )


def modify_user_book_ignore_status(
        db: Session,
        user: Union[str, external.User] = None,
        book: Union[str, external.Book] = None,
        ubl: external.UserBook = None,
        ignored: bool = True
) -> NoReturn:
    """
    User book ignore status helper
    Needs to know the UBL model or user and book pair

    Basically a soft delete
    :param ubl:         UserBook model
    :param user:        User
    :param book:        Book
    :param db:          ORM Session
    :param ignored:     Ignored status (default: True)
    """
    b = _get_book((book if isinstance(book, str) else book.handle) if ubl is None else ubl.handle, db)
    u = _get_user((user if isinstance(user, str) else user.username) if ubl is None else ubl.user, db)
    with db.begin_nested():
        try:
            r: internal.UserBook = db.query(internal.UserBook).where(
                internal.UserBook.user_id == u.id
            ).where(
                internal.UserBook.book_id == b.id
            ).one()
            r.ignored = ignored
        except NoResultFound:
            raise NotFound(f"User {u.username} has no record for {b.handle}")


__all__ = [
    'get_club',
    'get_user',
    'get_book',
    'get_comment',
    'get_review',
    'delete_club',
    'delete_user',
    'delete_book',
    'delete_review',
    'delete_comment',
    'update_club',
    'update_user',
    'update_review',
    'update_book',
    'update_comment',
    'create_club',
    'create_user',
    'create_book',
    'create_review',
    'create_comment',
    'modify_user_book_ignore_status',
    'store_user_book'
]
