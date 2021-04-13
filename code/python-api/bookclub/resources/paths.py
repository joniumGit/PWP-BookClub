from typing import Optional, List

from fastapi import APIRouter, Response, Request
from pydantic import BaseModel

from .models import User
from ..database import *

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
async def add_user(user: User, request: Request, response: Response, db: Session = Depends(database)):
    await crud.create_user(db, user)
    response.headers["Location"] = request.url_for("get_user", user=user.username)


@entry.get("/users", response_model=List[User])
async def get_users(db: Session = Depends(database)):
    return db.query(DBUser).all()


@entry.get("/users/{user}", response_model=User)
async def get_user(user: str, db: Session = Depends(database)):
    return await crud.get_user(db, user)
