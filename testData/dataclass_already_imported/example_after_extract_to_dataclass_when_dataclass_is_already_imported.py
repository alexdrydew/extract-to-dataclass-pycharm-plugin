from dataclasses import dataclass


@dataclass
class TargetFunctionMyParams:
    arg1: int
    arg2: str


def target_function(
        my_params: TargetFunctionMyParams):
    return my_params.arg1 + my_params.arg2


target_function(
    my_params=TargetFunctionMyParams(arg1=1, arg2='2'))
