package io.inugami.maven.plugin.analysis.api.utils.reflection.fieldTransformers;

import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ClassCursor;
import io.inugami.maven.plugin.analysis.api.utils.reflection.FieldTransformer;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;

public class ListFieldTransformer implements FieldTransformer {

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(Field field, Class<?> type, Type genericType,String currentPath) {
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

        Class<?> classType = ReflectionService.extractGenericType(genericType);
        JsonNode children = null;

        if(cursor.isPresentInParents(classType)){
            builder.type(ReflectionService.renderFieldTypeRecursive(classType));
        }else{
             children = ReflectionService.renderStructureJson(ReflectionService.extractGenericType(genericType), currentPath+"[]",
                                                                      cursor.createNewContext(classType));
        }

        if (children != null) {
            builder.children(List.of(children));
        }
    }
}
