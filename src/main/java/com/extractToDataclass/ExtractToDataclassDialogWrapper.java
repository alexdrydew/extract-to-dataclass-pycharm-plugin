package com.extractToDataclass;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Vector;


public class ExtractToDataclassDialogWrapper extends DialogWrapper {
    private final String[] argumentNames;
    private ParametersCheckboxGroup checkboxGroup;

    public ExtractToDataclassDialogWrapper(String[] argumentNames) {
        super(true);
        this.argumentNames = argumentNames;
        setTitle("Extract to Dataclass");
        init();
    }

    @Nullable
    public Vector<Integer> getSelectedParameterIndices() {
        return checkboxGroup == null ? null : checkboxGroup.getSelectedParameterIndices();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        checkboxGroup = new ParametersCheckboxGroup(argumentNames);
        return checkboxGroup;
    }
}
