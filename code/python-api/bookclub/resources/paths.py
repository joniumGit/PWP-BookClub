from functools import partial
from typing import Optional, TypeVar

from fastapi import APIRouter, Response, Request
from pydantic import BaseModel

from ..data import *
from ..mason import MasonBase, Control, Namespace

entry = APIRouter()
entry.get = partial(entry.get, response_model_exclude_defaults=True, response_model_exclude_none=True)


def path(request: Request, func: str, **kwargs):
    return request.url_for(func, **kwargs)


T = TypeVar('T', bound=MasonBase)


def append_(request: Request, control: str, path_function: str, out: T, **kwargs) -> T:
    if out.controls is None:
        out.controls = dict()
    try:
        out.controls.update({
            control: Control(
                href=path(request, path_function, **{
                    path_function.split("_")[1]: (out.handle if hasattr(out, 'handle') else out.username)
                }),
                **kwargs
            )
        })
    except Exception:
        raise Exception(
            f"{path_function}, "
            + path_function.split("_")[1]
            + ", "
            + (out.handle if hasattr(out, 'handle') else out.username)
        )
    return out


def append_self_link(request: Request, path_function: str, out: T) -> T:
    return append_(request, "self", path_function, out, method="GET")


def append_delete_link(request: Request, path_function: str, out: T) -> T:
    return append_(request, "delete", path_function, out, method="DELETE")


def append_edit_link(request: Request, path_function: str, out: T) -> T:
    return append_(request, "edit", path_function, out, method="PUT")


def append_home_link(request: Request, out: T) -> T:
    if out.controls is None:
        out.controls = dict()
    out.controls.update({
        "bc:home": Control(
            href=path(request, "entrypoint")
        )
    })
    return out


def append_namespace(_: Request, out: T) -> T:
    if out.namespaces is None:
        out.namespaces = dict()
    out.namespaces.update(
        {
            "bc": Namespace(
                name="https://bookclub4.docs.apiary.io/#"
            )
        }
    )
    return out


def append_single_resource_controls(out: T, resource: str, request: Request) -> T:
    append_self_link(request, "get_" + resource + "_resource", out)
    append_edit_link(request, "edit_" + resource + "_resource", out)
    append_delete_link(request, "delete_" + resource + "_resource", out)
    append_home_link(request, out)
    return out


def append_collection_resource_controls(out: T, resource: str, request: Request) -> T:
    for item in out.items:
        append_single_resource_controls(item, resource, request)
    append_home_link(request, out)
    append_namespace(request, out)
    out.controls.update({
        "self": Control(
            href=path(request, "get_" + resource + "s_resource"),
            method="GET"
        ),

        "add": Control(
            href=path(request, "add_" + resource + "_resource"),
            method="POST"
        )
    })
    return out


class HelloModel(MasonBase):
    message: str
    name: str


class HelloQuery(BaseModel):
    message: Optional[str]
    name: Optional[str]


"""
####### ##########  #######  ######## ##########
##          ##     ##     ## ###   ##     ##    
#######     ##     ######### #######      ##    
     ##     ##     ##     ## ##    ##     ##    
#######     ##     ##     ## ##     ##    ##    
"""


@entry.get("/", response_model=HelloModel)
async def entrypoint(request: Request, query: HelloQuery = Depends()):
    hm = HelloModel(
        name=query.name or "none",
        message=query.message or "none"
    )
    hm.controls = {
        "bc:books-all": Control(
            title="Books Collection",
            href=path(request, "get_books_resource")
        ),
        "bc:users-all": Control(
            title="Users Collection",
            href=path(request, "get_users_resource")
        ),
        "bc:clubs-all": Control(
            title="Clubs Collection",
            href=path(request, "get_clubs_resource")
        ),
        "self": Control(
            href=path(request, "entrypoint")
        )
    }
    return append_namespace(request, append_home_link(request, hm))


"""
####### ###### ########## #######
##      ##         ##     ##     
##  ### #####      ##     #######
##   ## ##         ##          ##
####### ######     ##     #######
"""


@entry.get("/users", response_model=Users)
async def get_users_resource(request: Request, db: Session = Depends(database)):
    return append_collection_resource_controls(get_users(db), "user", request)


