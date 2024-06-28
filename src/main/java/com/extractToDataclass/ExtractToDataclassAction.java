package com.extractToDataclass;

import com.google.common.base.CaseFormat;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.jetbrains.python.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class ExtractToDataclassAction extends AnAction {
    private static final String DATACLASS_IDENTIFIER = "dataclass";

    @Override
    public void actionPerformed(AnActionEvent event) {
        PyFunction function = getTargetFunction(event);

        if (function != null) {
            PyParameterList params = function.getParameterList();
            if (params.getParameters().length == 0) {
                return;
            }

            ExtractToDataclassDialogWrapper dialog = new ExtractToDataclassDialogWrapper(
                    Arrays.stream(function.getParameterList().getParameters())
                            .map(PyParameter::getName)
                            .toArray(String[]::new));
            boolean isOk = dialog.showAndGet();
            if (!isOk) {
                return;
            }
            Vector<Integer> parametersIndicesToExtract = dialog.getSelectedParameterIndices();
            if (parametersIndicesToExtract == null) {
                throw new IllegalStateException("parametersIndicesToExtract is null");
            }

            PyFile targetFile = (PyFile) function.getContainingFile();

            importDataclassIfNeeded(targetFile, function);

            PyClass clazz = createDataclass(function, parametersIndicesToExtract);
            WriteCommandAction.runWriteCommandAction(
                    function.getProject(),
                    "Create class",
                    null,
                    () -> targetFile.addBefore(clazz, function));
            WriteCommandAction.runWriteCommandAction(
                    function.getProject(),
                    "Create parameter",
                    null,
                    () -> params.addParameter(PyElementGenerator.getInstance(function.getProject()).createParameter(
                            "params", null, dataclassName, LanguageLevel.getDefault()
                    ))
            );
            removeParameters(function, parametersIndicesToExtract);
        }
    }

    private PyClass createDataclass(PyFunction function, List<Integer> parametersIndicesToExtract) {
        PyParameterList params = function.getParameterList();
        String dataclassName = "%sParams".formatted(
                CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, function.getName()));
        String dataclassSource = buildParamsDataclassSource(params.getParameters(), dataclassName, parametersIndicesToExtract);
        return PyElementGenerator.getInstance(function.getProject()).createFromText(
                LanguageLevel.getDefault(), PyClass.class, dataclassSource.toString());
    }

    private static void importDataclassIfNeeded(PyFile targetFile, PyFunction function) {
        if (!isDataclassImported(targetFile)) {
            PyFromImportStatement importDataclass = PyElementGenerator.getInstance(function.getProject()).createFromImportStatement(
                    LanguageLevel.getDefault(),
                    "dataclasses",
                    DATACLASS_IDENTIFIER,
                    null);
            WriteCommandAction.runWriteCommandAction(
                    function.getProject(),
                    "Add dataclass import",
                    null,
                    () -> targetFile.addBefore(importDataclass, targetFile.getFirstChild()));
        }
    }

    private static boolean isDataclassImported(PyFile targetFile) {
        for (PyFromImportStatement fromImport : targetFile.getFromImports()) {
            if (!fromImport.getImportSource().asQualifiedName().toString().equals("dataclasses")) continue;
            for (PyImportElement importElement : fromImport.getImportElements()) {
                if (importElement.getVisibleName().equals(DATACLASS_IDENTIFIER)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @NotNull String buildParamsDataclassSource(
            PyParameter[] params,
            String dataclassName,
            List<Integer> parametersIndicesToExtract
    ) {
        StringBuilder paramsDataclassStringBuilder = new StringBuilder();
        paramsDataclassStringBuilder.append("@dataclass\n");
        paramsDataclassStringBuilder.append("class %s:\n".formatted(dataclassName));
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
