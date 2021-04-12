from typing import Generator

# noinspection PyUnresolvedReferences
from fastapi import Depends
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
# noinspection PyUnresolvedReferences
from sqlalchemy.exc import IntegrityError
# noinspection PyUnresolvedReferences
from sqlalchemy.orm import sessionmaker, Session

# noinspection PyUnresolvedReferences
from . import utils as crud
# noinspection PyUnresolvedReferences
from .db_models import (
    Book as BDBook,
    Club as DBClub,
    User as DBUser,
    Comment as DBComment,
    Review as DBReview,
    UserBook as DBUserBook,
    t_books_statistics as book_stats
)

engine: Engine
SessionLocal: sessionmaker


# Dependency
# https://fastapi.tiangolo.com/tutorial/sql-databases/
def database() -> Generator[Session, None, None]:
    db: Session = SessionLocal()
    try:
        with db.begin():
            yield db
    finally:
        db.close()


def init(url: str, test: bool = True):
    global engine
    global SessionLocal
    engine = create_engine(
        url,
        pool_size=2,
        pool_pre_ping=True,
        pool_reset_on_return='rollback',
        pool_recycle=50,
        max_overflow=18
    )
    SessionLocal = sessionmaker(autocommit=False, bind=engine)
    if test:
        def __database() -> Session:
            db: Session = SessionLocal()
            try:
                db.begin()
                yield db
                db.rollback()
            finally:
                db.close()

        database.__code__ = __database.__code__
