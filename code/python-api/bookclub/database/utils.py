from fastapi import HTTPException

from . import Session, IntegrityError
from . import db_models as dbo
from ..resources import models as ppo


class ResourceNotFound(HTTPException):

    def __init__(self, msg: str):
        super(ResourceNotFound, self).__init__(404, msg)


class ResourceAlreadyExists(HTTPException):

    def __init__(self, msg: str):
        super(ResourceAlreadyExists, self).__init__(409, msg)


async def get_user(database: Session, username: str) -> dbo.User:
    user = database.query(dbo.User).filter(dbo.User.username == username).one_or_none()
    if user is None:
        raise ResourceNotFound("User not found")
    else:
        return user


async def create_user(database: Session, user: ppo.User) -> None:
    try:
        database.add(dbo.User(**user.dict()))
        database.flush()
        database.commit()
    except IntegrityError:
        raise ResourceAlreadyExists(f"User with username {user.username} already exists")
