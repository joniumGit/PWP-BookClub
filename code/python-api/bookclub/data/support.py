from typing import Generator

from fastapi import Depends
from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import sessionmaker, Session

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


__all__ = ['init', 'database', 'Session', 'Generator', 'Depends', 'IntegrityError']
