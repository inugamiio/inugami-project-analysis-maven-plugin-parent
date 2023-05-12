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
import io.inugami.commons.security.EncryptionUtils;
import io.inugami.maven.plugin.analysis.annotations.Description;
import io.inugami.maven.plugin.analysis.annotations.PotentialError;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.utils.reflection.fieldTransformers.DefaultFieldTransformer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Constraint;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.inugami.maven.plugin.analysis.api.utils.Constants.*;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflexionServiceUtils.*;
import static io.inugami.maven.plugin.analysis.functional.FunctionalUtils.applyIfNotNull;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ReflectionService {
    public static final  String                GET            = "get";
    public static final  String                IS             = "is";
    public static final  String                ERROR_CODE     = "errorCode";
    public static final  String                MESSAGE        = "message";
    public static final  String                MESSAGE_DETAIL = "messageDetail";
    public static final  String                ERROR_TYPE     = "errorType";
    public static final  String                PAYLOAD1       = "payload";
    public static final  String                STATUS_CODE    = "statusCode";
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
        return runSafe(() -> Class.forName(className, true, classLoader));
    }

    public static Class<?> safeLoadClass(final String className, final ClassLoader classLoader) {
        Class<?> result = loadClass(className, classLoader);
        if (result == null) {
            result = loadClass(className, ReflectionService.class.getClassLoader());
        }
        return result;
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
        JsonNode result = parameter == null
                          ? null
                          : renderType(parameter.getType(),
                                       parameter.getParameterizedType(),
                                       new ClassCursor(),
                                       strict);
        if (parameter.getAnnotation(Description.class) != null) {
            result = result.toBuilder()
                           .description(extractDescription(parameter.getAnnotation(Description.class)))
                           .build();
        }
        return result;
    }


    public static JsonNode renderReturnType(final Method method) {
        return renderReturnType(method, true);
    }

    public static JsonNode renderReturnType(final Method method, final boolean strict) {
        return renderType(method.getReturnType(), method.getGenericReturnType(), new ClassCursor(), strict);
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

        try {
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

                    if (isList(returnClass)) {
                        final JsonNode.JsonNodeBuilder builder = JsonNode.builder();
                        builder.list(true);
                        path = "[]";
                        builder.path("[]");
                        JsonNode structure = null;
                        if (currentClass != null) {
                            structure = renderStructureJson(currentClass, path, cursorChildren, strict);
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
                        log.info("render type : {}", type);
                        result = renderStructureJson(currentClass, null, cursorChildren, strict);
                        result.isStructure();

                    }
                    Loggers.DEBUG.debug("json structure : {}\n{}", currentClass.getTypeName(), result.convertToJson());
                }

                if (result != null) {
                    CACHE.put(key, result);
                }

            }
            return result;
        }
        catch (Throwable error) {
            log.error("error on rendering class : {} : {}", type.getName(), error.getMessage(), error);
            throw error;
        }
    }


    public static JsonNode renderStructureJson(final Class<?> genericType,
                                               final String path,
                                               final ClassCursor cursor) {
        return renderStructureJson(genericType, path, cursor, true);
    }

    public static JsonNode renderStructureJson(final Class<?> genericType,
                                               final String path,
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
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
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

    public static JsonNode renderFieldJson(final Field field,
                                           final String parentPath,
                                           final ClassCursor cursor) {
        final JsonNode.JsonNodeBuilder result      = JsonNode.builder();
        final Type                     genericType = field.getGenericType();
        final Class<?> fieldClass = genericType == null && genericType != Class.class
                                    ? field.getType()
                                    : extractGenericType(genericType);

        final String name = ifHasAnnotation(field, JsonProperty.class, JsonProperty::value, field::getName);
        result.fieldName(name);

        result.description(extractDescription(field.getAnnotation(Description.class)));

        final String currentPath = parentPath + "." + name;
        result.path(currentPath);


        for (final FieldTransformer transformer : FIELD_TRANSFORMERS) {
            if (transformer.accept(field, fieldClass, genericType, currentPath)) {
                try {
                    transformer.transform(field, fieldClass, genericType, result, currentPath, cursor);
                }
                catch (final Exception error) {
                    log.error(error.getMessage(), error);
                }

                if (transformer.stop(field, fieldClass, genericType, currentPath)) {
                    break;
                }
            }
        }

        return result.build();
    }


    public static String renderFieldType(final Class<?> classType) {
        return classType == null ? "null" : classType.getSimpleName();
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


    public static List<Node> extractInputDto(final Method method) {
        Set<Node> result = new LinkedHashSet<>();
        if (method != null) {
            for (Parameter parameter : method.getParameters()) {

                final Class<?> paramClass  = parameter.getType();
                final JsonNode payloadNode = renderType(paramClass, null, null, true);
                final String   payload     = payloadNode == null ? null : payloadNode.convertToJson();

                if (parameter == null || parameter.getName() == null || payload == null) {
                    continue;
                }
                final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
                additionalInfo.put(PAYLOAD, payload);
                result.add(Node.builder()
                               .name(parameter.getName())
                               .uid(encodeSha1(payload))
                               .type(INPUT_DTO)
                               .properties(additionalInfo)
                               .build());
            }
        }
        return new ArrayList<>(result);
    }

    public static Node extractOutputDto(final Method method) {
        Node           result     = null;
        final Class<?> returnType = method.getReturnType();
        if (returnType != null) {
            String payload = null;
            if (returnType == void.class || returnType == Void.class) {
                payload = "Void";
            }
            else {
                JsonNode payloadNode = renderType(returnType, null, null, true);
                payload = payloadNode == null ? "null" : payloadNode.convertToJson();
            }

            final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
            additionalInfo.put(PAYLOAD, payload);

            result = Node.builder()
                         .name(payload)
                         .uid(encodeSha1(payload))
                         .type(OUTPUT_DTO)
                         .properties(additionalInfo)
                         .build();
        }
        return result;
    }

    public static String encodeSha1(final String value) {
        return value == null ? null : new EncryptionUtils().encodeSha1(value);
    }

    public static DescriptionDTO extractDescription(final Description annotation) {
        if (annotation == null) {
            return null;
        }
        final DescriptionDTO.DescriptionDTOBuilder builder = DescriptionDTO.builder();
        builder.content(annotation.value())
               .example(annotation.example())
               .url(annotation.url());

        if (annotation.potentialErrors() != null && annotation.potentialErrors().length > 0) {
            List<PotentialErrorDTO> potentialErrors = new ArrayList<>(annotation.potentialErrors().length);
            for (PotentialError potentialErrorAnnotation : annotation.potentialErrors()) {
                PotentialErrorDTO potentialError = extractPotentialError(potentialErrorAnnotation);
                if (potentialError != null) {
                    potentialErrors.add(potentialError);
                }
            }
            builder.potentialErrors(potentialErrors);
        }

        return builder.build();
    }

    private static PotentialErrorDTO extractPotentialError(final PotentialError potentialErrorAnnotation) {
        final PotentialErrorDTO.PotentialErrorDTOBuilder builder = PotentialErrorDTO.builder();
        builder.throwsAs(potentialErrorAnnotation.throwsAs())
               .description(potentialErrorAnnotation.description())
               .example(potentialErrorAnnotation.example())
               .url(potentialErrorAnnotation.url())
               .errorCode(potentialErrorAnnotation.errorCode())
               .errorMessage(potentialErrorAnnotation.errorMessage())
               .errorMessageDetail(potentialErrorAnnotation.errorMessageDetail())
               .payload(potentialErrorAnnotation.payload())
               .httpStatus(potentialErrorAnnotation.httpStatus())
               .type(potentialErrorAnnotation.type());

        Class<?> errorCodeClass = null;
        if (potentialErrorAnnotation.errorCodeClass() != PotentialError.NONE.class) {
            errorCodeClass = safeLoadClass(potentialErrorAnnotation.errorCodeClass().getName(), CLASS_LOADER);
        }

        Object realErrorCode = null;
        if (errorCodeClass != null) {
            if (errorCodeClass.isEnum()) {
                realErrorCode = getEnumValue(potentialErrorAnnotation.errorCode(), errorCodeClass);
            }
            else {
                realErrorCode = getStaticFieldValue(potentialErrorAnnotation.errorCode(), errorCodeClass);
            }
        }

        if (realErrorCode != null) {
            applyIfNotNull(callGetterForField(ERROR_CODE, realErrorCode),
                           value -> builder.errorCode(String.valueOf(value)));

            applyIfNotNull(callGetterForField(MESSAGE, realErrorCode),
                           value -> builder.errorMessage(String.valueOf(value)));

            applyIfNotNull(callGetterForField(MESSAGE_DETAIL, realErrorCode),
                           value -> builder.errorMessageDetail(String.valueOf(value)));

            applyIfNotNull(callGetterForField(ERROR_TYPE, realErrorCode),
                           value -> builder.type(String.valueOf(value)));

            applyIfNotNull(callGetterForField(PAYLOAD1, realErrorCode),
                           value -> builder.type(String.valueOf(value)));

            applyIfNotNull(callGetterForField(STATUS_CODE, realErrorCode),
                           value -> builder.httpStatus(parseInt(value)));
        }

        return builder.build();
    }


    private static Object getEnumValue(final String errorCode, final Class<?> errorCodeClass) {
        Object             result = null;
        final List<Object> values = getEnumValues(errorCodeClass);
        for (Object value : values) {
            if (errorCode.equals(getFieldValue("name", value))) {
                result = getFieldValue(ERROR_CODE, value);
            }
        }
        return result;
    }


    public static List<Object> getEnumValues(final Class<?> enumClass) {
        final Object[] values = runSafe(() -> enumClass.getEnumConstants());
        return values == null ? new ArrayList<Object>() : Arrays.asList(values);
    }


    public static Object getStaticFieldValue(final String fieldName, final Class<?> clazz) {
        final Field currentField = getField(fieldName, clazz);
        currentField.setAccessible(true);
        return runSafe(() -> currentField.get(null));
    }

    private static Object getFieldValue(final String fieldName, final Object instance) {
        final Field currentField = getField(fieldName, instance);
        setAccessible(currentField);
        return runSafe(() -> currentField.get(instance));
    }


    public static Field getField(final String fieldName, final Object instance) {
        if (instance == null || fieldName == null) {
            return null;
        }
        return getField(fieldName, instance.getClass());
    }


    public static Field getField(final String fieldName, final Class<?> instanceClass) {
        if (instanceClass == null || fieldName == null) {
            return null;
        }
        Field             currentField = null;
        final List<Field> fields       = getAllFields(instanceClass);
        for (Field classField : fields) {
            if (classField.getName().equals(fieldName)) {
                currentField = classField;
                break;
            }
        }
        return currentField;
    }

    public static List<Field> getAllFields(final Class<?> instanceClasss) {
        List<Field> result = new ArrayList<>();
        if (instanceClasss == null || instanceClasss == Object.class) {
            return result;
        }
        result.addAll(Arrays.asList(instanceClasss.getDeclaredFields()));
        if (instanceClasss.getSuperclass() != null) {
            result.addAll(getAllFields(instanceClasss.getSuperclass()));
        }

        return result;
    }


    private static Object callGetterForField(final String field, final Object instance) {
        if (field == null || instance == null) {
            return null;
        }
        final List<Method> methods = loadAllMethods(instance.getClass());
        final Method getter = methods.stream()
                                     .filter(m -> m.getName().equalsIgnoreCase(GET + field)
                                             || m.getName().equalsIgnoreCase(IS + field))
                                     .findFirst()
                                     .orElse(null);
        setAccessible(getter);
        return runSafe(() -> getter.invoke(instance));
    }


    public static boolean isBasicType(final Class<?> currentClass) {
        return currentClass == null ? true :
               PRIMITIF_TYPES.contains(currentClass) || currentClass.getName().startsWith("java.lang", 0);
    }

    public static boolean isList(final Class<?> returnClass) {
        return Collection.class.isAssignableFrom(returnClass);
    }

}
