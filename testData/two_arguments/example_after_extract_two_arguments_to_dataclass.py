from dataclasses import dataclass


@dataclass
class TargetFunctionParams:
    arg1: int
    arg2: str


def target_function(
        params: TargetFunctionParams):
    return params.arg1 + params.arg2


target_function(
    params=TargetFunctionParams(arg1=1, arg2='2'))
