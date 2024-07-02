package com.extractToDataclass;

import com.google.common.base.CaseFormat;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import com.jetbrains.python.psi.LanguageLevel;
import com.jetbrains.python.psi.PyAnnotation;
import com.jetbrains.python.psi.PyArgumentList;
import com.jetbrains.python.psi.PyAssignmentStatement;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyElementGenerator;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyFromImportStatement;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyImportElement;
import com.jetbrains.python.psi.PyNamedParameter;
import com.jetbrains.python.psi.PyParameter;
import com.jetbrains.python.psi.PyParameterList;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.PyStatementList;
import com.jetbrains.python.psi.PyTargetExpression;
import com.jetbrains.python.psi.resolve.PyResolveContext;
import com.jetbrains.python.psi.types.PyCallableParameter;
import com.jetbrains.python.psi.types.TypeEvalContext;

import java.util.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.stream.Collectors;


// TODO: handle typevars in signature
public class ExtractToDataclassHelper {

    String parameterName;
    String dataclassNameSuffix;
    private static final String DATACLASS_IDENTIFIER = "dataclass";

    public ExtractToDataclassHelper(@NotNull String parameterName, @NotNull String dataclassNameSuffix) {
        this.parameterName = parameterName;
        this.dataclassNameSuffix = dataclassNameSuffix;
    }

    public void extractParametersToDataclass(@NotNull PyFile targetFile, @NotNull PyFunction function,
                                             @NotNull List<Integer> parametersIndicesToExtract) {
        PyParameterList params = function.getParameterList();

        importDataclassDecoratorIfNeeded(targetFile);

        PyClass clazz = createDataclass(targetFile, function, parametersIndicesToExtract);
        targetFile.addBefore(clazz, function);
        String className = clazz.getName();

        String paramName = generateUnusedParamName(params, parameterName);
        addDataclassParameterToFunction(function, params, paramName, clazz);

        updateLocalParametersUsage(function, paramName, parametersIndicesToExtract);
        updateFunctionCalls(function, className, paramName, parametersIndicesToExtract);
        removeParameters(function, parametersIndicesToExtract);
    }

    private static void addDataclassParameterToFunction(@NotNull PyFunction function, @NotNull PyParameterList params
            , @NotNull String paramName, @NotNull PyClass clazz) {
        params.addParameter(PyElementGenerator.getInstance(function.getProject()).createParameter(paramName, null,
                clazz.getName(), LanguageLevel.getDefault()));
    }

    private static String generateUnusedParamName(@NotNull PyParameterList parameters, @NotNull String baseName) {
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
        String dataclassName = generateUnusedInFileDataclassName(file, function);
        String dataclassSource = buildParamsDataclassSource(params.getParameters(), dataclassName,
                parametersIndicesToExtract);
        return createClassFromSource(PyElementGenerator.getInstance(function.getProject()), dataclassSource);
    }

    private static @NotNull PyClass createClassFromSource(@NotNull PyElementGenerator generator,
                                                          @NotNull String dataclassSource) {
        return generator.createFromText(LanguageLevel.getDefault(), PyClass.class, dataclassSource.toString());
    }

    private @NotNull String generateUnusedInFileDataclassName(@NotNull PyFile file, @NotNull PyFunction function) {
        int freeIndex = 1;
        String dataclassName = generateDataclassName(function.getName());
        while (file.findTopLevelClass(dataclassName) != null) {
            dataclassName = generateDataclassName(function.getName(), freeIndex);
            freeIndex++;
        }
        return dataclassName;
    }

    private void importDataclassDecoratorIfNeeded(@NotNull PyFile targetFile) {
        Project project = targetFile.getProject();
        if (!isDataclassImported(targetFile)) {
            PyFromImportStatement importDataclass =
                    PyElementGenerator.getInstance(project).createFromImportStatement(LanguageLevel.getDefault(),
                            "dataclasses", DATACLASS_IDENTIFIER, null);
            targetFile.addBefore(importDataclass, targetFile.getFirstChild());
        }
    }

    private static boolean hasFromImport(@NotNull PyFile targetFile, @NotNull String importSource,
                                         @NotNull String importElementName) {
        for (PyFromImportStatement fromImport : targetFile.getFromImports()) {
            if (!fromImport.getImportSource().asQualifiedName().toString().equals(importSource)) {
                continue;
            }
            for (PyImportElement importElement : fromImport.getImportElements()) {
                if (importElement.getVisibleName().equals(importElementName)) {
                    return true;
                }
            }
        }
        return false;
    }


