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


class UserBookIncomingModel(BaseModel):
    user: str
    handle: str
    reading_status: Optional[str]
    reviewed: Optional[bool]
    ignored: Optional[bool]
    liked: Optional[bool]
    page: Optional[int]


class UserBook(UserBookInternalBase, Book):
    user: str


class StatBook(BookStatsMixIn, Book):
    pass


class StatUserBook(BookStatsMixIn, UserBook):
    pass


class ClubInternalBase(BaseModel):
    handle: str
    description: Optional[str]

    class Config:
        orm_mode = True


class Club(ClubInternalBase):
    owner: Optional[str]


class ReviewInternalBase(BaseModel):
    stars: int = Field(le=5, ge=1)
    title: Optional[str]
    content: Optional[str]

    class Config:
        orm_mode = True


class Review(ReviewInternalBase):
    user: str
    book: str


class NewComment(BaseModel):
    user: str
    content: str


class CommentInternalBase(BaseModel):
    uuid: int
    content: str

    class Config:
        orm_mode = True


class Comment(CommentInternalBase):
    user: str
