from enum import Enum
from typing import Optional

from pydantic import Field, BaseModel
from ...mason import MasonBase


class StatusEnum(str, Enum):
    pending = 'pending'
    completed = 'completed'
    reading = 'reading'


class NewUser(BaseModel):
    username: str = Field(min_length=1, max_length=60)
    description: Optional[str] = Field(max_length=250)


class User(NewUser, MasonBase):
    class Config:
        orm_mode = True


class BookStatsMixIn(MasonBase):
    rating: float = Field(le=10, ge=1)
    readers: int = Field(ge=0)
    completed: int = Field(ge=0)
    pending: int = Field(ge=0)
    liked: int = Field(ge=0)
    disliked: int = Field(ge=0)


class NewBook(BaseModel):
    handle: str = Field(min_length=1, max_length=60)
    full_name: str = Field(min_length=1, max_length=250)
    description: Optional[str] = Field(max_length=65000)
    pages: Optional[int] = Field(le=2000000000)


class Book(NewBook, MasonBase):
    class Config:
        orm_mode = True


class UserBookInternalBase(MasonBase):
    reading_status: Optional[StatusEnum]
    reviewed: Optional[bool]
    ignored: Optional[bool]
    liked: Optional[bool]
    page: Optional[int] = Field(ge=0)

    class Config:
        orm_mode = True


class NewUserBook(BaseModel):
    user: str = Field(min_length=1, max_length=60)
    handle: str = Field(min_length=1, max_length=60)
    reading_status: Optional[StatusEnum]
    reviewed: Optional[bool]
    ignored: Optional[bool]
    liked: Optional[bool]
    current_page: Optional[int]


class UserBook(UserBookInternalBase, Book):
    user: str = Field(min_length=1, max_length=60)


class StatBook(BookStatsMixIn, Book):
    pass


class StatUserBook(BookStatsMixIn, UserBook):
    pass


class NewClub(BaseModel):
    owner: Optional[str] = Field(min_length=1, max_length=60)
    handle: str = Field(min_length=1, max_length=60)
    description: Optional[str] = Field(max_length=2040)


class ClubInternalBase(MasonBase):
    handle: str = Field(min_length=1, max_length=60)
    description: Optional[str] = Field(max_length=2040)

    class Config:
        orm_mode = True


class Club(ClubInternalBase):
    owner: Optional[str] = Field(min_length=1, max_length=60)


class NewReview(BaseModel):
    user: str = Field(min_length=1, max_length=60)
    book: str = Field(min_length=1, max_length=60)
    stars: int = Field(le=5, ge=1)
    title: str = Field(min_length=1, max_length=120)
    content: Optional[str] = Field(max_length=65000)


class ReviewInternalBase(MasonBase):
    stars: int = Field(le=5, ge=1)
    title: str = Field(min_length=1, max_length=120)
    content: Optional[str] = Field(max_length=65000)

    class Config:
        orm_mode = True


class Review(ReviewInternalBase):
    user: str = Field(min_length=1, max_length=60)
    book: str = Field(min_length=1, max_length=60)


class NewComment(BaseModel):
    user: str = Field(min_length=1, max_length=60)
    content: str = Field(min_length=1, max_length=65000)


class CommentInternalBase(BaseModel):
    uuid: int
    content: str = Field(min_length=1, max_length=65000)

    class Config:
        orm_mode = True


class Comment(CommentInternalBase):
    user: str = Field(min_length=1, max_length=60)


class CommentMason(Comment, MasonBase):
    pass
