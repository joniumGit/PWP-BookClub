from typing import Optional, List

from fastapi import APIRouter, HTTPException, Response, Request
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
    try:
        db.add(DBUser(username=user.username, description=user.description))
        db.flush()
        db.commit()
    except IntegrityError as e:
        db.rollback()
        raise HTTPException(409, detail="Already exists")
    response.headers["Location"] = request.url_for("get_users")


@entry.get("/users", response_model=List[User])
async def get_users(db: Session = Depends(database)):
    return db.query(DBUser).all()
