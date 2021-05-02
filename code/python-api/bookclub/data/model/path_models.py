from .data_models import *
from ...mason import *


class Users(MasonBase):
    items: list[User]


__all__ = [
    'Users'
]
