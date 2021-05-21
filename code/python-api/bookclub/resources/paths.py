from functools import partial
from typing import Optional, TypeVar, Callable
from urllib.parse import quote

from fastapi import APIRouter, Response, Request
from fastapi.exceptions import HTTPException
from pydantic import BaseModel

from ..data import *
from ..mason import MasonBase, Control, Namespace

entry = APIRouter()
entry.get = partial(entry.get, response_model_exclude_defaults=True, response_model_exclude_none=True)

"""
Replace this with a 'better' version that will quote absolutely everything.

ASGI spec can't handle / characters and maybe some other ones, so we have to take them out.
Similarly all incoming new models have to be sanitized.
"""
quote = partial(quote, safe="")


def path(request: Request, func: str, **kwargs) -> str:
    """
    Resolve path from request
    Until router can resolve full paths, this will stay here

    :param request:     Request
    :param func:        Path function to resolve from
    :param kwargs:      Path arguments
    :return:            Path string
    """
    return request.url_for(func, **kwargs)


T = TypeVar('T', bound=MasonBase)


def append_(request: Request, control: str, path_function: str, out: T, **kwargs) -> T:
    """
    Append a Control to a Model

    :param request:             Request
    :param control:             Control name to append
    :param path_function:       Name of path function to resolve Control href from
    :param out:                 Model to append to
    :param kwargs:              Pass-through to Control
    :return:                    Model
    """
    if out.controls is None:
        out.controls = dict()
    try:
        out.controls.update({
            control: Control(
                href=path(request, path_function, **{
                    path_function.split("_")[1]: quote(out.handle if hasattr(out, 'handle') else out.username)
                }),
                **kwargs
            )
        })
    except Exception as e:
        from logging import exception
        exception(f"{path_function}, "
                  + path_function.split("_")[1]
                  + ", "
                  + (out.handle if hasattr(out, 'handle') else out.username),
                  exc_info=e)
    return out


def append_self_link(request: Request, path_function: str, out: T) -> T:
    """
    Appends self link to a model

    :param request: Request
    :param out:     Model to append to
    :return:        Model
    """
    return append_(request, "self", path_function, out, method="GET")


def append_delete_link(request: Request, path_function: str, out: T) -> T:
    """
    Appends delete link to a model

    :param request: Request
    :param out:     Model to append to
    :return:        Model
    """
    return append_(request, "delete", path_function, out, method="DELETE")


def append_edit_link(request: Request, path_function: str, out: T) -> T:
    """
    Appends edit link to a model

    :param request: Request
    :param out:     Model to append to
    :return:        Model
    """
    return append_(request, "edit", path_function, out, method="PUT")


def append_home_link(request: Request, out: T) -> T:
    """
    Appends home link to a model

    :param request: Request
    :param out:     Model to append to
    :return:        Model
    """
    if out.controls is None:
        out.controls = dict()
    out.controls.update({
        "bc:home": Control(
            href=path(request, "entrypoint")
        )
    })
    return out


def append_namespace(_: Request, out: T) -> T:
    """
    Appends namespace to object

    :param _:   Request, not used can be anything
    :param out: Model going out
    :return:    Model going out
    """
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
    """
    Common links for all singular resources

    :param out:         Model going out
    :param resource:    SINGULAR NOUN of resource in question
    :param request:     Request object
    :return:            Model object
    """
    append_self_link(request, "get_" + resource + "_resource", out)
    append_edit_link(request, "edit_" + resource + "_resource", out)
    append_delete_link(request, "delete_" + resource + "_resource", out)
    append_home_link(request, out)
    return out


def append_collection_resource_controls(out: T, resource: str, request: Request) -> T:
    """
    Common links for all collection resources

    :param out:         Collection model going out
    :param resource:    SINGULAR NOUN of resource in question
    :param request:     Request object
    :return:            Model object
    """
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


class Entrypoint(MasonBase):
    """
    Test model
    """
    message: str
    name: str


class HelloQuery(BaseModel):
    """
    Test model, how to use query
    """
    message: Optional[str]
    name: Optional[str]


"""
####### ##########  #######  ######## ##########
##          ##     ##     ## ###   ##     ##    
#######     ##     ######### #######      ##    
     ##     ##     ##     ## ##    ##     ##    
#######     ##     ##     ## ##     ##    ##    
"""


