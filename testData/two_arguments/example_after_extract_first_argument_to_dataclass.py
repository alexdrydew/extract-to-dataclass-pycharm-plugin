from dataclasses import dataclass


@dataclass
class TargetFunctionParams:
    arg1: int


def target_function(
        arg2: str,
params: TargetFunctionParams):
    return params.arg1 + arg2


target_function(
    arg2='2', params=TargetFunctionParams(arg1=1)
)
