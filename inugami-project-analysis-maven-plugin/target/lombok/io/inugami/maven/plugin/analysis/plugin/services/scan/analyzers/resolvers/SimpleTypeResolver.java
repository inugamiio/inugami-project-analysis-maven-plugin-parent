package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.resolvers;

import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.BeanPropertyTypeResolver;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY_TYPE;


public class SimpleTypeResolver implements BeanPropertyTypeResolver {

    // =========================================================================
    // API
    // =========================================================================

    @Override
    public Set<Node> resolve(final String path, final Field field, final Class<?> fieldType, final Class<?> clazz,
                             final SpringPropertiesAnalyzer springPropertiesAnalyzer) {

        Set<Node> result = null;

        if (ReflectionService.isBasicType(field.getType())) {
            result = new LinkedHashSet<>();
            final String                    uid            = (path == null ? "" : path + ".") + field.getName();
            final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
            additionalInfo.put(PROPERTY_TYPE,
                               springPropertiesAnalyzer.setShortName(fieldType) ? fieldType.getSimpleName() : fieldType
                                       .getName());

            final Node node = Node.builder()
                                  .uid(uid)
                                  .name(uid)
                                  .type(PROPERTY)
                                  .properties(additionalInfo)
                                  .build();

            springPropertiesAnalyzer.resolveConstraints(node, field.getDeclaredAnnotations());
            result.add(node);
        }


        return result;
    }

}
