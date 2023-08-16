package io.inugami.maven.plugin.analysis.api.utils.reflection.field.transformers;

import io.inugami.maven.plugin.analysis.api.utils.reflection.ClassCursor;
import io.inugami.maven.plugin.analysis.api.utils.reflection.FieldTransformer;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
@SuppressWarnings({"java:S1854","java:S120"})
public class ListFieldTransformer implements FieldTransformer {

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Field field, final Class<?> type, final Type genericType, final String currentPath) {
        return Collection.class.isAssignableFrom(field.getType());
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void transform(final Field field,
                          final Class<?> type,
                          final Type genericType,
                          final JsonNode.JsonNodeBuilder builder,
                          final String currentPath,
                          final ClassCursor cursor) {
        builder.list(true);

        final Class<?> classType = ReflectionService.extractGenericType(genericType);
        JsonNode       children  = null;

        if (cursor.isPresentInParents(classType)) {
            builder.type(ReflectionService.renderFieldTypeRecursive(classType));
        } else {
            children = classType == null ? null : ReflectionService.renderStructureJson(ReflectionService.extractGenericType(genericType), currentPath + "[]",
                                                                                        cursor.createNewContext(classType));
        }

        if (children != null) {
            builder.children(List.of(children));
        }
    }
}
