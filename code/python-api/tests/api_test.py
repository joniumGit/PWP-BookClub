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


def test_hello(client: FlaskClient):
    r: Response = client.get("/hello")
    assert r.get_json()["hello"] == "world"


def test_custom_hello(client: FlaskClient):
    import random
    import string
    expected = ''.join(random.choices(string.ascii_letters + string.digits, k=100))
    r: Response = client.get("/hello/" + expected)
    assert r.get_json()["hello"] == expected
