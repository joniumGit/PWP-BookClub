import pytest
from fastapi.testclient import TestClient
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from bookclub import api
from bookclub.data import database

client = TestClient(api)


@pytest.fixture(scope="function")
def db() -> Session:
    for db in database():
        db.begin_nested()
        try:
            yield db
        finally:
            db.rollback()


def hello_test():
    from bookclub.resources.paths import HelloModel
    model: HelloModel = HelloModel(**client.get("/").json())
    assert model.name == "none" and model.message == "none"


def test_db(db: Session):
    from bookclub.data.model.db_models import User
    u1 = User(username="test")
    u2 = User(username="test")
    db.add(u1)
    db.flush()
    with pytest.raises(IntegrityError):
        with db.begin_nested():
            db.add(u2)
            db.flush()
