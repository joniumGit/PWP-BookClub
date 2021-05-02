from typing import List

from .data_models import *
from ...mason import *


class Users(MasonBase):
    items: List[User]


__all__ = [
    'Users'
]
