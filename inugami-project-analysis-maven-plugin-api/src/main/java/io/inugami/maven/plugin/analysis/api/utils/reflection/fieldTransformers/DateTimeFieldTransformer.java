package io.inugami.maven.plugin.analysis.api.utils.reflection.fieldTransformers;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.inugami.maven.plugin.analysis.api.utils.reflection.FieldTransformer;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ClassCursor;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.time.temporal.Temporal;
import java.util.Calendar;
import java.util.Date;

public class DateTimeFieldTransformer implements FieldTransformer {

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(Field field, Class<?> type, Type genericType, String currentPath) {
        return Date.class.isAssignableFrom(field.getType()) ||
                Calendar.class.isAssignableFrom(field.getType()) ||
                Temporal.class.isAssignableFrom(field.getType());
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void transform(final Field field, Class<?> type,
                          final Type genericType,
                          final JsonNode.JsonNodeBuilder builder,
                          final String currentPath,
                          final ClassCursor cursor) {
        String result = null;

        JsonFormat format = field.getAnnotation(JsonFormat.class);
        if (format != null && format.shape() != null) {
            switch (format.shape()) {
                case STRING:
                    result = format.pattern();
                    break;
                case NUMBER:
                    result = "long";
                    break;
                case NUMBER_INT:
                    result = "long<justDate>";
                    break;
            }
        }

        if (result == null) {
            builder.type(ReflectionService.renderFieldType(type));
        }else{
            builder.type(result);
        }
    }
}
