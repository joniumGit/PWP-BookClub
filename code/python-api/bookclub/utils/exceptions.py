"""
Shorthands for some common exceptions
"""
from fastapi import HTTPException


class NotFound(HTTPException):

    def __init__(self, msg: str):
        super(NotFound, self).__init__(404, msg)


class AlreadyExists(HTTPException):

    def __init__(self, msg: str):
        super(AlreadyExists, self).__init__(409, msg)


class Unauthorized(HTTPException):

    def __init__(self, msg: str):
        super(Unauthorized, self).__init__(401, msg)


class Forbidden(HTTPException):

    def __init__(self, msg: str):
        super(Forbidden, self).__init__(403, msg)


class InternalError(HTTPException):

    def __init__(self, msg: str):
        super(InternalError, self).__init__(500, msg)
