package com.extractToDataclass;

import com.google.common.base.CaseFormat;
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
import java.util.stream.Collectors;

public class ExtractToDataclassHelper {
    String parameterName;
    String dataclassNameSuffix;
    private static final String DATACLASS_IDENTIFIER = "dataclass";

    public ExtractToDataclassHelper(@NotNull String parameterName, @NotNull String dataclassNameSuffix) {
        this.parameterName = parameterName;
        this.dataclassNameSuffix = dataclassNameSuffix;
    }

    public PyClass extractParametersToDataclass(@NotNull PyFile targetFile, @NotNull PyFunction function,
                                                @NotNull List<Integer> parametersIndicesToExtract) {
        PyParameterList params = function.getParameterList();

        importDataclassIfNeeded(targetFile);
        PyClass clazz = createDataclass(targetFile, function, parametersIndicesToExtract);
        targetFile.addBefore(clazz, function);
        String paramName = generateParamName(params, parameterName);
        params.addParameter(PyElementGenerator.getInstance(function.getProject()).createParameter(paramName, null,
                clazz.getName(), LanguageLevel.getDefault()));
        removeParameters(function, clazz.getName(), paramName, parametersIndicesToExtract);
        return clazz;
    }

    private static String generateParamName(@NotNull PyParameterList parameters, @NotNull String baseName) {
        String name = baseName;
        int index = 1;
        while (parameters.findParameterByName(name) != null) {
            name = "%s%s".formatted(baseName, index);
            index++;
        }
        return name;
    }

    private @NotNull String generateDataclassName(@NotNull String functionName) {
        return "%s%s".formatted(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, functionName),
                dataclassNameSuffix);
    }

    private @NotNull String generateDataclassName(String functionName, int index) {
        return "%s%s%s".formatted(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, functionName),
                dataclassNameSuffix, index);
    }

    private @NotNull PyClass createDataclass(@NotNull PyFile file, @NotNull PyFunction function,
                                             @NotNull List<Integer> parametersIndicesToExtract) {
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

    private void importDataclassIfNeeded(@NotNull PyFile targetFile) {
        Project project = targetFile.getProject();
        if (!isDataclassImported(targetFile)) {
            PyFromImportStatement importDataclass =
                    PyElementGenerator.getInstance(project).createFromImportStatement(LanguageLevel.getDefault(),
                            "dataclasses", DATACLASS_IDENTIFIER, null);
            targetFile.addBefore(importDataclass, targetFile.getFirstChild());
        }
    }

    private static boolean isDataclassImported(@NotNull PyFile targetFile) {
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

    private static @NotNull String buildParamsDataclassSource(@NotNull PyParameter[] params,
                                                              @NotNull String dataclassName,
                                                              @NotNull List<Integer> parametersIndicesToExtract) {
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

    private void removeParameters(@NotNull PyFunction function, @NotNull String dataclassClassName,
                                  @NotNull String dataclassParameterName,
                                  @NotNull List<Integer> parametersIndicesToRemove) {
        updateLocalParametersUsage(function, dataclassParameterName, parametersIndicesToRemove);
        updateFunctionCalls(function, dataclassClassName, dataclassParameterName, parametersIndicesToRemove);

        PyParameter[] params = function.getParameterList().getParameters();
        for (Integer index : parametersIndicesToRemove) {
            params[index].delete();
        }
    }

    private static void updateFunctionCalls(@NotNull PyFunction function, @NotNull String dataclassClassName,
                                            @NotNull String dataclassParameterName,
                                            @NotNull List<Integer> parametersIndicesToRemove) {
        PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(function.getProject());
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
                        entry.getKey().delete();
                        deletedArguments.add(entry.getKey());
                    }
                }

                // TODO: handle function in other modules (need to import dataclass first)
                PyCallExpression dataclassCall = pyElementGenerator.createCallExpression(LanguageLevel.getDefault(),
                        dataclassClassName);
                PyArgumentList dataclassCallArguments = dataclassCall.getArgumentList();
                AddArgumentHelper dataclassCallArgumentsHelper = new AddArgumentHelper(dataclassCallArguments);
                for (PyExpression deletedArgument : deletedArguments) {
                    dataclassCallArgumentsHelper.addArgument(deletedArgument);
                }
                AddArgumentHelper callArgumentsHelper = new AddArgumentHelper(call.getArgumentList());
                callArgumentsHelper.addArgument(pyElementGenerator.createKeywordArgument(LanguageLevel.getDefault(),
                        dataclassParameterName, dataclassCall.getText()));
            }
        }
    }

    private static void updateLocalParametersUsage(@NotNull PyFunction function,
                                                   @NotNull String dataclassParameterName,
                                                   @NotNull List<Integer> parametersIndicesToRemove) {
        PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(function.getProject());
        PyParameter[] params = function.getParameterList().getParameters();
        PyStatementList statementsList = function.getStatementList();
        for (Integer index : parametersIndicesToRemove) {
            // TODO: handle assignments
            PyParameter param = params[index];
            String paramName = param.getName();

            Collection<PsiElement> referencingElements = ReferencesSearch.search(param,
                    new LocalSearchScope(function)).mapping(reference -> reference.getElement()).findAll();

            Set<String> assignedParameterNames =
                    referencingElements.stream().filter(element -> element instanceof PyTargetExpression).map(element -> ((PyTargetExpression) element).getName()).collect(Collectors.toSet());

            statementsList.addBefore(pyElementGenerator.createFromText(LanguageLevel.getDefault(),
                    PyAssignmentStatement.class,
                    "%s = %s.%s".formatted(paramName, dataclassParameterName, paramName)),
                    statementsList.getFirstChild());

            for (PsiElement referencingElement : referencingElements) {
                if (assignedParameterNames.contains(paramName)) continue;
                if (referencingElement instanceof PyReferenceExpression) {
                    PyReferenceExpression reference = (PyReferenceExpression) referencingElement;
                    String newReferenceSource = "%s.%s".formatted(dataclassParameterName, reference.getName());
                    PyExpression newReference =
                            pyElementGenerator.createExpressionFromText(LanguageLevel.getDefault(), newReferenceSource);
                    reference.replace(newReference);
                }
            }
        }
    }
}
