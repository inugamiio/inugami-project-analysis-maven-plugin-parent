package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import io.inugami.maven.plugin.analysis.api.models.Node;

import java.lang.reflect.Field;
import java.util.Set;

@FunctionalInterface
public interface BeanPropertyTypeResolver {

    Set<Node> resolve(String path,
                      Field field,
                      Class<?> fieldType,
                      Class<?> clazz,
                      SpringPropertiesAnalyzer springPropertiesAnalyzer);

    default String buildFullPath(final String path, final Field field) {
        return new StringBuilder()
                .append(path == null ? "" : path)
                .append(path == null ? "" : ".")
                .append(field.getName())
                .toString();
    }
}
