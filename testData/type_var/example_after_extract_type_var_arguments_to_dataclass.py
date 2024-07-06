from typing import Generic
from typing import TypeVar
from dataclasses import dataclass

T = TypeVar('T', bound=int)


@dataclass
class TargetFunctionArgs(Generic[T]):
    arg1: T
    arg2: str


def target_function(
        my_params: TargetFunctionArgs):
    return my_params.arg1 + my_params.arg2

target_function(
    my_params=TargetFunctionArgs(arg1=1, arg2='2'))