@entry.get("/", response_model=Entrypoint)
async def entrypoint(request: Request, query: HelloQuery = Depends()):
    hm = Entrypoint(
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
"""
Functions for adding stuff, all pretty much the same thing
"""


def check_string(s: str):
    """
    Since we can't accept bad identifiers
    """
    s = s.strip()
    out = quote(s, safe="")
    # Only space is allowed
    if s.replace(" ", "%20") != out:
        raise HTTPException(409, f"Unacceptable value for identifier {s}")
    # Reserve this
    elif out == "deleted":
        raise HTTPException(409, f"Reserved identifier value {s}")
    return out


@entry.post("/books", status_code=204)
async def add_book_resource(
        book: NewBook,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    check_string(book.handle)
    create_book(book, db)
    response.headers["Location"] = request.url_for("get_book_resource", book=quote(book.handle))
    response.status_code = 204
    return response


@entry.post("/clubs", status_code=204)
async def add_club_resource(
        club: NewClub,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    check_string(club.handle)
    create_club(club, db)
    response.headers["Location"] = request.url_for("get_club_resource", club=quote(club.handle))
    response.status_code = 204
    return response


@entry.post("/users", status_code=204, responses={409: {}, 415: {}})
async def add_user_resource(
        user: NewUser,
        request: Request,
        response: Response,
        db: Session = Depends(database)
):
    check_string(user.username)
    create_user(user, db)
    response.headers["Location"] = request.url_for("get_user_resource", user=quote(user.username))
    response.status_code = 204
    return response


"""
###### ######   ## ########## 
##     ##   ##  ##     ##     
#####  ##   ##  ##     ##     
##     ##   ##  ##     ##     
###### ######   ##     ##     
"""

E = TypeVar('E', bound=BaseModel)
entry.put = partial(entry.put, status_code=200, responses={304: {}, 201: {}, 409: {}})


def _edit_(
        get: Callable,
        update: Callable,
        create: Callable,
        delete: Callable,
        response: Response,
        existing: str,
        new_model: E,
        db: Session,
        location: bool = False,
        check_identity: bool = True,
        simple_update: bool = False,
        url: str = None

):
    # Don't change state
    if check_identity:
        identity = (new_model.handle if hasattr(new_model, 'handle') else new_model.username)
        if existing != identity:
            raise HTTPException(409, f"Entity identity doesn't match resource, expected: {existing} got: {identity}")
    exists = False
    deleted = False
    if not simple_update:
        try:
            get(existing, db)
            exists = True
        except HTTPException as e:
            # Ah fuck we have to do some mental gymnastics
            if 'deleted' in e.detail:
                exists = True
                deleted = True
            elif 'exists' in e.detail or 'taken' in e.detail:
                exists = True
    if (exists and not deleted) or simple_update:
        changed = update(existing, new_model, db)
        if changed is not None:
            if location:
                # request.url_for("get_user_resource", user=new_user.username)
                response.headers["Location"] = url
                response.status_code = 204
            else:
                response.status_code = 200
        else:
            response.status_code = 304
    else:
        check_string((new_model.handle if hasattr(new_model, 'handle') else new_model.username))
        delete(existing, db)
        create(new_model, db)
        response.status_code = 201
    return response


@entry.put("/users/{user}")
async def edit_user_resource(
        user: str,
        new_user: NewUser,
        _: Request,  # ignored for now
        response: Response,
        db: Session = Depends(database)
):
    return _edit_(
        get_user,
        update_user,
        create_user,
        partial(delete_user, hard=True),
        response,
        user,
        new_user,
        db
    )


@entry.put("/books/{book}")
async def edit_book_resource(
        book: str,
        new_book: NewBook,
        _: Request,
        response: Response,
        db: Session = Depends(database)
):
    return _edit_(
        get_book,
        update_book,
        create_book,
        partial(delete_book, hard=True),
        response,
        book,
        new_book,
        db
    )


@entry.put("/clubs/{club}")
async def edit_club_resource(
        club: str,
        new_club: NewClub,
        _: Request,
        response: Response,
        db: Session = Depends(database)
):
    return _edit_(
        get_club,
        update_club,
        create_club,
        partial(delete_club, hard=True),
        response,
        club,
        new_club,
        db
    )


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
