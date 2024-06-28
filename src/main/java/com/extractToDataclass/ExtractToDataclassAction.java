package com.extractToDataclass;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ExtractToDataclassAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        PyFunction function = getTargetFunction(event);


        if (function != null) {
            PyParameterList params = function.getParameterList();
            if (params.getParameters().length == 0) {
                return;
            }

            ExtractToDataclassDialogWrapper dialog = new ExtractToDataclassDialogWrapper(Arrays.stream(function.getParameterList().getParameters()).map(PyParameter::getName).toArray(String[]::new));
            boolean isOk = dialog.showAndGet();
            if (!isOk) {
                return;
            }
            Vector<Integer> parametersIndicesToExtract = dialog.getSelectedParameterIndices();
            if (parametersIndicesToExtract == null) {
                throw new IllegalStateException("parametersIndicesToExtract is null");
            }

            PsiFile targetFile = function.getContainingFile();
            String paramsDataclassStringBuilder = buildParamsDataclassSource(params.getParameters(), parametersIndicesToExtract);
            PyClass clazz = PyElementGenerator.getInstance(function.getProject()).createFromText(
                    LanguageLevel.getDefault(), PyClass.class, paramsDataclassStringBuilder.toString()
            );
            PyFromImportStatement importDataclass = PyElementGenerator.getInstance(function.getProject()).createFromText(
                    LanguageLevel.getDefault(),
                    PyFromImportStatement.class,
                    "from dataclasses import dataclass\n"
            );
            WriteCommandAction.runWriteCommandAction(
                    function.getProject(),
                    "Add dataclass import",
                    null,
                    () -> targetFile.addBefore(importDataclass, targetFile.getFirstChild())
            );
            WriteCommandAction.runWriteCommandAction(
                    function.getProject(),
                    "Create class",
                    null,
                    () -> targetFile.addBefore(clazz, function)
            );
            removeParameters(function, parametersIndicesToExtract);
        }
    }

    private static @NotNull String buildParamsDataclassSource(
            PyParameter[] params,
            List<Integer> parametersIndicesToExtract
    ) {
        StringBuilder paramsDataclassStringBuilder = new StringBuilder();
        paramsDataclassStringBuilder.append("@dataclass\n");
        paramsDataclassStringBuilder.append("class Params:\n");
        for (Integer index : parametersIndicesToExtract) {
            PyNamedParameter namedParam = params[index].getAsNamed();
            paramsDataclassStringBuilder.append("    ");
            paramsDataclassStringBuilder.append(namedParam.getName());
            PyAnnotation annotation = namedParam.getAnnotation();
            if (annotation != null) {
                paramsDataclassStringBuilder.append(annotation.getText());
            } else {
                paramsDataclassStringBuilder.append(": Any");
            }
            paramsDataclassStringBuilder.append("\n");
        }
        return paramsDataclassStringBuilder.toString();
    }

    private void removeParameters(PyFunction function, Vector<Integer> parametersIndicesToRemove) {
        PyParameter[] params = function.getParameterList().getParameters();
        WriteCommandAction.runWriteCommandAction(
                function.getProject(),
                "Remove extracted parameters",
                null,
                () -> {

                    for (Integer index : parametersIndicesToRemove) {
                        params[index].delete();
                    }
                }
        );
    }

    private PyFunction getTargetFunction(@NotNull AnActionEvent event) {
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
