import os

from fastapi import FastAPI, HTTPException, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import ValidationError
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


async def masonware(r: Request, call_next):
    try:
        return await call_next(r)
    except Exception as e:
        if isinstance(e, HTTPException) or isinstance(e, LowHTTPException) or isinstance(e, RequestValidationError):
            raise e
        else:
            from logging import exception
            exception("Unhandled Exception", exc_info=e)
            return JSONResponse(
                media_type=MASON,
                status_code=500,
                content={
                    "@error": {
                        "@message": "Internal Server Error",
                        "@httpStatusCode": 500,
                        "@controls": {
                            "bc:home": {
                                "href": str(r.base_url)
                            }
                        }
                    }
                }
            )


api.middleware('http')(masonware)


@api.exception_handler(HTTPException)
def exception_handler(r: Request, exc: HTTPException):
    return JSONResponse(
        media_type=MASON,
        status_code=exc.status_code,
        content={
            "@error": {
                "@message": exc.detail or "An exception occurred",
                "@httpStatusCode": exc.status_code,
                "@controls": {
                    "bc:home": {
                        "href": str(r.base_url)
                    }
                }
            }
        },
    )


@api.exception_handler(RequestValidationError)
def exception_handler(r: Request, exc: RequestValidationError):
    return JSONResponse(
        media_type=MASON,
        status_code=422,
        content={
            "@error": {
                "@message": "Failed to process Entity",
                "@messages": [
                    (e.json() if isinstance(e, ValidationError) else str(e)) for e in exc.raw_errors
                ],
                "@httpStatusCode": 422,
                "@controls": {
                    "bc:home": {
                        "href": str(r.base_url)
                    }
                }
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
                    "bc:home": {
                        "href": str(r.base_url)
                    }
                }
            }
        },
    )


__all__ = 'api'
