package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.resolvers;

import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.BeanPropertyTypeResolver;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ObjectTypeResolver implements BeanPropertyTypeResolver {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<Node> resolve(final String path, final Field field, final Class<?> fieldType, final Class<?> clazz,
                             final SpringPropertiesAnalyzer springPropertiesAnalyzer) {

        Set<Node> result = null;
        if (!ReflectionService.isBasicType(fieldType)
                && !Collection.class.isAssignableFrom(fieldType)
                && !Map.class.isAssignableFrom(fieldType)) {
            result = processResolve(path, field, fieldType, clazz, springPropertiesAnalyzer);
        }
        return result;
    }

    private Set<Node> processResolve(final String path, final Field field, final Class<?> fieldType,
                                     final Class<?> clazz, final SpringPropertiesAnalyzer springPropertiesAnalyzer) {

        final String currentPath = buildFullPath(path, field);
        return processResolveOnClass(currentPath, fieldType, springPropertiesAnalyzer);
    }

    public Set<Node> processResolveOnClass(final String path, final Class<?> fieldType,
                                            final SpringPropertiesAnalyzer springPropertiesAnalyzer
                                            ) {
        final Set<Node>  result = new HashSet<>();
        final Set<Field> fields = ReflectionService.loadAllFields(fieldType);



        for (final Field innerField : fields) {
            result.addAll(springPropertiesAnalyzer.extractFieldProperties(path, fieldType, innerField));
        }

        return result;
    }

}
