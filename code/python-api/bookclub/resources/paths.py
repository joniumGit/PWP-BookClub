from typing import Optional

from fastapi import APIRouter, Response, Request, Depends
from pydantic import BaseModel

from ..data import da, Session, database
from ..data.model import dbm, ext

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
async def add_user(
        user: ext.User,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    da.create_user(user, db)
    response.headers["Location"] = request.url_for("get_user", user=user.username)


@entry.get("/users")
async def get_users(db: Session = Depends(database)):
    return {'items': [ext.User.from_orm(x) for x in db.query(dbm.User).all()]}


@entry.get("/users/{user}", response_model=ext.User)
async def get_user(user: str, db: Session = Depends(database)):
    return da.get_user(user, db)
