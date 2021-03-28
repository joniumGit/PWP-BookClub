# noinspection PyUnresolvedReferences
from fastapi import Depends
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
# noinspection PyUnresolvedReferences
from sqlalchemy.exc import IntegrityError
# noinspection PyUnresolvedReferences
from sqlalchemy.orm import sessionmaker, Session

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
# noinspection PyUnresolvedReferences
from . import utils as crud

engine: Engine
SessionLocal: sessionmaker


def init(url: str):
    global engine
    global SessionLocal
    engine = create_engine(url)
    SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


# https://fastapi.tiangolo.com/tutorial/sql-databases/
# Dependency
def database():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
