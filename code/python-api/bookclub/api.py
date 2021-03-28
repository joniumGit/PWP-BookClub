from fastapi import FastAPI

from .database import *
from .resources.entry import entry

DATABASE_URL = "mysql+pymysql://root:test@localhost:6969/book_club"
init(DATABASE_URL)

api = FastAPI()

api.include_router(entry)
