from dataclasses import dataclass


@dataclass
class TargetFunctionMyParams:
    arg1: int


def target_function(
        arg2: str,
my_params: TargetFunctionMyParams):
    return my_params.arg1 + arg2


target_function(
    arg2='2', my_params=TargetFunctionMyParams(arg1=1)
)
