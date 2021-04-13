from ..data.model.data_models import *
from ..mason import *


class Users(MasonBase):
    items: list[User]
