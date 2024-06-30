from dataclasses import dataclass


@dataclass
class TargetFunctionParams:
    arg2: str


def target_function(
        arg1: int,
        params: TargetFunctionParams):
    return arg1 + params.arg2


target_function(
    arg1=1, params=TargetFunctionParams(arg2='2')
)
