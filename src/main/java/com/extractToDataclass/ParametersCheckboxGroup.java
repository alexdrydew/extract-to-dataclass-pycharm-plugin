package com.extractToDataclass;

import com.intellij.openapi.ui.VerticalFlowLayout;

import javax.swing.*;
import java.util.Vector;

public class ParametersCheckboxGroup extends JPanel {
    private final Vector<JCheckBox> checkBoxes;

    public ParametersCheckboxGroup(String[] parameterNames) {
        super(new VerticalFlowLayout());
        checkBoxes = new Vector<>();
        for (String parameterName : parameterNames) {
            JCheckBox checkBox = new JCheckBox(parameterName);
            add(checkBox);
            checkBoxes.add(checkBox);
        }
    }

    public Vector<Integer> getSelectedParameterIndices() {
        Vector<Integer> selectedIndices = new Vector<>();
        for (int i = 0; i < checkBoxes.size(); i++) {
            if (checkBoxes.get(i).isSelected()) {
                selectedIndices.add(i);
            }
        }
        return selectedIndices;
    }
}
