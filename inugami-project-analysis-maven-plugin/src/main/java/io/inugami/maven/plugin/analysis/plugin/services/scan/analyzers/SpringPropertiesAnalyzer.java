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
package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import org.springframework.beans.factory.annotation.Value;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;

public class SpringPropertiesAnalyzer implements ClassAnalyzer {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final List<Class<?>> SHORT_NAME          = List.of(String.class,
                                                                      Short.class,
                                                                      Byte.class,
                                                                      Integer.class,
                                                                      Long.class,
                                                                      Float.class,
                                                                      Double.class,
                                                                      BigDecimal.class,
                                                                      Date.class,
                                                                      Calendar.class,
                                                                      LocalDate.class,
                                                                      LocalDateTime.class
                                                                     );
    private static final String         GROUP_PROPERTY      = "property";
    private static final String         GROUP_DEFAULT_VALUE = "defaultValue";
    private static final Pattern        VALUE_PATTERN       = Pattern
            .compile(
                    "(?:[$][{])(?<property>[^:}]+)(?:[:](?:#[{]){0,1}(?<defaultValue>[^}]+)(?:[}]){0,1}){0,1}(?:[}]){0,1}");
    public static final  String         USE_PROPERTY        = "USE_PROPERTY";

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return true;
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();

        final Set<Node> nodes = new HashSet<>();
        nodes.addAll(searchInFields(clazz));
        nodes.addAll(searchInConstructors(clazz));
        nodes.addAll(searchInMethods(clazz));

        if (!nodes.isEmpty()) {
            result.addNode(new ArrayList<>(nodes));

            final Node artifact = buildNodeVersion(context.getProject());
            for (final Node propertyNode : nodes) {
                result.addRelationship(Relationship.builder()
                                                   .from(artifact.getUid())
                                                   .to(propertyNode.getUid())
                                                   .type(USE_PROPERTY)
                                                   .build());
            }
        }


        return List.of(result);
    }


    // =========================================================================
    // PRIVATE
    // =========================================================================
    private Set<Node> searchInFields(final Class<?> clazz) {
        final Set<Node> result = new HashSet<>();

        final Set<Field> fields = loadAllFields(clazz);

        for (final Field field : fields) {
            final Node node = ifHasAnnotation(field, Value.class,
                                              annotation -> this.mapToNode(annotation, field.getType()));
            if (node != null) {
                result.add(node);
            }
        }
        return result;
    }


    private Set<Node> searchInConstructors(final Class<?> clazz) {
        final Set<Node>           result       = new HashSet<>();
        final Set<Constructor<?>> constructors = loadAllConstructors(clazz);

        for (final Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > 0) {
                for (final Parameter parameter : constructor.getParameters()) {
                    final Node node = ifHasAnnotation(parameter, Value.class,
                                                      annotation -> this.mapToNode(annotation, parameter.getType()));
                    if (node != null) {
                        result.add(node);
                    }
                }
            }
        }

        return result;
    }

    private Set<Node> searchInMethods(final Class<?> clazz) {
        final Set<Node>    result  = new HashSet<>();
        final List<Method> methods = loadAllMethods(clazz);

        for (final Method method : methods) {
            if (method.getParameterCount() > 0) {
                for (final Parameter parameter : method.getParameters()) {
                    final Node node = ifHasAnnotation(parameter, Value.class,
                                                      annotation -> this.mapToNode(annotation, parameter.getType()));
                    if (node != null) {
                        result.add(node);
                    }
                }
            }
        }
        return result;
    }


    // =========================================================================
    // MAPPER
    // =========================================================================
    private <A extends Annotation> Node mapToNode(final Value annotation,
                                                  final Class<?> type) {
        Node   result       = null;
        String property     = null;
        String defaultValue = null;

        final String value = annotation.value();
        if (value != null && value.contains("$")) {
            final Matcher matcher = VALUE_PATTERN.matcher(value);
            if (matcher.matches()) {
                property     = matcher.group(GROUP_PROPERTY);
                defaultValue = matcher.group(GROUP_DEFAULT_VALUE);
            }
        }

        if (property != null) {
            final Map<String, Serializable> additionalInfo = new HashMap<>();
            if (defaultValue == null) {
                additionalInfo.put("mandatory", Boolean.TRUE);
            }
            else {
                additionalInfo.put("defaultValue", defaultValue);
            }
            additionalInfo.put("propertyType", setShortName(type) ? type.getSimpleName() : type.getName());

            result = Node.builder()
                         .type("Property")
                         .name(property)
                         .uid(property)
                         .properties(additionalInfo)
                         .build();
        }
        return result;
    }

    private boolean setShortName(final Class<?> clazz) {
        return SHORT_NAME.contains(clazz);
    }
}
