package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.resolvers;

import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.BeanPropertyTypeResolver;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.buildField;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.extractGenericType;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY_TYPE;

public class MapTypeResolver implements BeanPropertyTypeResolver {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<Node> resolve(final String path, final Field field, final Class<?> fieldType, final Class<?> clazz,
                             final SpringPropertiesAnalyzer springPropertiesAnalyzer) {

        Set<Node> result = null;
        if (Map.class.isAssignableFrom(field.getType())) {
            result = processResolve(path, field, fieldType, clazz, springPropertiesAnalyzer);
        }
        return result;
    }

    private Set<Node> processResolve(final String path, final Field field, final Class<?> fieldType,
                                     final Class<?> clazz, final SpringPropertiesAnalyzer springPropertiesAnalyzer) {

        Set<Node>      result    = null;
        final Class<?> keyType   = extractGenericType(field.getGenericType());
        final Class<?> valueType = extractGenericType(field.getGenericType(), 1);

        final String currentPath = buildFullPath(path, field);

        final StringBuilder fieldName = new StringBuilder();
        fieldName.append('<');
        if (springPropertiesAnalyzer.setShortName(keyType)) {
            fieldName.append(keyType.getSimpleName());
        }
        else {
            fieldName.append(keyType.getName());
        }
        fieldName.append('>');

        if (ReflectionService.isBasicType(valueType)) {
            String type = "null";
            if (valueType != null) {
                type = springPropertiesAnalyzer.setShortName(valueType) ? valueType
                        .getSimpleName() : valueType.getName();
            }

            final String                    nodeUid        = currentPath + ".<" + type + ">";
            final Map<String, Serializable> additionalInfo = new HashMap<>();
            additionalInfo.put(PROPERTY_TYPE, type);

            final Node node = Node.builder()
                                  .uid(nodeUid)
                                  .name(nodeUid)
                                  .type(PROPERTY)
                                  .properties(additionalInfo)
                                  .build();

            springPropertiesAnalyzer.resolveConstraints(node, field.getDeclaredAnnotations());
            result = Set.of(node);
        }
        else {
            final Field fakeField = buildField(valueType, fieldName.toString());
            result = springPropertiesAnalyzer.extractFieldProperties(currentPath, valueType, fakeField);
        }

        return result;
    }

}
