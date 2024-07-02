package com.extractToDataclass;

import com.google.common.base.CaseFormat;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;


abstract public class SingleFileExtractToDataclassTestCase extends BasePlatformTestCase {
    @Override
    protected @NotNull String getTestDataPath() {
        return "testData";
    }

    protected abstract String getPythonExamplePrefix();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ExtractToDataclassAction.testActionData = null;
        myFixture.configureByFile("%s/example.py".formatted(getPythonExamplePrefix()));
    }

    protected void triggerAction() {
        myFixture.performEditorAction("com.extractToDataclass.ExtractToDataclassAction");
    }

    protected String getExpectedFileName() {
        return "%s/example_after_%s.py".formatted(getPythonExamplePrefix(),
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, getTestName(true)));
    }
}
