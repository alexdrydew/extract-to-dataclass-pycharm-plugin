package com.extractToDataclass;

import java.util.List;

public class ExtractToDataclassActionFunctionOverloadTest extends SingleFileExtractToDataclassTestCase {

    public void testExtractToDataclassExtractOverloadFunction() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    @Override
    protected String getPythonExamplePrefix() {
        return "function_overload";
    }
}
