package com.extractToDataclass;

import com.google.common.base.CaseFormat;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.types.PyCallableParameter;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ExtractToDataclassAction extends AnAction {
    private static final String DATACLASS_IDENTIFIER = "dataclass";
    private static final String PARAMETER_NAME = "params";
    private static final String CLASSNAME_SUFFIX = "Params";

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

            importDataclassIfNeeded(targetFile);

            PyClass clazz = createDataclass(targetFile, function, parametersIndicesToExtract);
            WriteCommandAction.runWriteCommandAction(function.getProject(), "Create class", null,
                    () -> targetFile.addBefore(clazz, function));
            String paramName = generateParamName(params, PARAMETER_NAME);
            WriteCommandAction.runWriteCommandAction(function.getProject(), "Create parameter", null,
                    () -> params.addParameter(PyElementGenerator.getInstance(function.getProject()).createParameter(paramName, null, clazz.getName(), LanguageLevel.getDefault())));
            removeParameters(function, clazz.getName(), paramName, parametersIndicesToExtract);
        }
    }

    private static String generateParamName(PyParameterList parameters, String baseName) {
        String name = baseName;
        int index = 1;
        while (parameters.findParameterByName(name) != null) {
            name = "%s%s".formatted(baseName, index);
            index++;
        }
        return name;
    }

    private static String generateDataclassName(String functionName) {
        return "%s%s".formatted(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, functionName), CLASSNAME_SUFFIX);
    }

    private static String generateDataclassName(String functionName, int index) {
        return "%s%s%s".formatted(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, functionName),
                CLASSNAME_SUFFIX, index);
    }

    private PyClass createDataclass(PyFile file, PyFunction function, List<Integer> parametersIndicesToExtract) {
        PyParameterList params = function.getParameterList();
        int freeIndex = 1;
        String dataclassName = generateDataclassName(function.getName());
        while (file.findTopLevelClass(dataclassName) != null) {
            dataclassName = generateDataclassName(function.getName(), freeIndex);
            freeIndex++;
        }
        String dataclassSource = buildParamsDataclassSource(params.getParameters(), dataclassName,
                parametersIndicesToExtract);
        return PyElementGenerator.getInstance(function.getProject()).createFromText(LanguageLevel.getDefault(),
                PyClass.class, dataclassSource.toString());
    }

    private static void importDataclassIfNeeded(PyFile targetFile) {
        Project project = targetFile.getProject();
        if (!isDataclassImported(targetFile)) {
            PyFromImportStatement importDataclass =
                    PyElementGenerator.getInstance(project).createFromImportStatement(LanguageLevel.getDefault(),
                            "dataclasses", DATACLASS_IDENTIFIER, null);
            WriteCommandAction.runWriteCommandAction(project, "Add dataclass import", null,
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

    private static @NotNull String buildParamsDataclassSource(PyParameter[] params, String dataclassName,
                                                              List<Integer> parametersIndicesToExtract) {
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

    private void removeParameters(PyFunction function, String dataclassClassName, String dataclassParameterName,
                                  Vector<Integer> parametersIndicesToRemove) {
        PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(function.getProject());
        updateLocalParametersUsage(function, dataclassParameterName, parametersIndicesToRemove);

        PyParameter[] params = function.getParameterList().getParameters();
        HashSet<String> parametersToRemoveNames = new HashSet<>();
        for (Integer index : parametersIndicesToRemove) {
            parametersToRemoveNames.add(params[index].getName());
        }

        Query<PyCallExpression> callsQuery =
                ReferencesSearch.search(function, function.getUseScope()).filtering(reference -> reference instanceof PsiReference).mapping(reference -> reference.getElement().getParent()).filtering(element -> element instanceof PyCallExpression).mapping(element -> (PyCallExpression) element);
        for (PyCallExpression call : callsQuery.findAll()) {
            List<PyCallExpression.PyArgumentsMapping> argumentsMappings =
                    call.multiMapArguments(PyResolveContext.defaultContext(TypeEvalContext.userInitiated(function.getProject(), function.getContainingFile())));
            // TODO: handle overloaded functions?
            Vector<PyExpression> deletedArguments = new Vector<>();
            if (argumentsMappings.size() == 1) {
                PyCallExpression.PyArgumentsMapping mapping = argumentsMappings.get(0);
                for (Map.Entry<PyExpression, PyCallableParameter> entry : mapping.getMappedParameters().entrySet()) {
                    String argumentName = entry.getValue().getName();
                    if (parametersToRemoveNames.contains(argumentName)) {
                        WriteCommandAction.runWriteCommandAction(function.getProject(), "Remove Extracted Parameter",
                                null, () -> {
                            entry.getKey().delete();
                        });
                        deletedArguments.add(entry.getKey());
                    }
                }

                PyCallExpression dataclassCall = pyElementGenerator.createCallExpression(LanguageLevel.getDefault(),
                        dataclassClassName);
                PyArgumentList dataclassCallArguments = dataclassCall.getArgumentList();
                for (PyExpression deletedArgument : deletedArguments) {
                    dataclassCallArguments.addArgumentFirst(deletedArgument);
                }

                WriteCommandAction.runWriteCommandAction(function.getProject(), "Add Dataclass Call", null,
                        () -> call.getArgumentList().addArgument(pyElementGenerator.createKeywordArgument(LanguageLevel.getDefault(), dataclassParameterName, dataclassCall.getText())));
            }
        }


        WriteCommandAction.runWriteCommandAction(function.getProject(), "Remove Extracted Parameters", null, () -> {
            for (Integer index : parametersIndicesToRemove) {
                params[index].delete();
            }
        });
    }

    private static void updateLocalParametersUsage(PyFunction function, String dataclassParameterName,
                                                   Vector<Integer> parametersIndicesToRemove) {
        PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(function.getProject());
        PyParameter[] params = function.getParameterList().getParameters();
        for (Integer index : parametersIndicesToRemove) {
            PyParameter param = params[index];
            Query<PyReferenceExpression> query =
                    ReferencesSearch.search(param, new LocalSearchScope(function)).mapping(reference -> reference.getElement()).filtering(element -> element instanceof PyReferenceExpression).mapping(element -> (PyReferenceExpression) element);

            for (PyReferenceExpression reference : query.findAll()) {
                String newReferenceSource = "%s.%s".formatted(dataclassParameterName, reference.getName());
                PyExpression newReference = pyElementGenerator.createExpressionFromText(LanguageLevel.getDefault(),
                        newReferenceSource);
                WriteCommandAction.runWriteCommandAction(function.getProject(), "Update Extracted Parameter Usage",
                        null, () -> reference.replace(newReference));
            }
        }
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
