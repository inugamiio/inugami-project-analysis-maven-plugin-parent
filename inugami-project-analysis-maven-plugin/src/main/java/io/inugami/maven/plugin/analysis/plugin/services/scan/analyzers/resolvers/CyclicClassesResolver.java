package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.resolvers;

import io.inugami.api.spi.SpiPriority;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.BeanPropertyTypeResolver;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.SpringPropertiesAnalyzer.PROPERTY_TYPE;

@SpiPriority(1)
public class CyclicClassesResolver implements BeanPropertyTypeResolver {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final Map<String, String> CACHE = new LinkedHashMap<>();


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public Set<Node> resolve(final String path, final Field field, final Class<?> fieldType, final Class<?> clazz,
                             final SpringPropertiesAnalyzer springPropertiesAnalyzer) {
        Set<Node>    result   = null;
        final String fullPath = buildFullPath(path, field);
        if (isCyclic(fullPath, field.getType().getName())) {
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

    private boolean isCyclic(final String path, final String name) {
        boolean            result = false;
        final String[]     parts  = path.split("[.]");
        final List<String> paths  = new ArrayList<>();
        String             cursor = null;
        for (final String part : parts) {
            if (cursor == null) {
                cursor = part;
            }
            else {
                cursor = String.join(".", cursor, part);
            }
            paths.add(cursor);
        }

        for (int i = paths.size() - 2; i >= 0; i--) {
            final String rankedClass = CACHE.get(paths.get(i));
            result = rankedClass != null && rankedClass.equals(name);
            if(result){
                break;
            }
        }
        return result;
    }


    public void register(final String fullPath, final Field field) {
        CACHE.put(fullPath, field.getType().getName());
    }

    public void register(final String fullPath, final Class<?> clazz) {
        CACHE.put(fullPath, clazz.getName());
    }
}
