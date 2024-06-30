package com.extractToDataclass;

import java.util.List;

public record ExtractToDataclassActionData(boolean isOk, List<Integer> selectedParameterIndices, String parameterName,
                                           String className) {
}
