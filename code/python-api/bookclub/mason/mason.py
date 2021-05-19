"""
Provides Pydantic models for representing the JSON elements in the Mason standard

https://github.com/JornWildt/Mason/blob/master/Documentation/Mason-draft-2.md
"""
from datetime import datetime
from enum import Enum
from typing import Optional, Dict, List

from pydantic import BaseModel, Field, Json, AnyUrl

MASON: str = 'vnd.mason+json'


class EncodingEnum(str, Enum):
    """
    Contains the allowed values for Mason Control encoding field
    """
    none = 'none'
    json = 'json'
    json_files = 'json+files'
    raw = 'raw'


class FileDescriptor(BaseModel):
    """
    Model for Mason Control file descriptors
    """
    name: str
    title: Optional[str]
    description: Optional[str]
    accept: Optional[List[str]]


class BaseControl(BaseModel):
    """
    Base model for a Mason control
    """
    href: AnyUrl
    isHrefTemplate: Optional[bool]
    title: Optional[str]
    description: Optional[str]
    method: Optional[str]
    encoding: Optional[EncodingEnum] = EncodingEnum.none
    schema_: Optional[Json]
    schemaUrl: Optional[AnyUrl]
    template: Optional[Json]
    accept: Optional[List[str]]
    output: Optional[List[str]]
    files: Optional[List[FileDescriptor]]


class Control(BaseControl):
    """
    Mason control

    Adds the capability to have alternative controls
    """
    alt: Optional[List[BaseControl]]


class Meta(BaseModel):
    """
    Mason Meta
    """
    title: Optional[str] = Field(alias='@title')
    description: Optional[str] = Field(alias='@description')
    controls: Optional[Dict[str, Control]] = Field(alias='@controls')


class Namespace(BaseModel):
    """
    Wrapper for a namespace object

    Might have more properties in the future so this is represented as a class.
    Another possibility would be to simple serialize this as a Dict[str,str]
    """
    name: str


class Error(BaseModel):
    """
    Mason error element
    """
    message: str = Field(alias='@error')
    id: Optional[str] = Field(alias='@id')
    code: Optional[str] = Field(alias='@code')
    messages: Optional[List[str]] = Field(alias='@messages')
    details: Optional[str] = Field(alias='@details')
    httpStatusCode: Optional[int] = Field(alias='@httpStatusCode')
    controls: Optional[Dict[str, Control]] = Field(alias='@controls')
    time: Optional[datetime] = Field(alias='@time')


class MasonBase(BaseModel):
    """
    Base class for Mason

    Can be used as a mix-in class to add Mason capability
    """
    meta: Optional[Meta] = Field(alias='@meta')
    namespaces: Optional[Dict[str, Namespace]] = Field(alias='@namespaces')
    controls: Optional[Dict[str, Control]] = Field(alias='@controls')
    error: Optional[Error] = Field(alias='@error')
