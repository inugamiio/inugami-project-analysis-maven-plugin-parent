package io.inugami.maven.plugin.analysis.api.utils.reflection.field.transformers;

import io.inugami.maven.plugin.analysis.api.utils.reflection.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

@SuppressWarnings({"java:S1854","java:S120"})
public class MapFieldTransformer implements FieldTransformer {


    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Field field, final Class<?> type, final Type genericType, final String currentPath) {
        return Map.class.isAssignableFrom(field.getType());
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
        builder.map(true);

        if (genericType instanceof ParameterizedType) {
            final Type[] arguments = ((ParameterizedType) genericType).getActualTypeArguments();
            if (arguments.length == 2) {
                final Class<?> keyClass = ReflectionService.extractGenericType(arguments[0]);
                builder.mapKey(keyClass == null ? UnknownJsonType.OBJECT : keyClass.getSimpleName());

                final Class<?> subType = ReflectionService.extractGenericType(arguments[1]);

                if (cursor.isPresentInParents(subType)) {
                    builder.type(ReflectionService.renderFieldTypeRecursive(subType));
                } else {
                    builder.mapValue(ReflectionService.renderStructureJson(subType, currentPath + ".{}",
                                                                           cursor.createNewContext(subType)));
                }

            } else {
                buildDefault(builder);
            }

        } else {
            buildDefault(builder);
        }

    }

    private void buildDefault(final JsonNode.JsonNodeBuilder builder) {
        builder.mapKey(UnknownJsonType.OBJECT);
        builder.mapValue(new UnknownJsonType());
    }
}