    private static boolean isDataclassImported(@NotNull PyFile targetFile) {
        return hasFromImport(targetFile, "dataclasses", DATACLASS_IDENTIFIER);
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

    private void removeParameters(@NotNull PyFunction function, @NotNull List<Integer> parametersIndicesToRemove) {
        PyParameter[] params = function.getParameterList().getParameters();
        for (Integer index : parametersIndicesToRemove) {
            params[index].delete();
        }
    }

    private record PyElementFromImportSource(String importSource, String importElementName) {

    }

    private static @Nullable PyElementFromImportSource findClassFromImportSource(@NotNull PyFile targetFile,
                                                                                 String topLevelClassName) {
        PyClass clazz = targetFile.findTopLevelClass(topLevelClassName);
        if (clazz == null) {
            return null;
        }
        String classQualifiedName = clazz.getQualifiedName();
        assert classQualifiedName != null : "Class qualified name should not be null";
        int lastSeparatorIndex = classQualifiedName.lastIndexOf('.');
        String importSource = classQualifiedName.substring(0, lastSeparatorIndex);
        String className = classQualifiedName.substring(lastSeparatorIndex + 1);
        return new PyElementFromImportSource(importSource, className);
    }

    private static void updateFunctionCalls(@NotNull PyFunction function, @NotNull String dataclassClassName,
                                            @NotNull String dataclassParameterName,
                                            @NotNull List<Integer> parametersIndicesToRemove) {
        PyParameter[] params = function.getParameterList().getParameters();
        HashSet<String> parametersToRemoveNames = new HashSet<>();
        for (Integer index : parametersIndicesToRemove) {
            parametersToRemoveNames.add(params[index].getName());
        }
        Query<PyCallExpression> callsQuery =
                ReferencesSearch.search(function, function.getUseScope()).filtering(reference -> reference instanceof PsiReference).mapping(reference -> reference.getElement().getParent()).filtering(element -> element instanceof PyCallExpression).mapping(element -> (PyCallExpression) element);

        for (PyCallExpression call : callsQuery.findAll()) {
            updateFunctionCall(call, dataclassClassName, dataclassParameterName, parametersToRemoveNames);
        }
    }

    private static void updateFunctionCall(PyCallExpression call, @NotNull String dataclassClassName,
                                           @NotNull String dataclassParameterName,
                                           HashSet<String> parametersToRemoveNames) {
        PyFunction targetFunction = getCalledFunction(call);
        PyFile targetFunctionFile = (PyFile) targetFunction.getContainingFile();
        List<PyCallExpression.PyArgumentsMapping> argumentsMappings =
                call.multiMapArguments(PyResolveContext.defaultContext(TypeEvalContext.userInitiated(targetFunction.getProject(), targetFunction.getContainingFile())));
        // TODO: handle overloaded functions?
        if (argumentsMappings.size() == 1) {
            PyFile callFile = (PyFile) call.getContainingFile();
            if (!callFile.equals(targetFunctionFile)) {
                PyElementFromImportSource dataclassImportSource = findClassFromImportSource(targetFunctionFile,
                        dataclassClassName);
                addParameterDataclassImportIfNeeded(dataclassImportSource, callFile);
            }

            Vector<PyExpression> deletedArguments = deleteArgumentsUsingMapping(parametersToRemoveNames,
                    argumentsMappings.get(0));
            addDataclassInitializationArgumentToCall(dataclassClassName, dataclassParameterName, call,
                    deletedArguments);
        } else {
            throw new IllegalStateException("Function call has multiple argument mappings");
        }
    }

    private static PyFunction getCalledFunction(PyCallExpression call) {
        return (PyFunction) (((PyReferenceExpression) call.getCallee()).getReference().resolve());
    }

    private static Vector<PyExpression> deleteArgumentsUsingMapping(HashSet<String> parametersToRemoveNames,
                                                                    PyCallExpression.PyArgumentsMapping argumentsMapping) {
        Vector<PyExpression> deletedArguments = new Vector<>();
        for (Map.Entry<PyExpression, PyCallableParameter> entry : argumentsMapping.getMappedParameters().entrySet()) {
            String argumentName = entry.getValue().getName();
            if (parametersToRemoveNames.contains(argumentName)) {
                entry.getKey().delete();
                deletedArguments.add(entry.getKey());
            }
        }
        return deletedArguments;
    }

    private static void addDataclassInitializationArgumentToCall(@NotNull String dataclassClassName,
                                                                 @NotNull String dataclassParameterName,
                                                                 @NotNull PyCallExpression call,
                                                                 @NotNull List<PyExpression> dataclassCallArguments) {
        PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(call.getProject());
        PyCallExpression dataclassCall = pyElementGenerator.createCallExpression(LanguageLevel.getDefault(),
                dataclassClassName);
        PyArgumentList dataclassCallArgumentsList = dataclassCall.getArgumentList();
        AddArgumentHelper dataclassCallArgumentsHelper = new AddArgumentHelper(dataclassCallArgumentsList);
        for (PyExpression argument : dataclassCallArguments) {
            dataclassCallArgumentsHelper.addArgument(argument);
        }
        AddArgumentHelper callArgumentsHelper = new AddArgumentHelper(call.getArgumentList());
        callArgumentsHelper.addArgument(pyElementGenerator.createKeywordArgument(LanguageLevel.getDefault(),
                dataclassParameterName, dataclassCall.getText()));
    }

    private static void addParameterDataclassImportIfNeeded(@NotNull PyElementFromImportSource dataclassImportSource,
                                                            @NotNull PyFile callFile) {
        if (!hasFromImport(callFile, dataclassImportSource.importSource(), dataclassImportSource.importElementName())) {
            PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(callFile.getProject());
            PyFromImportStatement dataclassImportElement =
                    pyElementGenerator.createFromImportStatement(LanguageLevel.getDefault(),
                            dataclassImportSource.importSource(), dataclassImportSource.importElementName(), null);
            callFile.addBefore(dataclassImportElement, callFile.getFirstChild());
        }
    }

    private static void updateLocalParametersUsage(@NotNull PyFunction function,
                                                   @NotNull String dataclassParameterName,
                                                   @NotNull List<Integer> parametersIndicesToRemove) {
        for (Integer index : parametersIndicesToRemove) {
            updateLocalParameterUsage(function, index, dataclassParameterName);
        }
    }

    private static void updateLocalParameterUsage(@NotNull PyFunction function, Integer parameterIndex,
                                                  @NotNull String dataclassParameterName) {
        PyParameter[] params = function.getParameterList().getParameters();
        PyParameter oldParam = params[parameterIndex];
        PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(function.getProject());
        String paramName = oldParam.getName();
        Collection<PsiElement> referencingElements = ReferencesSearch.search(oldParam,
                new LocalSearchScope(function)).mapping(reference -> reference.getElement()).findAll();

        boolean hasParameterAssignments =
                referencingElements.stream().anyMatch(element -> element instanceof PyTargetExpression);

        if (hasParameterAssignments) {
            assignParameterToDataclassParameterAttribute(function, dataclassParameterName, paramName, paramName);
        } else {
            replaceReferencesWithDataclassAttributeAccess(referencingElements.stream().filter(element -> element instanceof PyReferenceExpression).map(element -> (PyReferenceExpression) element).collect(Collectors.toList()), dataclassParameterName, Function.identity(), pyElementGenerator);
        }
    }

    private static void replaceReferencesWithDataclassAttributeAccess(@NotNull Collection<PyReferenceExpression> references, @NotNull String dataclassParameterName, @NotNull Function<String, String> parameterNameToAttributeName, @NotNull PyElementGenerator pyElementGenerator) {
        for (PyReferenceExpression reference : references) {
            String newReferenceSource = "%s.%s".formatted(dataclassParameterName,
                    parameterNameToAttributeName.apply(reference.getName()));
            PyExpression newReference = pyElementGenerator.createExpressionFromText(LanguageLevel.getDefault(),
                    newReferenceSource);
            reference.replace(newReference);
        }
    }

    private static void assignParameterToDataclassParameterAttribute(PyFunction function,
                                                                     @NotNull String dataclassParameterName,
                                                                     String paramName, String attributeName) {
        PyStatementList statementsList = function.getStatementList();
        PyElementGenerator pyElementGenerator = PyElementGenerator.getInstance(function.getProject());

        statementsList.addBefore(pyElementGenerator.createFromText(LanguageLevel.getDefault(),
                PyAssignmentStatement.class,
                "%s = %s.%s".formatted(paramName, dataclassParameterName, attributeName)),
                statementsList.getFirstChild());
    }
}
