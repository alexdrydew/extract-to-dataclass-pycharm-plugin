package com.extractToDataclass;

import java.util.List;

public class ExtractToDataclassActionNoAnnotationTest extends SingleFileExtractToDataclassTestCase {

    public void testExtractToDataclassNoAnnotation() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "Args");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    @Override
    protected String getPythonExamplePrefix() {
        return "no_annotation";
    }
}
