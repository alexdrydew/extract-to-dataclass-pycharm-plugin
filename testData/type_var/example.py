from typing import TypeVar
from dataclasses import dataclass

T = TypeVar('T', bound=int)

def <caret>target_function(
    arg1: T,
    arg2: str,
):
    return arg1 + arg2

target_function(
    arg1=1,
    arg2='2'
)
