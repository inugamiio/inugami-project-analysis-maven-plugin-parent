package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.resolvers;

import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.BeanPropertyTypeResolver;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.buildField;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.extractGenericType;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY_TYPE;

public class CollectionTypeResolver implements BeanPropertyTypeResolver {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<Node> resolve(final String path, final Field field, final Class<?> fieldType, final Class<?> clazz,
                             final SpringPropertiesAnalyzer springPropertiesAnalyzer) {

        Set<Node> result = null;
        if (Collection.class.isAssignableFrom(field.getType())) {
            result = processResolve(path, field, springPropertiesAnalyzer);
        }
        return result;
    }

    private Set<Node> processResolve(final String path, final Field field,
                                     final SpringPropertiesAnalyzer springPropertiesAnalyzer) {

        Set<Node>      result      = null;
        final String   uid         = buildFullPath(path, field);
        final Class<?> genericType = extractGenericType(field.getGenericType());

        if (ReflectionService.isBasicType(genericType)) {
            final String type = springPropertiesAnalyzer.setShortName(genericType) ? genericType
                    .getSimpleName() : genericType.getName();
            final String                              nodeUid        = uid + "[].<" + type + ">";
            final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
            additionalInfo.put(PROPERTY_TYPE, type);

            final Node node = Node.builder()
                                  .uid(nodeUid)
                                  .name(nodeUid)
                                  .type(PROPERTY)
                                  .properties(additionalInfo)
                                  .build();

            springPropertiesAnalyzer.resolveConstraints(node, field.getDeclaredAnnotations());
            result = Set.of(node);
        } else {
            final Field fakeField = buildField(genericType, "[]");
            result = springPropertiesAnalyzer.extractFieldProperties(uid, genericType, fakeField);
        }
        return result;
    }

}
