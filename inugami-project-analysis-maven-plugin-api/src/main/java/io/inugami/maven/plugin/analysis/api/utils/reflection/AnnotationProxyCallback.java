package io.inugami.maven.plugin.analysis.api.utils.reflection;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class AnnotationProxyCallback implements InvocationHandler {
    private final Object       value;
    private final List<Method> methods;

    public AnnotationProxyCallback(final Object value) {
        this.value = value;
        methods = new ArrayList<>();
        if (value != null) {
            methods.addAll(Arrays.asList(value.getClass().getMethods()));
        }

    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Method currentMethod = searchMethod(method);
        if (currentMethod == null) {
            return null;
        }
        return currentMethod.invoke(value, args);
    }

    private Method searchMethod(final Method method) {
        Method result = null;
        for (final Method item : methods) {
            if (item.getName().equals(method.getName())
                    && item.getParameterCount() == method.getParameterCount()
                    && convertToString(item.getParameters()).equals(convertToString(method.getParameters()))) {
                result = item;
                break;
            }
        }
        return result;
    }

    private String convertToString(final Parameter[] parameters) {
        final StringBuilder result = new StringBuilder();
        if (parameters == null) {
            return result.toString();
        }
        for (final Parameter param : parameters) {
            result.append("|").append(param.getType().getName());
        }
        return result.toString();
    }
}
