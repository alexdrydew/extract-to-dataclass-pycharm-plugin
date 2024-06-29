package com.extractToDataclass;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;


public class ExtractToDataclassDialogWrapper extends DialogWrapper {
    private final String[] argumentNames;
    private ParametersCheckboxGroup checkboxGroup;
    private JTextField parameterName;
    private JTextField className;

    private static final String PARAMETER_NAME = "params";
    private static final String CLASS_NAME = "Params";

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

    @Nullable
    public String getParameterName() {
        return parameterName == null ? null : parameterName.getText();
    }

    @Nullable
    public String getClassName() {
        return className == null ? null : className.getText();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel root = new JPanel(new BorderLayout());

        this.parameterName = new JTextField(PARAMETER_NAME);
        this.className = new JTextField(CLASS_NAME);

        JPanel inputFields = new JPanel(new BorderLayout());
        JPanel parameterNameTextField = new JPanel(new BorderLayout());
        parameterNameTextField.add(new JLabel("Parameter name:"), BorderLayout.NORTH);
        parameterNameTextField.add(this.parameterName, BorderLayout.SOUTH);
        inputFields.add(parameterNameTextField, BorderLayout.NORTH);
        JPanel classNameTextField = new JPanel(new BorderLayout());
        classNameTextField.add(new JLabel("Class name suffix:"), BorderLayout.NORTH);
        classNameTextField.add(this.className, BorderLayout.SOUTH);
        inputFields.add(classNameTextField, BorderLayout.SOUTH);

        root.add(inputFields, BorderLayout.NORTH);

        this.checkboxGroup = new ParametersCheckboxGroup(argumentNames);
        root.add(this.checkboxGroup, BorderLayout.SOUTH);

        return root;
    }
}
