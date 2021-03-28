from flask import Blueprint
from flask_restful import Resource
from .models import *
from flask_pydantic import validate
entry_bp = Blueprint("api", __name__)


@entry_bp.route("/")
@validate()
def hello():
    return User(username="peng")
