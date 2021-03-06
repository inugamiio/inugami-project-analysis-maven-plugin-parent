/* --------------------------------------------------------------------
 *  Inugami
 * --------------------------------------------------------------------
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.inugami.maven.plugin.analysis.api.utils.reflection;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.inugami.api.loggers.Loggers;
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.api.utils.reflection.fieldTransformers.DefaultFieldTransformer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionService {
    private static       ClassLoader           CLASS_LOADER   = null;
    private static final Map<String, JsonNode> CACHE          = new LinkedHashMap<>();
    private static final List<Class<?>>        PRIMITIF_TYPES = List.of(
            boolean.class,
            byte.class,
            char.class,
            short.class,
            int.class,
            long.class,
            float.class,
            double.class,
            long.class);

    // =========================================================================
    // API
    // =========================================================================
    private static final List<FieldTransformer> FIELD_TRANSFORMERS = SpiLoader.INSTANCE
            .loadSpiServicesByPriority(FieldTransformer.class, new DefaultFieldTransformer());

    public static Class<?> loadClass(final String className, final ClassLoader classLoader) {
        try {
            return Class.forName(className, true, classLoader);
        }
        catch (final ClassNotFoundException e) {
            return null;
        }
    }

    public static Set<Field> loadAllFields(final Class<?> clazz) {
        final Set<Field> result = new LinkedHashSet<>();
        try {
            if (clazz != null && clazz != Object.class) {
                result.addAll(Arrays.asList(clazz.getDeclaredFields()));
                if (clazz.getSuperclass() != null) {
                    result.addAll(loadAllFields(clazz.getSuperclass()));
                }
            }
        }
        catch (final Throwable err) {
            Loggers.DEBUG.warn(err.getMessage());
        }

        return result;
    }

    public static Set<Field> loadAllStaticFields(final Class<?> clazz) {
        final Set<Field> result = new LinkedHashSet<>();
        loadAllFields(clazz).stream()
                            .filter(field -> Modifier.isStatic(field.getModifiers()))
                            .forEach(result::add);
        return result;
    }


    public static Set<Constructor<?>> loadAllConstructors(final Class<?> clazz) {
        final Set<Constructor<?>> result = new LinkedHashSet<>();
        try {
            if (clazz != null && clazz != Object.class) {
                result.addAll(Arrays.asList(clazz.getDeclaredConstructors()));
                if (clazz.getSuperclass() != null) {
                    result.addAll(loadAllConstructors(clazz.getSuperclass()));
                }
            }
        }
        catch (final Throwable err) {
            Loggers.DEBUG.warn(err.getMessage());
        }

        return result;
    }

    public static List<Method> loadAllMethods(final Class<?> clazz) {
        final List<Method> result = new ArrayList<>();

        try {
            if (clazz != null && clazz != Object.class) {
                result.addAll(Arrays.asList(clazz.getDeclaredMethods()));
                if (clazz.getSuperclass() != null) {
                    result.addAll(loadAllMethods(clazz.getSuperclass()));
                }
            }

        }
        catch (final Throwable err) {
            Loggers.DEBUG.warn(err.getMessage());
        }

        return result;
    }

    public static <AE extends AnnotatedElement> boolean hasAnnotation(final AE annotatedElement,
                                                                      final Class<? extends Annotation>... annotations) {
        boolean result = false;
        if (annotatedElement != null && annotations != null) {
            for (final Class<? extends Annotation> annotation : annotations) {
                result = annotatedElement.getDeclaredAnnotation(annotation) != null;
                if (result) {
                    break;
                }
            }
        }
        return result;
    }


    public static <T, A extends Annotation, AE extends AnnotatedElement> T ifHasAnnotation(final AE annotatedElement,
                                                                                           final Class<A> annotation,
                                                                                           final Function<A, T> handler) {
        return ifHasAnnotation(annotatedElement, annotation, handler, null);
    }


    public static <T, A extends Annotation, AE extends AnnotatedElement> T ifHasAnnotation(final AE annotatedElement,
                                                                                           final Class<A> annotation,
                                                                                           final Function<A, T> handler,
                                                                                           final Supplier<T> defaultValue) {
        T result = null;
        if (hasAnnotation(annotatedElement, annotation)) {
            result = handler == null ? null : handler.apply(annotatedElement.getDeclaredAnnotation(annotation));
        }

        if (result == null && defaultValue != null) {
            result = defaultValue.get();
        }
        return result;
    }


    public static <T, A extends Annotation, AE extends AnnotatedElement> void processOnAnnotation(
            final AE annotatedElement,
            final Class<A> annotationClass,
            final Consumer<A> handler) {
        final A annotation = annotatedElement == null ? null : annotatedElement.getDeclaredAnnotation(annotationClass);
        if (annotation != null && handler != null) {
            handler.accept(annotation);
        }
    }

    public static JsonNode renderParameterType(final Parameter parameter) {
        return renderParameterType(parameter, true);
    }

    public static JsonNode renderParameterType(final Parameter parameter, final boolean strict) {
        return renderType(parameter.getType(), parameter.getParameterizedType(), new ClassCursor(), strict);
    }

    public static JsonNode renderReturnType(final Method method) {
        return renderReturnType(method, true);
    }

    public static JsonNode renderReturnType(final Method method, final boolean strict) {
        return renderType(method.getReturnType(), method.getGenericReturnType(), new ClassCursor(),strict);
    }

    public static JsonNode renderType(final Class<?> type,
                                      final Type genericReturnType,
                                      final ClassCursor classCursor) {
        return renderType(type, genericReturnType, classCursor, true);
    }

    public static JsonNode renderType(final Class<?> type,
                                      final Type genericReturnType,
                                      final ClassCursor classCursor,
                                      final boolean strict) {
        final String key = "class:" + (type == null ? "null" : type
                .getName()) + ":" + (genericReturnType == null ? null : genericReturnType.getTypeName())
                + ":strict " + strict;

        final ClassCursor cursor = classCursor == null ? new ClassCursor() : classCursor;
        JsonNode          result = CACHE.get(key);
        if (result == null) {
            final Class<?> returnClass = type;

            final ClassCursor cursorChildren = cursor.createNewContext(returnClass);
            if (returnClass != null && !"void".equals(returnClass.getName())) {

                String     path       = null;
                final Type returnType = genericReturnType;

                Class<?> currentClass = returnClass;
                if (returnType != null) {
                    currentClass = extractGenericType(returnType);
                }

                if (Collection.class.isAssignableFrom(returnClass)) {
                    final JsonNode.JsonNodeBuilder builder = JsonNode.builder();
                    builder.list(true);
                    path = "[]";
                    builder.path("[]");
                    JsonNode structure = null;
                    if (currentClass != null) {
                        structure = renderStructureJson(currentClass, path, cursorChildren,strict);
                    }

                    if (structure != null) {
                        builder.children(List.of(structure));
                    }
                    result = builder.build();
                }
                else if (isBasicType(currentClass)) {
                    final JsonNode.JsonNodeBuilder node = JsonNode.builder()
                                                                  .type(renderFieldType(currentClass))
                                                                  .basicType(true);
                    result = node.build();
                }
                else {
                    result = renderStructureJson(currentClass, null, cursorChildren, strict);

                }
                Loggers.DEBUG.debug("json structure : {}\n{}", currentClass.getTypeName(), result.convertToJson());
            }

            if (result != null) {
                CACHE.put(key, result);
            }

        }

        return result;
    }


    public static boolean isBasicType(final Class<?> currentClass) {
        return currentClass == null ? true :
               PRIMITIF_TYPES.contains(currentClass) || currentClass.getName().startsWith("java.lang");
    }

    public static JsonNode renderStructureJson(final Class<?> genericType, final String path,
                                               final ClassCursor cursor) {
        return renderStructureJson(genericType, path, cursor, true);
    }

    public static JsonNode renderStructureJson(final Class<?> genericType, final String path,
                                               final ClassCursor cursor,
                                               final boolean strict) {
        final JsonNode.JsonNodeBuilder result = JsonNode.builder();
        result.structure(true);
        final String currentPath = path == null ? "" : path + ".";
        result.path(currentPath);

        if (genericType == null) {
            Loggers.DEBUG.warn("generic is null for path : {}", path);
        }
        else if (isBasicType(genericType)) {
            result.structure(false);
            result.basicType(true);
            result.type(renderFieldType(genericType));
        }
        else {
            final List<Field> fields = new ArrayList<>(Arrays.asList(genericType.getDeclaredFields()));
            fields.addAll(extractParentsFields(genericType.getSuperclass()));
            final List<JsonNode> fieldNodes = new ArrayList<>();

            for (final Field field : fields) {
                if (strict || hasConstraints(field)) {
                    fieldNodes.add(renderFieldJson(field, currentPath, cursor));
                }
            }
            result.children(fieldNodes);
        }

        return result.build();
    }

    private static boolean hasConstraints(final Field field) {
        boolean result = false;
        if (field.getAnnotations().length > 0) {
            for (final Annotation annotation : field.getAnnotations()) {
                result = annotation.annotationType().getName().endsWith(".persistence.Id") || hasAnnotation(
                        annotation.annotationType(), Constraint.class);
                if (result) {
                    break;
                }
            }
        }
        return result;
    }

    public static JsonNode renderFieldJson(final Field field, final String parentPath,
                                           final ClassCursor cursor) {
        final JsonNode.JsonNodeBuilder result      = JsonNode.builder();
        final Type                     genericType = field.getGenericType();
        final Class<?> fieldClass = genericType == null && genericType != Class.class ? field
                .getType() : extractGenericType(genericType);

        final String name = ifHasAnnotation(field, JsonProperty.class, JsonProperty::value, field::getName);
        result.fieldName(name);

        final String currentPath = parentPath + "." + name;
        result.path(currentPath);


        for (final FieldTransformer transformer : FIELD_TRANSFORMERS) {
            if (transformer.accept(field, fieldClass, genericType, currentPath)) {
                try {
                }
                catch (final Exception error) {
                    log.error(error.getMessage(), error);
                }
                transformer.transform(field, fieldClass, genericType, result, currentPath, cursor);
                if (transformer.stop(field, fieldClass, genericType, currentPath)) {
                    break;
                }
            }
        }

        return result.build();
    }


    public static String renderFieldType(final Class<?> classType) {
        return classType==null? "null" : classType.getSimpleName();
    }

    public static String renderFieldTypeRecursive(final Class<?> classType) {
        return String.format("\"<<%s>>\"", classType.getSimpleName());
    }

    public static String renderFieldTypeRecursiveNoQuot(final Class<?> classType) {
        return String.format("<<%s>>", classType.getSimpleName());
    }


    public static List<Field> extractParentsFields(final Class<?> superclass) {
        final List<Field> result = new ArrayList<>();
        if (superclass != null) {
            result.addAll(Arrays.asList(superclass.getDeclaredFields()));
            if (superclass.getSuperclass() != null) {
                result.addAll(extractParentsFields(superclass.getSuperclass()));
            }
        }
        return result;
    }


    public static Class<?> getGenericType(final Type type) {
        Class<?> result = null;
        if (type instanceof ParameterizedType) {
            final ParameterizedType paramType = (ParameterizedType) type;
            final Type[]            argTypes  = paramType.getActualTypeArguments();
            if (argTypes.length > 0) {
                result = argTypes[0].getClass();
            }
        }
        return result;
    }

    public static Class<?> extractGenericType(final Type genericType) {
        return extractGenericType(genericType, 0);
    }

    public static Class<?> extractGenericType(final Type genericType, final int typeIndex) {
        Class<?> result = null;
        if (genericType != null) {
            if (genericType instanceof ParameterizedType) {
                final String className = ((ParameterizedType) genericType).getActualTypeArguments()[typeIndex]
                        .getTypeName();
                try {
                    result = getClassloader().loadClass(className);
                }
                catch (final ClassNotFoundException e) {
                    log.error("no class def found : {}", genericType);
                }
            }
            else if (genericType instanceof Class) {
                result = (Class<?>) genericType;
            }

        }
        return result;
    }

    private static ClassLoader getClassloader() {
        return CLASS_LOADER == null ? Thread.currentThread().getContextClassLoader() : CLASS_LOADER;
    }

    public static synchronized void initializeClassloader(final ClassLoader classLoader) {
        CLASS_LOADER = classLoader;
    }


    public static Field buildField(final Class<?> type, final String name) {
        Field                field       = null;
        final Constructor<?> constructor = Field.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        try {
            field = (Field) constructor.newInstance(type, name, type, 0, 0, null, null);
        }
        catch (final Exception e) {
            Loggers.DEBUG.error(e.getMessage());
        }
        return field;
    }


}