@entry.get("/books", response_model=Books)
async def get_books_resource(request: Request, db: Session = Depends(database)):
    return append_collection_resource_controls(get_books(db), "book", request)


@entry.get("/clubs", response_model=Clubs)
async def get_clubs_resource(request: Request, db: Session = Depends(database)):
    return append_collection_resource_controls(get_clubs(db), "club", request)


"""
####### ###### ##########
##      ##         ##    
##  ### #####      ##    
##   ## ##         ##    
####### ######     ##    
"""


@entry.get("/users/{user}", response_model=User)
async def get_user_resource(request: Request, user: str, db: Session = Depends(database)):
    return append_single_resource_controls(get_user(user, db), "user", request)


@entry.get("/books/{book}", response_model=Book)
async def get_book_resource(request: Request, book: str, db: Session = Depends(database)):
    return append_single_resource_controls(get_book(book, db), "book", request)


@entry.get("/clubs/{club}", response_model=Club)
async def get_club_resource(request: Request, club: str, db: Session = Depends(database)):
    return append_single_resource_controls(get_club(club, db), "club", request)


"""
 #######  ######  ###### 
##     ## ##   ## ##   ##
######### ##   ## ##   ##
##     ## ##   ## ##   ##
##     ## ######  ###### 
"""


@entry.post("/books", status_code=204)
async def add_book_resource(
        book: NewBook,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    create_book(book, db)
    response.headers["Location"] = request.url_for("get_book_resource", book=book.handle)
    response.status_code = 204
    return response


@entry.post("/clubs", status_code=204)
async def add_club_resource(
        club: NewClub,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    create_club(club, db)
    response.headers["Location"] = request.url_for("get_club_resource", club=club.handle)
    response.status_code = 204
    return response


@entry.post("/users", status_code=204)
async def add_user_resource(
        user: NewUser,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    create_user(user, db)
    response.headers["Location"] = request.url_for("get_user_resource", user=user.username)
    response.status_code = 204
    return response


"""
###### ######   ## ########## 
##     ##   ##  ##     ##     
#####  ##   ##  ##     ##     
##     ##   ##  ##     ##     
###### ######   ##     ##     
"""


@entry.put("/users/{user}", status_code=204, responses={304: {}})
async def edit_user_resource(
        user: str,
        new_user: NewUser,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    uname = update_user(user, new_user, db)
    if uname is not None:
        response.headers["Location"] = request.url_for("get_user_resource", user=new_user.username)
        response.status_code = 204
        return response
    response.status_code = 304
    return response


@entry.put("/books/{book}", status_code=204)
async def edit_book_resource(
        book: str,
        new_book: NewBook,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    handle = update_book(book, new_book, db)
    if handle is not None:
        response.headers["Location"] = request.url_for("get_book_resource", book=new_book.handle)
        response.status_code = 204
    response.status_code = 304
    return response


@entry.put("/clubs/{club}", status_code=204)
async def edit_club_resource(
        club: str,
        new_club: NewClub,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    handle = update_club(club, new_club, db)
    if handle is not None:
        response.headers["Location"] = request.url_for("get_club_resource", club=new_club.handle)
        response.status_code = 204
    response.status_code = 304
    return response


"""
######  ###### ##     ###### ########## ######
##   ## ##     ##     ##         ##     ##    
##   ## #####  ##     #####      ##     ##### 
##   ## ##     ##     ##         ##     ##    
######  ###### ###### ######     ##     ######
"""


@entry.delete("/users/{user}", status_code=204)
async def delete_user_resource(user: str, response: Response, db: Session = Depends(database)):
    delete_user(user, db)
    response.status_code = 204
    return response


@entry.delete("/books/{book}", status_code=204)
async def delete_book_resource(book: str, response: Response, db: Session = Depends(database)):
    delete_book(book, db)
    response.status_code = 204
    return response


@entry.delete("/clubs/{club}", status_code=204)
async def delete_club_resource(club: str, response: Response, db: Session = Depends(database)):
    delete_club(club, db)
    response.status_code = 204
    return response
