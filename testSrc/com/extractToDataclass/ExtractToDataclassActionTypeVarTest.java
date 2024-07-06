package com.extractToDataclass;

import java.util.List;

public class ExtractToDataclassActionTypeVarTest extends SingleFileExtractToDataclassTestCase {

    public void testExtractTypeVarArgumentsToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "Args");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    @Override
    protected String getPythonExamplePrefix() {
        return "type_var";
    }
}
