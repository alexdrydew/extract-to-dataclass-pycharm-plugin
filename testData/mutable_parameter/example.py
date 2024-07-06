def <caret>target_function(
        arg1: int,
        arg2: str,
        arg3: str,
):
    arg1 = 10
    arg2 += '123'
    return arg1 + arg2 + arg3


target_function(
    arg1=1,
    arg2='2',
    arg3='3'
)
