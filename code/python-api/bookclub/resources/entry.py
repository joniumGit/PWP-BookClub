from flask import Blueprint
from flask_restful import Resource

entry_bp = Blueprint("api", __name__)


@entry_bp.route("/")
def hello():
    return "Hello"


class Entry(Resource):

    def get(self):
        return {"hello": "world"}


class CustomEntry(Resource):

    def get(self, message: str):
        return {"hello": message}
