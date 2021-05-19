from typing import Optional

from fastapi import APIRouter, Response, Request
from pydantic import BaseModel

from ..data import *

entry = APIRouter()


class HelloModel(BaseModel):
    message: str
    name: str


class HelloQuery(BaseModel):
    message: Optional[str]
    name: Optional[str]


@entry.get("/")
async def hello_endpoint(query: HelloQuery = Depends()):
    return HelloModel(
        name=query.name or "none",
        message=query.message or "none"
    )


@entry.put("/users", status_code=204)
async def add_user_resource(
        user: User,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    create_user(user, db)
    response.headers["Location"] = request.url_for("get_user_resource", user=user.username)
    response.status_code = 204
    return response


@entry.get("/users", response_model=Users, response_model_exclude_none=True, response_model_exclude_defaults=True)
async def get_users_resource(db: Session = Depends(database)):
    return get_users(db)


@entry.get("/users/{user}", response_model=User, response_model_exclude_none=True, response_model_exclude_defaults=True)
async def get_user_resource(user: str, db: Session = Depends(database)):
    u = get_user(user, db)
    return u


@entry.delete("/users", status_code=204)
async def delete_user_resource(
        user: User,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    delete_user(user, db)
    response.headers["Location"] = request.url_for("get_user_resource", user=user.username)
    response.status_code = 204  # user still exists in users
    return response


# Books
@entry.put("/books", status_code=204)
async def add_book_resource(
        book: Book,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    create_book(book, db)
    response.headers["Location"] = request.url_for("get_book_resource", book=book.handle)
    response.status_code = 204
    return response


@entry.get("/books", response_model=Books, response_model_exclude_none=True, response_model_exclude_defaults=True)
async def get_books_resource(db: Session = Depends(database)):
    return get_books(db)


@entry.get("/books/{book}", response_model=Book, response_model_exclude_none=True, response_model_exclude_defaults=True)
async def get_book_resource(book: str, db: Session = Depends(database)):
    u = get_book(book, db)
    return u


@entry.delete("/books", status_code=204)
async def delete_book_resource(
        book: Book,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    delete_book(book, db)
    response.headers["Location"] = request.url_for("get_book_resource", book=book.handle)
    response.status_code = 204
    return response


# Clubs
@entry.put("/clubs", status_code=204)
async def add_club_resource(
        club: Club,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    create_club(club, db)
    response.headers["Location"] = request.url_for("get_club_resource", club=club.owner)
    response.status_code = 204
    return response


@entry.get("/clubs", response_model=Clubs, response_model_exclude_none=True, response_model_exclude_defaults=True)
async def get_clubs_resource(db: Session = Depends(database)):
    return get_clubs(db)


@entry.get("/clubs/{club}", response_model=Club, response_model_exclude_none=True, response_model_exclude_defaults=True)
async def get_club_resource(handle: str, db: Session = Depends(database)):
    u = get_club(handle, db)
    return u


@entry.delete("/clubs", status_code=204)
async def delete_club_resource(
        club: Club,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    delete_club(club, db)
    response.headers["Location"] = request.url_for("get_club_resource", club=club.handle)
    response.status_code = 204
    return response
