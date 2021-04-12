from typing import Optional

from pydantic import BaseModel, Field


class User(BaseModel):
    username: str
    description: Optional[str]

    class Config:
        orm_mode = True


class BookStatsMixIn(BaseModel):
    rating: float = Field(le=10, ge=1)
    readers: int
    completed: int
    pending: int
    liked: int
    disliked: int


class Book(BaseModel):
    handle: str
    full_name: str
    description: Optional[str]
    pages: Optional[int]

    class Config:
        orm_mode = True


class UserBookInternalBase(BaseModel):
    reading_status: Optional[str]
    reviewed: Optional[bool]
    ignored: Optional[bool]
    liked: Optional[bool]
    page: Optional[int]

    class Config:
        orm_mode = True


class UserBook(UserBookInternalBase, Book):
    user: str


class StatBook(BookStatsMixIn, Book):
    pass


class StatUserBook(BookStatsMixIn, UserBook):
    pass


class Club(BaseModel):
    handle: str
    description: Optional[str]
    owner: Optional[str]

    class Config:
        orm_mode = True


class Review(BaseModel):
    user: str
    book: str
    stars: int = Field(le=5, ge=1)
    title: Optional[str]
    content: Optional[str]

    class Config:
        orm_mode = True


class Comment(BaseModel):
    uuid: int
    user: str
    content: str

    class Config:
        orm_mode = True
