import json

from bookclub.mason import *

sample = """
{
  "name": "test",
  "@controls": {
    "self": {
      "href": "https://self.self"
    },
    "up": {
      "href": "https://up/up/and/away",
      "title": "upwards"
    },
    "ns:down": {
      "encoding": "json",
      "href": "https://downwards"
    }
  },
  "@namespaces" : {
    "ns" : {
        "name" : "https://self"
    }
  }
}
"""


class Name(mason.MasonBase):
    name: str


def test_mason():
    """
    Just a simple reading test, doesn't really test anything except that inheritance works
    """
    d = json.loads(sample)
    name: Name = Name.parse_obj(d)
    assert name.name == 'test'
    assert name.controls is not None
    assert name.controls['self'].href == 'https://self.self'
    assert 'up' in name.controls
    assert 'ns' in name.namespaces
