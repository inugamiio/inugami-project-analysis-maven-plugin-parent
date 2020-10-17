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

import io.inugami.api.exceptions.tools.StrategyException;
import io.inugami.api.loggers.Loggers;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.models.tools.Strategy;
import io.inugami.api.models.tools.StringPatternStrategy;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

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
    public static final  String                         PROPERTY_TYPE         = "propertyType";
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final List<Class<?>>                 SHORT_NAME            = List.of(String.class,
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
    private static final List<Strategy<String, String>> CONDITIONAL_BEAN_TYPE = List.of(
            new StringPatternStrategy("TRUE|true", "boolean"),
            new StringPatternStrategy("FALSE|false", "boolean"),
            new StringPatternStrategy("[0-9]+", "int"),
            new StringPatternStrategy("[0-9]+[.][0-9]+", "double"),
            new StringPatternStrategy("String"));

    private static final String  GROUP_PROPERTY           = "property";
    private static final String  GROUP_DEFAULT_VALUE      = "defaultValue";
    private static final Pattern VALUE_PATTERN            = Pattern
            .compile(
                    "(?:[$][{])(?<property>[^:}]+)(?:[:](?:#[{]){0,1}(?<defaultValue>[^}]+)(?:[}]){0,1}){0,1}(?:[}]){0,1}");
    public static final  String  USE_PROPERTY             = "USE_PROPERTY";
    public static final  String  PROPERTY                 = "Property";
    public static final  String  DEFAULT_VALUE            = "defaultValue";
    public static final  String  MANDATORY                = "mandatory";
    public static final  String  USE_FOR_CONDITIONAL_BEAN = "useForConditionalBean";
    public static final  String  MATCH_IF_MISSING         = "matchIfMissing";

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
        final ScanNeo4jResult result                = ScanNeo4jResult.builder().build();
        final Set<Node>       conditionalProperties = new HashSet<>();
        final Set<Node>       nodes                 = new HashSet<>();

        conditionalProperties.addAll(searchOnClass(clazz));

        searchOnFields(clazz,nodes);
        searchInConstructors(clazz,nodes);
        searchInMethods(clazz,nodes,conditionalProperties);

        fusion(nodes, conditionalProperties);
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
    private Set<Node> searchOnClass(final Class<?> clazz) {
        final Set<Node> result = new HashSet<>();
        final Set<Node> nodes = ifHasAnnotation(clazz, ConditionalOnProperty.class,
                                                annotation -> this.mapToNode(annotation));

        if (nodes != null) {
            result.addAll(nodes);
        }
        return result;
    }


    private void searchOnFields(final Class<?> clazz,
                                     final Set<Node> nodes) {

        final Set<Field> fields = loadAllFields(clazz);

        for (final Field field : fields) {
            final Node node = ifHasAnnotation(field, Value.class,
                                              annotation -> this.mapToNode(annotation, field.getType()));
            if (node != null) {
                nodes.add(node);
            }
        }
    }


    private void  searchInConstructors(final Class<?> clazz,
                                           final Set<Node> nodes) {
        final Set<Node>           result       = new HashSet<>();
        final Set<Constructor<?>> constructors = loadAllConstructors(clazz);

        for (final Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > 0) {
                for (final Parameter parameter : constructor.getParameters()) {
                    final Node node = ifHasAnnotation(parameter, Value.class,
                                                      annotation -> this.mapToNode(annotation, parameter.getType()));
                    if (node != null) {
                        nodes.add(node);
                    }
                }
            }
        }
    }

    private void searchInMethods(final Class<?> clazz,
                                      final Set<Node> nodes,
                                      final Set<Node> conditionalProperties) {
        final List<Method> methods           = loadAllMethods(clazz);

        for (final Method method : methods) {
            final Set<Node> nodesConditionals = ifHasAnnotation(method, ConditionalOnProperty.class,
                                                                annotation -> this.mapToNode(annotation));
            if (nodesConditionals != null) {
                conditionalProperties.addAll(nodesConditionals);
            }
            if (method.getParameterCount() > 0) {
                for (final Parameter parameter : method.getParameters()) {
                    final Node node = ifHasAnnotation(parameter, Value.class,
                                                      annotation -> this.mapToNode(annotation, parameter.getType()));
                    if (node != null) {
                        nodes.add(node);
                    }
                }
            }
        }
    }


    // =========================================================================
    // MAPPER
    // =========================================================================
    private <A extends Annotation> Set<Node> mapToNode(final ConditionalOnProperty annotation) {
        final Set<Node> result = new HashSet<>();
        final String[]  values = annotation.value();
        final String[]  names  = annotation.name();
        if (names != null && names.length > 0) {

            final String prefix = annotation.prefix() == null || "".equals(annotation.prefix().trim())
                                  ? null
                                  : annotation.prefix();
            final String  havingValue    = annotation.havingValue();
            final boolean matchIfMissing = annotation.matchIfMissing();

            for (final String name : names) {
                if (name != null && !"".equals(name.trim())) {

                    final String                    valueFull      = (prefix == null ? "" : prefix + ".") + name;
                    final Map<String, Serializable> additionalInfo = new HashMap<>();

                    additionalInfo.put(USE_FOR_CONDITIONAL_BEAN, Boolean.TRUE);
                    additionalInfo.put(MANDATORY, Boolean.FALSE);
                    additionalInfo.put(PROPERTY_TYPE, resolveType(havingValue));
                    additionalInfo.put(MATCH_IF_MISSING, matchIfMissing);
                    result.add(Node.builder()
                                   .type(PROPERTY)
                                   .name(valueFull)
                                   .uid(valueFull)
                                   .properties(additionalInfo)
                                   .build());
                }
            }
        }
        return result;
    }

    private String resolveType(final String havingValue) {
        String result = "undefine";

        if (havingValue != null) {
            for (final Strategy<String, String> strategy : CONDITIONAL_BEAN_TYPE) {
                if (strategy.accept(havingValue)) {
                    try {
                        result = strategy.process(havingValue);
                    }
                    catch (final StrategyException e) {
                        Loggers.DEBUG.debug(e.getMessage(), e);
                        //can't occurs
                    }
                    break;
                }
            }
        }
        return result;
    }

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
                additionalInfo.put(MANDATORY, Boolean.TRUE);
            }
            else {
                additionalInfo.put(MANDATORY, Boolean.FALSE);
                additionalInfo.put(DEFAULT_VALUE, defaultValue);
            }
            additionalInfo.put(PROPERTY_TYPE, setShortName(type) ? type.getSimpleName() : type.getName());


            result = Node.builder()
                         .type(PROPERTY)
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


    private void fusion(final Set<Node> nodes, final Set<Node> conditionalProperties) {
        if (nodes == null) {
            return;
        }
        if (conditionalProperties != null) {
            final List<Node> classicProperties = new ArrayList<>(nodes);
            for (final Node conditionalNode : conditionalProperties) {
                if (classicProperties.contains(conditionalNode)) {
                    final Node node = classicProperties
                            .get(classicProperties.indexOf(conditionalNode));
                    final Map<String, Serializable> additionalProperties = conditionalNode.getProperties();
                    for (final Map.Entry<String, Serializable> entry : conditionalNode.getProperties().entrySet()) {
                        if (!PROPERTY_TYPE.equals(entry.getKey())) {
                            node.getProperties().put(entry.getKey(), entry.getValue());
                        }
                    }
                }
                else {
                    nodes.add(conditionalNode);
                }
            }
        }

    }

}
