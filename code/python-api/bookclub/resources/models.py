from pydantic import BaseModel
from typing import Optional


class User(BaseModel):
    username: str
    description: Optional[str]


class Book(BaseModel):
    handle: str
    name: str
    description: str
    pages: Optional[int]


class UserBook(BaseModel):
    user: str
    status: Optional[str]
    reviewed: Optional[bool]
    ignored: Optional[bool]
    liked: Optional[bool]
    page: Optional[int]


class Club(BaseModel):
    handle: str
    description: Optional[str]
    owner: Optional[str]


class Review(BaseModel):
    user: str
    book: str
    stars: int
    title: Optional[str]
    content: Optional[str]


class Comment(BaseModel):
    id: str
    user: str
    content: str
