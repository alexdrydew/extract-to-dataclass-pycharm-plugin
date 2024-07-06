from dataclasses import dataclass


@dataclass
class TargetFunctionMyParams:
    arg1: int
    arg2: str
    arg3: str


def target_function(
        my_params: TargetFunctionMyParams):
    arg1 = my_params.arg1
    arg2 = my_params.arg2
    arg1 = 10
    arg2 += '123'
    return arg1 + arg2 + my_params.arg3


target_function(
    my_params=TargetFunctionMyParams(arg1=1, arg2='2', arg3='3'))
