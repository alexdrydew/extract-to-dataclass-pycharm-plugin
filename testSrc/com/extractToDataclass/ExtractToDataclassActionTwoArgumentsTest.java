package com.extractToDataclass;

import java.util.List;


public class ExtractToDataclassActionTwoArgumentsTest extends SingleFileExtractToDataclassTestCase {

    public void testExtractTwoArgumentsToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    public void testExtractFirstArgumentToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    public void testExtractSecondArgumentToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(1), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    public void testExtractNoArgumentsToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    @Override
    protected String getPythonExamplePrefix() {
        return "two_arguments";
    }
}
