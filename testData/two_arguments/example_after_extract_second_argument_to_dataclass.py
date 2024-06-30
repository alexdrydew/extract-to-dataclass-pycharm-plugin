from dataclasses import dataclass


@dataclass
class TargetFunctionMyParams:
    arg2: str


def target_function(
        arg1: int,
        my_params: TargetFunctionMyParams):
    return arg1 + my_params.arg2


target_function(
    arg1=1, my_params=TargetFunctionMyParams(arg2='2')
)
