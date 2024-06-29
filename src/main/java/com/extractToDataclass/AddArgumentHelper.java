package com.extractToDataclass;

import com.google.common.collect.Collections2;
import com.google.common.collect.Queues;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.NotNullPredicate;
import com.jetbrains.python.psi.PyArgumentList;
import com.jetbrains.python.psi.PyElementGenerator;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyKeywordArgument;
import com.jetbrains.python.psi.impl.PyElementGeneratorImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;


/**
 * Helper class to add arguments to a PyArgumentList (PyArgumentList.addArgument() is bugged)
 */
public class AddArgumentHelper {
    private final PyArgumentList pyArgumentList;
    private static final NoKeyArguments NO_KEY_ARGUMENTS = new NoKeyArguments();

    private static class NoKeyArguments extends NotNullPredicate<PyExpression> {

        @Override
        protected boolean applyNotNull(@NotNull final PyExpression input) {
            return (PsiTreeUtil.getParentOfType(input, PyKeywordArgument.class) == null) && !(input instanceof PyKeywordArgument);
        }
    }

    public AddArgumentHelper(@NotNull PyArgumentList pyArgumentList) {
        this.pyArgumentList = pyArgumentList;
    }

    @NotNull
    private Deque<PyKeywordArgument> getKeyWordArguments() {
        return Queues.newArrayDeque(PsiTreeUtil.findChildrenOfType(pyArgumentList, PyKeywordArgument.class).stream().filter(child -> child.getParent() == this.pyArgumentList).toList());
    }

    @NotNull
    private Deque<PyExpression> getParameters() {
        final PyExpression[] childrenOfType = PsiTreeUtil.getChildrenOfType(pyArgumentList, PyExpression.class);
        if (childrenOfType == null) {
            return new ArrayDeque<>(0);
        }
        return Queues.newArrayDeque(Collections2.filter(Arrays.stream(childrenOfType).filter(child -> child.getParent() == pyArgumentList).toList(), NO_KEY_ARGUMENTS));
    }


    public void addArgument(@NotNull final PyExpression arg) {
        final PyElementGenerator generator = new PyElementGeneratorImpl(pyArgumentList.getProject());

        // Adds param to appropriate place
        final Deque<PyKeywordArgument> keywordArguments = getKeyWordArguments();
        final Deque<PyExpression> parameters = getParameters();

        if (keywordArguments.isEmpty() && parameters.isEmpty()) {
            generator.insertItemIntoListRemoveRedundantCommas(pyArgumentList, null, arg);
            return;
        }


        if (arg instanceof PyKeywordArgument) {
            if (parameters.isEmpty()) {
                generator.insertItemIntoListRemoveRedundantCommas(pyArgumentList, keywordArguments.getLast(), arg);
            } else {
                if (keywordArguments.isEmpty()) {
                    generator.insertItemIntoListRemoveRedundantCommas(pyArgumentList, parameters.getLast(), arg);
                } else {
                    generator.insertItemIntoListRemoveRedundantCommas(pyArgumentList, keywordArguments.getLast(), arg);
                }
            }
        } else {
            if (parameters.isEmpty()) {
                generator.insertItemIntoListRemoveRedundantCommas(pyArgumentList, null, arg);
            } else {
                generator.insertItemIntoListRemoveRedundantCommas(pyArgumentList, parameters.getLast(), arg);
            }
        }
    }
}
