package com.extractToDataclass;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class ExtractToDataclassActionTest extends BasePlatformTestCase {
    @Override
    protected @NotNull String getTestDataPath() {
        return "testData";
    }

    public void testExtractToDataclass() {
        myFixture.configureByFile("example.py");
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "params",
                "Params");
        myFixture.performEditorAction("com.extractToDataclass.ExtractToDataclassAction");
        myFixture.checkResultByFile("example_after.py");
    }
}
