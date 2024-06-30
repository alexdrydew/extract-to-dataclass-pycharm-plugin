package com.extractToDataclass;

import com.google.common.base.CaseFormat;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class ExtractToDataclassActionTest extends BasePlatformTestCase {
    @Override
    protected @NotNull String getTestDataPath() {
        return "testData";
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ExtractToDataclassAction.testActionData = null;
        myFixture.configureByFile("two_arguments/example.py");
    }

    private void triggerAction() {
        myFixture.performEditorAction("com.extractToDataclass.ExtractToDataclassAction");
    }

    private String getExpectedFileName() {
        return "two_arguments/example_after_%s.py".formatted(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE,
                getTestName(true)));
    }

    public void testExtractTwoArgumentsToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "params",
                "Params");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    public void testExtractFirstArgumentToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0), "params",
                "Params");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    public void testExtractSecondArgumentToDataclass() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(1), "params",
                "Params");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }
}
