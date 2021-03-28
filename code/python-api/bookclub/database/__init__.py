from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import sessionmaker

from .db_models import Book, Club, User, Comment, Review, UserBook, t_books_statistics

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
