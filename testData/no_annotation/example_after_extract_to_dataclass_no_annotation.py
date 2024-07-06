from typing import Any
from dataclasses import dataclass


@dataclass
class TargetFunctionArgs:
    arg1: int
    arg2: Any


def <caret>target_function(my_params: TargetFunctionArgs):
    return my_params.arg1 + my_params.arg2

target_function(my_params=TargetFunctionArgs(1, '2'))
