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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.errors;

import io.inugami.api.exceptions.ErrorCode;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;

@Slf4j
public class ErrorCodeAnalyzer implements ClassAnalyzer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME          = "inugami.maven.plugin.analysis.analyzer.errorCode";
    public static final String FEATURE               = FEATURE_NAME + ".enable";
    public static final String ERROR_CODE_INTERFACE  = "inugami.maven.plugin.analysis.analyzer.errorCode.interface";
    public static final String ERROR_CODE_FIELD_NAME = "inugami.maven.plugin.analysis.analyzer.errorCode.fieldName";
    public static final String ERROR_CODE            = "ErrorCode";
    public static final String ERROR_TYPE            = "ErrorType";
    public static final String ACCESSOR_GET          = "get";
    public static final String ACCESSOR_IS           = "is";
    public static final String HAS_ERROR_CODE        = "HAS_ERROR_CODE";
    public static final String HAS_ERROR_TYPE        = "HAS_ERROR_TYPE";
    public static final String ERROR_CODE_PREFIX     = "errorCode_";
    public static final String ERROR_TYPE_PREFIX     = "errorType_";

    private Class<?>     errorCodeClass     = null;
    private List<Method> methods            = null;
    private String       errorCodeFieldName = null;

    // =========================================================================
    // ACCEPT
    // =========================================================================

    @Override
    public void initialize(final ScanConext context) {
        resolveErrorCodeClass(context);
    }

    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        boolean result = isEnable(FEATURE, context, true);
        if (result && errorCodeClass != null) {
            result = errorCodeClass.isAssignableFrom(clazz) && clazz.isEnum();
            if (!result) {
                for (final Field field : loadAllStaticFields(clazz)) {
                    result = errorCodeClass.isAssignableFrom(field.getType());
                }
            }
        }
        return result;
    }


    private synchronized Class<?> resolveErrorCodeClass(final ScanConext context) {
        if (errorCodeClass == null) {
            final String interfaceName = context.getConfiguration().get(ERROR_CODE_INTERFACE);
            errorCodeFieldName = context.getConfiguration().grabOrDefault(ERROR_CODE_FIELD_NAME, "errorCode");
            if (interfaceName == null) {
                errorCodeClass = ErrorCode.class;
            }
            else {
                try {
                    errorCodeClass = this.getClass().getClassLoader().loadClass(interfaceName);
                }
                catch (final ClassNotFoundException e) {
                    try {
                        errorCodeClass = context.getClassLoader().loadClass(interfaceName);
                    }
                    catch (final ClassNotFoundException ex) {
                        log.error(e.getMessage(), e);
                    }

                }
            }
        }

        if (errorCodeClass != null) {
            methods = loadAllMethods(errorCodeClass);
            for (final Method method : methods) {
                method.setAccessible(true);
            }
        }
        return errorCodeClass;
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);
        List<JsonObject> result = null;
        if (errorCodeClass != null) {
            result = processAnalyze(clazz, context);
        }
        return result;
    }


    private List<JsonObject> processAnalyze(final Class<?> clazz, final ScanConext context) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();

        List<Node> nodes = null;
        if (errorCodeClass.isAssignableFrom(clazz) && clazz.isEnum()) {
            nodes = scanErrorEnum(clazz);
        }
        else {
            nodes = scanErrorOnClass(clazz);
        }

        if (nodes != null && !nodes.isEmpty()) {
            final Node artifactNode = buildNodeVersion(context.getProject());
            result.addNode(artifactNode);

            final Set<Node> errorTypes = new HashSet<>();
            for (final Node node : nodes) {
                result.addNode(node);

                final Node errorType = buildErrorType(node);
                errorTypes.add(errorType);

                result.addRelationship(Relationship.builder()
                                                   .from(artifactNode.getUid())
                                                   .to(node.getUid())
                                                   .type(HAS_ERROR_CODE)
                                                   .build());

                result.addRelationship(Relationship.builder()
                                                   .from(node.getUid())
                                                   .to(errorType.getUid())
                                                   .type(HAS_ERROR_TYPE)
                                                   .build());
            }

            result.addNode(new ArrayList<>(errorTypes));
        }
        return List.of(result);
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private List<Node> scanErrorEnum(final Class<?> clazz) {
        final Set<Node> result = new HashSet<>();
        for (final Object enumConstants : clazz.getEnumConstants()) {
            if (errorCodeClass.isAssignableFrom(enumConstants.getClass())) {
                final Node node = buildNode(enumConstants);
                if (node != null) {
                    result.add(node);
                }
            }
        }

        return new ArrayList<>(result);
    }


    private List<Node> scanErrorOnClass(final Class<?> clazz) {
        final Set<Node>  result = new HashSet<>();
        final Set<Field> fields = loadAllFields(clazz);

        for (final Field field : fields) {
            Object data = null;
            Node   node = null;
            if (Modifier.isStatic(field.getModifiers()) && errorCodeClass.isAssignableFrom(field.getType())) {
                try {
                    data = field.get(null);
                }
                catch (final IllegalAccessException e) {
                    if (log.isDebugEnabled()) {
                        log.error("class : {}, field : {}", clazz, field);
                        log.error(e.getMessage(), e);
                    }
                }
            }
            if (data != null) {
                node = buildNode(data);
            }
            if (node != null) {
                result.add(node);
            }
        }

        return new ArrayList<>(result);
    }


    private Node buildNode(final Object instance) {
        final Node.NodeBuilder node = Node.builder();
        node.type(ERROR_CODE);

        final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();
        for (final Method method : methods) {
            final String key   = method.getName();
            Serializable value = null;

            if (method.getParameters().length == 0 && !errorCodeClass.isAssignableFrom(method.getReturnType())) {
                try {
                    final Object rawValue = method.invoke(instance);
                    if (rawValue instanceof Serializable) {
                        value = (Serializable) rawValue;
                    }
                }
                catch (final Exception e) {
                    if (log.isDebugEnabled()) {
                        log.error("instance : {} on retrieve value from method : {}", instance.getClass(),
                                  method.getName());
                        log.error(e.getMessage(), e);
                    }
                }
            }

            if (key != null && value != null) {
                properties.put(cleanAccessor(key), value);
            }
        }

        final String uid = properties.get(errorCodeFieldName) == null ? instance.toString() : String
                .valueOf(properties.get(errorCodeFieldName));
        node.uid(ERROR_CODE_PREFIX + uid);
        node.name(uid);
        node.properties(properties);
        return node.build();
    }


    private Node buildErrorType(final Node errorNode) {
        final String errorType = errorNode.getProperties().get("errorType") == null ? "technical" :
                                 String.valueOf(errorNode.getProperties().get("errorType"));


        return Node.builder()
                   .type(ERROR_TYPE)
                   .uid(ERROR_TYPE_PREFIX + errorType)
                   .name(errorType)
                   .build();
    }
    // =========================================================================
    // TOOLS
    // =========================================================================

    private Object buildInstance(final Class<?> clazz) {
        Constructor<?> defaultConstructor = null;


        try {
            defaultConstructor = clazz.getConstructor();
        }
        catch (final NoSuchMethodException e) {
            log.error("no default constructor found on class : {}", clazz.getName());
        }
        return null;
    }

    private String cleanAccessor(final String value) {
        String result = value;
        if (result != null) {
            if (result.startsWith(ACCESSOR_GET)) {
                result = result.substring(ACCESSOR_GET.length());
            }
            else if (result.startsWith(ACCESSOR_IS)) {
                result = result.substring(ACCESSOR_IS.length());
            }

            if (result.length() > 1) {
                final String firstChar = result.substring(0, 1);
                if (firstChar.equals(firstChar.toUpperCase())) {
                    result = firstChar.toLowerCase() + result.substring(1);
                }
            }
        }
        return result;
    }

}
