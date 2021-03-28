import pytest
from flask import Response
from flask.testing import FlaskClient

from bookclub import create_app


@pytest.fixture
def client() -> FlaskClient:
    app = create_app()
    with app.test_client() as client:
        yield client


def test_root(client: FlaskClient):
    r: Response = client.get("/")
    assert r.get_data(as_text=True) == "Hello"
