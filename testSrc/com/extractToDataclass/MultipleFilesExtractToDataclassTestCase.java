package com.extractToDataclass;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MultipleFilesExtractToDataclassTestCase extends BasePlatformTestCase {
    @Override
    protected @NotNull String getTestDataPath() {
        return "testData";
    }

    public void testExtractAcrossTwoFiles() {
        myFixture.configureByFiles("multiple_modules/example.py", "multiple_modules/other_example.py");

        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "MyParams");
        myFixture.performEditorAction("com.extractToDataclass.ExtractToDataclassAction");
        myFixture.checkResultByFile("multiple_modules/example.py", "multiple_modules" +
                "/example_after_extract_across_two_files.py", true);
        myFixture.checkResultByFile("multiple_modules/other_example.py", "multiple_modules" +
                "/other_example_after_extract_across_two_files.py", true);
    }
}
