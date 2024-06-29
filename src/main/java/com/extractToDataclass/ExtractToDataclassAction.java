package com.extractToDataclass;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ExtractToDataclassAction extends AnAction {
    private static final String PARAMETER_NAME = "params";
    private static final String DATACLASS_NAME_SUFFIX = "Params";

    @Override
    public void actionPerformed(AnActionEvent event) {
        PyFunction function = getTargetFunction(event);

        if (function != null) {
            PyParameterList params = function.getParameterList();
            if (params.getParameters().length == 0) {
                return;
            }

            ExtractToDataclassDialogWrapper dialog =
                    new ExtractToDataclassDialogWrapper(Arrays.stream(function.getParameterList().getParameters()).map(PyParameter::getName).toArray(String[]::new));
            boolean isOk = dialog.showAndGet();
            if (!isOk) {
                return;
            }
            Vector<Integer> parametersIndicesToExtract = dialog.getSelectedParameterIndices();
            if (parametersIndicesToExtract == null) {
                throw new IllegalStateException("parametersIndicesToExtract is null");
            }

            PyFile targetFile = (PyFile) function.getContainingFile();
            ExtractToDataclassHelper helper = new ExtractToDataclassHelper(PARAMETER_NAME, DATACLASS_NAME_SUFFIX);
            WriteCommandAction.runWriteCommandAction(function.getProject(), "Extract Parameters To Dataclass", null,
                    () -> helper.extractParametersToDataclass(targetFile, function, parametersIndicesToExtract));
        }
    }

    private @Nullable PyFunction getTargetFunction(@NotNull AnActionEvent event) {
        PsiElement element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element instanceof PyFunction) {
            return (PyFunction) element;
        }
        return null;
    }

    @Override
    public void update(AnActionEvent event) {
        PyFunction function = getTargetFunction(event);
        event.getPresentation().setEnabled(function != null);
    }
}
