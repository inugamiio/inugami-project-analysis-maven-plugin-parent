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
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.resolvers.CyclicClassesResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.Constraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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

@SuppressWarnings({"java:S6397", "java:S6395","java:S6353"})
@Slf4j
public class SpringPropertiesAnalyzer implements ClassAnalyzer {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final  String                         FEATURE_NAME          = "inugami.maven.plugin.analysis.analyzer.properties";
    public static final  String                         FEATURE               = FEATURE_NAME + ".enable";
    private static final List<Class<?>>                 SHORT_NAME            = List.of(Boolean.class,
                                                                                        String.class,
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

    public static final String PROPERTY_TYPE = "propertyType";

    //@formatter:off
    private static final String                              GROUP_PROPERTY             = "property";
    private static final String                              GROUP_DEFAULT_VALUE        = "defaultValue";
    private static final Pattern                             VALUE_PATTERN              = Pattern.compile("(?:[$][{])(?<property>[^:}]+)(?:[:](?:#[{]){0,1}(?<defaultValue>[^}]+)(?:[}]){0,1}){0,1}(?:[}]){0,1}");
    public static final  String                              USE_PROPERTY               = "USE_PROPERTY";
    public static final  String                              PROPERTY                   = "Property";
    private static final String                              DEFAULT_VALUE              = "defaultValue";
    private static final String                              MANDATORY                  = "mandatory";
    private static final String                              USE_FOR_CONDITIONAL_BEAN   = "useForConditionalBean";
    private static final String                              MATCH_IF_MISSING           = "matchIfMissing";
    private static final String                              CONSTRAINT_TYPE            = "constraintType";
    private static final List<BeanPropertyTypeResolver>      TYPE_RESOLVERS             = SpiLoader.getInstance().loadSpiServicesByPriority(BeanPropertyTypeResolver.class);
    private static final CyclicClassesResolver               CYCLIC_CLASSES_RESOLVER    = (CyclicClassesResolver) TYPE_RESOLVERS.stream()
                                                                                                                                .filter( CyclicClassesResolver.class::isInstance)
                                                                                                                                .findFirst()
                                                                                                                                .get();
    private static final List<ConstraintInformationResolver> CONSTRAINTS_INFO_RESOLVERS = SpiLoader.getInstance().loadSpiServicesByPriority(ConstraintInformationResolver.class);
    //@formatter:on

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true);
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);
        final ScanNeo4jResult result                = ScanNeo4jResult.builder().build();
        final Set<Node>       conditionalProperties = new LinkedHashSet<>();
        final Set<Node>       nodes                 = new LinkedHashSet<>();

        conditionalProperties.addAll(searchOnClass(clazz));

        searchOnFields(clazz, nodes);
        searchInConstructors(clazz, nodes);
        searchInMethods(clazz, nodes, conditionalProperties);

        fusion(nodes, conditionalProperties);
        if (!nodes.isEmpty()) {
            final List<Node> currentNodes = new ArrayList<>(nodes);
            Collections.sort(currentNodes, (value, ref) -> value.getUid().compareTo(ref.getUid()));
            result.addNode(currentNodes);

            final Node artifact = buildNodeVersion(context.getProject());
            for (final Node propertyNode : nodes) {
                result.addRelationship(buildRelationship(artifact, propertyNode));
            }
        }

        return List.of(result);
    }

    public Relationship buildRelationship(final Node artifact, final Node propertyNode) {
        return Relationship.builder()
                           .from(artifact.getUid())
                           .to(propertyNode.getUid())
                           .type(USE_PROPERTY)
                           .build();
    }


    // =========================================================================
    // PRIVATE
    // =========================================================================
    private Set<Node> searchOnClass(final Class<?> clazz) {
        final Set<Node> result = new LinkedHashSet<>();
        final Set<Node> nodes = ifHasAnnotation(clazz, ConditionalOnProperty.class,this::mapToNode);


        final Set<Node> nodesBeanProperties = ifHasAnnotation(clazz, ConfigurationProperties.class,
                                                              annotation -> this
                                                                      .mapToBeanPropertyNode(annotation, clazz));

        if (nodes != null) {
            result.addAll(nodes);
        }

        if (nodesBeanProperties != null) {
            fusion(result, nodesBeanProperties);
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
                resolveConstraints(node, field.getDeclaredAnnotations());
                nodes.add(node);
            }
        }
    }


    private void searchInConstructors(final Class<?> clazz,
                                      final Set<Node> nodes) {
        final Set<Constructor<?>> constructors = loadAllConstructors(clazz);

        for (final Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() > 0) {
                for (final Parameter parameter : constructor.getParameters()) {
                    final Node node = ifHasAnnotation(parameter, Value.class,
                                                      annotation -> this.mapToNode(annotation, parameter.getType()));
                    if (node != null) {
                        resolveConstraints(node, parameter.getDeclaredAnnotations());
                        nodes.add(node);
                    }
                }
            }
        }
    }

    private void searchInMethods(final Class<?> clazz,
                                 final Set<Node> nodes,
                                 final Set<Node> conditionalProperties) {
        final List<Method> methods = loadAllMethods(clazz);

        for (final Method method : methods) {
            searchInMethod(nodes, conditionalProperties, method);
        }
    }

    private void searchInMethod(final Set<Node> nodes, final Set<Node> conditionalProperties, final Method method) {
        final Set<Node> nodesConditionals = ifHasAnnotation(method, ConditionalOnProperty.class,
                                                            this::mapToNode);
        if (nodesConditionals != null) {
            conditionalProperties.addAll(nodesConditionals);
        }
        if (method.getParameterCount() > 0) {
            for (final Parameter parameter : method.getParameters()) {
                final Node node = ifHasAnnotation(parameter, Value.class,
                                                  annotation -> this.mapToNode(annotation, parameter.getType()));
                if (node != null) {
                    resolveConstraints(node, parameter.getDeclaredAnnotations());
                    nodes.add(node);
                }
            }
        }
    }


    // =========================================================================
    // MAPPER
    // =========================================================================
    protected Set<Node> mapToNode(final ConditionalOnProperty annotation) {
        final Set<Node> result = new LinkedHashSet<>();
        final String[]  values = annotation.value();
        final String[]  names  = annotation.name();
        if ((names != null && names.length > 0) || (values != null && values.length > 0)) {

            final String prefix = annotation.prefix() == null || "".equals(annotation.prefix().trim())
                    ? null
                    : annotation.prefix();
            final String  havingValue    = annotation.havingValue();
            final boolean matchIfMissing = annotation.matchIfMissing();

            final Set<String> currentNames = new LinkedHashSet<>();
            if (values != null) {
                currentNames.addAll(Arrays.asList(values));
            }
            if (names != null) {
                currentNames.addAll(Arrays.asList(names));
            }

            createNodes(result, prefix, havingValue, matchIfMissing, currentNames);
        }
        return result;
    }

    protected void createNodes(final Set<Node> result, final String prefix, final String havingValue, final boolean matchIfMissing, final Set<String> currentNames) {
        for (final String name : currentNames) {
            if (stringNotEmpty(name)) {

                final String                              valueFull      = (prefix == null ? "" : prefix + ".") + name;
                final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();

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


    protected String resolveType(final String havingValue) {
        String result = "undefine";

        if (havingValue != null) {
            for (final Strategy<String, String> strategy : CONDITIONAL_BEAN_TYPE) {
                if (strategy.accept(havingValue)) {
                    try {
                        result = strategy.process(havingValue);
                    } catch (final StrategyException e) {
                        Loggers.DEBUG.debug(e.getMessage(), e);
                        //can't occurs
                    }
                    break;
                }
            }
        }
        return result;
    }

    protected Node mapToNode(final Value annotation,
                             final Class<?> type) {
        final String value = annotation.value();
        return buildPropertyNode(type, value);
    }

    public Node buildPropertyNode(final Class<?> type, final String value) {
        Node   result       = null;
        String property     = null;
        String defaultValue = null;
        if (value != null && value.contains("$")) {
            final Matcher matcher = VALUE_PATTERN.matcher(value);
            if (matcher.matches()) {
                property = matcher.group(GROUP_PROPERTY);
                defaultValue = matcher.group(GROUP_DEFAULT_VALUE);
            }
        }

        if (property != null) {
            final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
            if (defaultValue == null) {
                additionalInfo.put(MANDATORY, Boolean.TRUE);
            } else {
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

    // =========================================================================
    // BEAN PROPERTIES
    // =========================================================================
    protected Set<Node> mapToBeanPropertyNode(final ConfigurationProperties annotation, final Class<?> clazz) {

        final StringBuilder fullPrefix = new StringBuilder();
        if (stringNotEmpty(annotation.prefix())) {
            fullPrefix.append(annotation.prefix());
        }
        if (stringNotEmpty(annotation.value())) {
            if (fullPrefix.toString().isEmpty()) {
                fullPrefix.append(annotation.value());
            } else {
                fullPrefix.append('.');
                fullPrefix.append(annotation.value());
            }
        }
        final Set<Node> result = extractProperties(fullPrefix.toString(), clazz);

        return Optional.ofNullable(result).orElse(new LinkedHashSet<>());
    }

    public Set<Node> extractProperties(final String path, final Class<?> clazz) {
        final Set<Node>  result = new LinkedHashSet<>();
        final Set<Field> fields = loadAllFields(clazz);
        CYCLIC_CLASSES_RESOLVER.register(path, clazz);
        if (fields != null) {
            for (final Field field : fields) {
                result.addAll(extractFieldProperties(path, clazz, field));
            }
        }
        return result;
    }


    public Set<Node> extractFieldProperties(final String path, final Class<?> clazz,
                                            final Field field) {
        final Set<Node> result = new LinkedHashSet<>();
        if (field == null) {
            return result;
        }

        final String fullPath = CYCLIC_CLASSES_RESOLVER.buildFullPath(path, field);
        log.debug("extractFieldProperties : {} | {} | {}", fullPath, clazz == null ? null : clazz.getName(), field.getName());

        for (final BeanPropertyTypeResolver resolver : TYPE_RESOLVERS) {
            CYCLIC_CLASSES_RESOLVER.register(fullPath, field);
            final Set<Node> nodes = resolver.resolve(path, field, field.getType(), clazz, this);
            if (nodes != null) {
                result.addAll(nodes);
                break;
            }
        }

        return result;
    }

    public void addConstraints(final Field field, final Node node) {
        final Annotation[] annotations = field.getDeclaredAnnotations();
        resolveConstraints(node, annotations);
    }

    public void resolveConstraints(final Node node, final Annotation[] annotations) {
        if (annotations != null) {
            for (final Annotation annotation : annotations) {
                resolveContraintOnAnnotation(node, annotation);
            }
        }
    }

    protected static void resolveContraintOnAnnotation(final Node node, final Annotation annotation) {
        if (hasAnnotation(annotation.annotationType(), Constraint.class)) {

            if (annotation instanceof NotNull || annotation instanceof NotEmpty || annotation instanceof NotBlank) {
                node.getProperties().put(MANDATORY, Boolean.TRUE);
            }
            node.getProperties().put(CONSTRAINT_TYPE, annotation.annotationType().getName());

            for (final ConstraintInformationResolver constraintsInfoResolver : CONSTRAINTS_INFO_RESOLVERS) {
                if (constraintsInfoResolver.accept(annotation)) {
                    constraintsInfoResolver.appendInformation(node.getProperties(), annotation);
                }
            }
        }
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public boolean setShortName(final Class<?> clazz) {
        return SHORT_NAME.contains(clazz);
    }


    protected void fusion(final Set<Node> nodes, final Set<Node> conditionalProperties) {
        if (nodes == null) {
            return;
        }
        if (conditionalProperties != null) {
            final List<Node> classicProperties = new ArrayList<>(nodes);
            for (final Node conditionalNode : conditionalProperties) {
                fusionOnNode(nodes, classicProperties, conditionalNode);
            }
        }
    }

    protected static void fusionOnNode(final Set<Node> nodes, final List<Node> classicProperties, final Node conditionalNode) {
        if (classicProperties.contains(conditionalNode)) {
            final Node node = classicProperties.get(classicProperties.indexOf(conditionalNode));
            
            for (final Map.Entry<String, Serializable> entry : conditionalNode.getProperties().entrySet()) {
                if (!PROPERTY_TYPE.equals(entry.getKey())) {
                    node.getProperties().put(entry.getKey(), entry.getValue());
                }
            }
        } else {
            nodes.add(conditionalNode);
        }
    }

    protected boolean stringNotEmpty(final String value) {
        return value != null && !"".equals(value.trim());
    }
}
