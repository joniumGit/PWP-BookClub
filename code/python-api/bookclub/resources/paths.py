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
