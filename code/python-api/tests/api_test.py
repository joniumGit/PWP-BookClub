import pytest
from fastapi.testclient import TestClient
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from bookclub import api, database

client = TestClient(api)


@pytest.fixture()
def db() -> Session:
    yield from database()


def hello_test():
    from bookclub.resources.entry import HelloModel
    model: HelloModel = HelloModel(**client.get("/").json())
    assert model.name == "none" and model.message == "none"


def test_db(db: Session):
    from bookclub.database.db_models import User
    u1 = User(username="test")
    u2 = User(username="test")
    failed = False
    try:
        db.add(u1)
        db.flush()
        db.add(u2)
        db.flush()
    except IntegrityError:
        failed = True
    finally:
        db.rollback()
    assert failed
