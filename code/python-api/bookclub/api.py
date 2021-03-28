from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse

from .database import *
from .resources.entry import entry

MASON = 'application/vnd.mason+json'

DATABASE_URL = "mysql+pymysql://root:test@localhost:6969/book_club"
init(DATABASE_URL)

api = FastAPI()

api.include_router(entry)


@api.exception_handler(HTTPException)
def exception_handler(_: Request, exc: HTTPException):
    return JSONResponse(
        media_type=MASON,
        status_code=exc.status_code,
        content={
            "@error": {
                "@message": exc.detail or "An exception occurred",
                "@httpStatusCode": exc.status_code
            }
        },
    )
