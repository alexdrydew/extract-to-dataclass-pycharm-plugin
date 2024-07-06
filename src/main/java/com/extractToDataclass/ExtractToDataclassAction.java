package com.extractToDataclass;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

public class ExtractToDataclassAction extends AnAction {
    @TestOnly
    public static ExtractToDataclassActionData testActionData = null;

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        PyFunction function = getTargetFunction(event);
        if (function != null) {
            PyParameterList params = function.getParameterList();
            if (params.getParameters().length == 0) {
                return;
            }
            ExtractToDataclassActionData actionData = askForActionData(function);
            if (actionData == null || !actionData.isOk() || actionData.selectedParameterIndices().isEmpty()) {
                return;
            }

            PyFile targetFile = (PyFile) function.getContainingFile();
            ExtractToDataclassHelper helper = new ExtractToDataclassHelper(actionData.parameterName(),
                    actionData.className());
            WriteCommandAction.runWriteCommandAction(function.getProject(), "Extract Parameters To Dataclass", null,
                    () -> helper.extractParametersToDataclass(targetFile, function,
                            actionData.selectedParameterIndices()));
        } else {
            HintManager.getInstance().showErrorHint(event.getData(CommonDataKeys.EDITOR), "No function selected");
        }
    }

    private @Nullable ExtractToDataclassActionData askForActionData(PyFunction function) {
        // TODO: is there a clean way to test dialog wrappers?
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            return testActionData;
        }
        ExtractToDataclassDialogWrapper dialog =
                new ExtractToDataclassDialogWrapper(Arrays.stream(function.getParameterList().getParameters()).map(PyParameter::getName).toArray(String[]::new));
        boolean isOk = dialog.showAndGet();
        if (!isOk) {
            return null;
        }
        Vector<Integer> parametersIndicesToExtract = dialog.getSelectedParameterIndices();
        if (parametersIndicesToExtract == null) {
            throw new IllegalStateException("parametersIndicesToExtract is null");
        }
        String parameterName = dialog.getParameterName();
        if (parameterName == null) {
            throw new IllegalStateException("parameterName is null");
        }
        String className = dialog.getClassName();
        if (className == null) {
            throw new IllegalStateException("className is null");
        }
        return new ExtractToDataclassActionData(isOk, parametersIndicesToExtract, parameterName, className);
    }

    private @Nullable PyFunction getTargetFunction(@NotNull AnActionEvent event) {
        PsiElement element = event.getData(CommonDataKeys.PSI_ELEMENT);
        if (element instanceof PyFunction) {
            return (PyFunction) element;
        }
        return null;
    }
}
