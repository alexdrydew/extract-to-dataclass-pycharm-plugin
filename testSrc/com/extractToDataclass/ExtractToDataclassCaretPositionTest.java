package com.extractToDataclass;

import java.util.List;

public class ExtractToDataclassCaretPositionTest extends SingleFileExtractToDataclassTestCase {

    public void testExtractWithCaretOnArgument() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    @Override
    protected String getPythonExamplePrefix() {
        return "caret_position";
    }
}
