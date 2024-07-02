package com.extractToDataclass;

import java.util.List;

public class ExtractToDataclassActionMutableParameterTest extends SingleFileExtractToDataclassTestCase {

    public void testExtractToDataclassWithFirstParameterAssignedInsideFunction() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    @Override
    protected String getPythonExamplePrefix() {
        return "mutable_parameter";
    }
}
