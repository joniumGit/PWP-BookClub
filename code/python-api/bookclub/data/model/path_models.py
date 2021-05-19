from typing import List

from .data_models import *
from ...mason import *


class Users(MasonBase):
    items: List[User]


class Books(MasonBase):
    items: List[Book]


class Clubs(MasonBase):
    items: List[Club]


__all__ = [
    'Users',
    'Books',
    'Clubs'
]
