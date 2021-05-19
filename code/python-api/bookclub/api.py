import os

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as LowHTTPException

from .data import *
from .mason import MASON
from .resources.paths import entry

DATABASE_URL = "mysql+pymysql://root:test@localhost:6969/book_club"

if os.getenv("book_club_db_url"):
    DATABASE_URL = os.getenv("book_club_db_url")
    init(DATABASE_URL, False)
elif os.getenv("book_club_persist"):
    init(DATABASE_URL, False)
else:
    init(DATABASE_URL, True)

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


@api.exception_handler(LowHTTPException)
def low_http_exception(r: Request, exc: LowHTTPException):
    return JSONResponse(
        media_type=MASON,
        status_code=exc.status_code,
        content={
            "@error": {
                "@message": exc.detail or "An exception occurred",
                "@httpStatusCode": exc.status_code,
                "@controls": {
                    "index": {
                        "href": str(r.base_url)
                    }
                }
            }
        },
    )


__all__ = 'api'
