package com.extractToDataclass;

import java.util.List;

public class ExtractToDataclassActionAlreadyImportedTest extends BaseExtractToDataclassTestCase {

    public void testExtractToDataclassWhenDataclassIsAlreadyImported() {
        ExtractToDataclassAction.testActionData = new ExtractToDataclassActionData(true, List.of(0, 1), "my_params",
                "MyParams");
        triggerAction();
        myFixture.checkResultByFile(getExpectedFileName());
    }

    @Override
    protected String getPythonExamplePrefix() {
        return "dataclass_already_imported";
    }
}
