from typing import overload

@overload
def target_function(arg1: int, arg2: str) -> str:
    ...

@overload
def target_function(arg1: float, arg2: str) -> str:
    ...

def <caret>target_function(arg1, arg2: str):
    return str(arg1) + arg2

target_function(1, '2')
target_function(1.0, '2')