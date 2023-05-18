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
import io.inugami.maven.plugin.analysis.annotations.JmsEvent;
import io.inugami.maven.plugin.analysis.annotations.JmsSender;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.tools.BuilderTools;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import static io.inugami.api.exceptions.Asserts.assertNotEmpty;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildMethodNode;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.Constants.HAS_INPUT_DTO;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.*;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;

@Slf4j
public class JmsListenerAnalyzer implements ClassAnalyzer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final  String                   FEATURE_NAME              = "inugami.maven.plugin.analysis.analyzer.jms";
    public static final  String                   FEATURE                   = FEATURE_NAME + ".enable";
    public static final  String                   SERVICE                   = "Service";
    private static final String                   SERVICE_TYPE              = "ServiceType";
    private static final String                   SERVICE_TYPE_RELATIONSHIP = "SERVICE_TYPE";
    private static final String                   JMS                       = "JMS";
    private static final String                   EXPOSE                    = "EXPOSE";
    private static final String                   CONSUME                   = "CONSUME";
    private static final SpringPropertiesAnalyzer PROPERTIES_ANALYZER       = new SpringPropertiesAnalyzer();

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && containsListenerOrSender(clazz);
    }

    private boolean containsListenerOrSender(final Class<?> clazz) {
        boolean result = false;

        final List<Method> methods = loadAllMethods(clazz);

        for (final Method method : methods) {
            result = hasAnnotation(method, JmsListener.class, JmsSender.class);
            if (result) {
                break;
            }
        }
        return result;
    }

    // =========================================================================
    // ANALYSE
    // =========================================================================
    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        log.info("{} : {}", FEATURE_NAME, clazz);
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();

        final List<Method> methods = loadAllMethods(clazz);

        final Node projectNode = buildNodeVersion(context.getProject());
        final Node serviceType = Node.builder().type(SERVICE_TYPE).uid(JMS).name(JMS).build();
        result.addNode(projectNode, serviceType);

        for (final Method method : methods) {
            if (hasAnnotation(method, JmsListener.class)) {
                buildListenerNode(method, (node, properties) -> {
                    result.addNode(node);
                    result.addNode(properties);

                    final Node methodNode = buildMethodNode(clazz, method);
                    result.addNode(methodNode);
                    result.addRelationship(
                            buildRelationships(node, CONSUME, projectNode, serviceType, methodNode, properties,
                                               "consume"));

                    final List<Node> inputDto = ReflectionService.extractInputDto(method);
                    result.addNode(inputDto);
                    for (final Node input : inputDto) {
                        result.addRelationship(Relationship.builder()
                                                           .from(input.getUid())
                                                           .to(node.getUid())
                                                           .type(HAS_INPUT_DTO)
                                                           .build());
                    }
                    final Node outputDto = ReflectionService.extractOutputDto(method);
                    if (outputDto != null) {
                        result.addNode(outputDto);
                        result.addRelationship(Relationship.builder()
                                                           .from(outputDto.getUid())
                                                           .to(node.getUid())
                                                           .type(HAS_INPUT_DTO)
                                                           .build());
                    }
                });

            } else if (hasAnnotation(method, JmsSender.class)) {
                buildSenderNode(method, (node, properties) -> {
                    result.addNode(node);
                    result.addNode(properties);
                    final Node methodNode = buildMethodNode(clazz, method);
                    result.addNode(methodNode);
                    result.addRelationship(
                            buildRelationships(node, EXPOSE, projectNode, serviceType, methodNode, properties,
                                               "produce"));

                    final List<Node> inputDto = ReflectionService.extractInputDto(method);
                    result.addNode(inputDto);
                    for (final Node input : inputDto) {
                        result.addRelationship(Relationship.builder()
                                                           .from(input.getUid())
                                                           .to(node.getUid())
                                                           .type(HAS_INPUT_DTO)
                                                           .build());
                    }
                    final Node outputDto = ReflectionService.extractOutputDto(method);
                    if (outputDto != null) {
                        result.addNode(outputDto);
                        result.addRelationship(Relationship.builder()
                                                           .from(outputDto.getUid())
                                                           .to(node.getUid())
                                                           .type(HAS_INPUT_DTO)
                                                           .build());
                    }
                });
            }

        }

        return List.of(result);
    }

    // =========================================================================
    // BUILDER
    // =========================================================================
    private List<Relationship> buildRelationships(final Node jmsNode,
                                                  final String jmsNodeType,
                                                  final Node artifactNode,
                                                  final Node serviceNode,
                                                  final Node methodNode,
                                                  final List<Node> properties,
                                                  final String useByType) {

        final List<Relationship> result = new ArrayList<>();

        result.add(Relationship.builder()
                               .from(artifactNode.getUid())
                               .to(jmsNode.getUid())
                               .type(jmsNodeType)
                               .build());

        result.add(Relationship.builder()
                               .from(artifactNode.getUid())
                               .to(methodNode.getUid())
                               .type(BuilderTools.RELATION_HAS_METHOD)
                               .build());

        result.add(Relationship.builder()
                               .from(jmsNode.getUid())
                               .to(methodNode.getUid())
                               .type(BuilderTools.RELATION_USE_BY)
                               .properties(new LinkedHashMap<>(Map.of("linkType", useByType)))
                               .build());

        result.add(Relationship.builder()
                               .from(jmsNode.getUid())
                               .to(serviceNode.getUid())
                               .type(SERVICE_TYPE_RELATIONSHIP)
                               .build());

        if (properties != null) {
            for (final Node propertyNode : properties) {
                result.add(PROPERTIES_ANALYZER.buildRelationship(artifactNode, propertyNode));
            }
        }
        return result;
    }


    private void buildListenerNode(final Method method, final BiConsumer<Node, List<Node>> onData) {
        final List<Node>  properties = new ArrayList<>();
        final JmsListener listener   = method.getDeclaredAnnotation(JmsListener.class);

        String name = hasText(listener.id()) ? listener.id()
                : listener.destination();

        if (!hasText(name)) {
            name = method.getName();
        }

        Parameter event = null;
        for (final Parameter param : method.getParameters()) {
            if (hasAnnotation(param, JmsEvent.class) ||
                    param.getAnnotations() == null ||
                    param.getAnnotations().length == 0 ||
                    !hasAnnotation(param, Header.class, Headers.class)) {
                event = param;
                break;
            }
        }

        final String eventPayload = buildEventPayload(event);

        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        processIfNotEmpty(listener.destination(), value -> additionalInfo.put("destination", value));
        processIfNotEmpty(listener.containerFactory(), value -> additionalInfo.put("containerFactory", value));
        processIfNotEmpty(listener.subscription(), value -> additionalInfo.put("subscription", value));
        processIfNotEmpty(listener.selector(), value -> additionalInfo.put("selector", value));
        processIfNotEmpty(listener.concurrency(), value -> additionalInfo.put("concurrency", value));
        processIfNotEmpty(eventPayload, value -> additionalInfo.put("event", eventPayload));


        final Node result = Node.builder()
                                .type(SERVICE)
                                .name(name)
                                .uid(name)
                                .properties(additionalInfo)
                                .build();


        //@formatter:off
        processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, listener.id()), properties::add);
        processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, listener.containerFactory()), properties::add);
        processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, listener.destination()), properties::add);
        processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, listener.subscription()), properties::add);
        processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, listener.selector()), properties::add);
        processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, listener.concurrency()), properties::add);
        //@formatter:on

        onData.accept(result, properties);

    }

    private String buildEventPayload(final Parameter parameter) {
        JsonNode result = null;
        if (hasAnnotation(parameter, JmsEvent.class) && getAnnotation(parameter, JmsEvent.class).value() != JmsEvent.None.class) {
            result = renderType(getAnnotation(parameter, JmsEvent.class)
                                        .value(), null, null);
        } else {
            result = renderParameterType(parameter);
        }
        return result == null ? null : result.convertToJson();
    }


    private void buildSenderNode(final Method method, final BiConsumer<Node, List<Node>> onData) {
        final List<Node> properties = new ArrayList<>();
        final JmsSender  sender     = method.getDeclaredAnnotation(JmsSender.class);
        final String name = hasText(sender.id()) ? sender.id()
                : sender.destination();

        assertNotEmpty("id or destination must be define on JmsSender annotation on method " + method.getName(),
                       name);

        Parameter event = null;
        for (final Parameter param : method.getParameters()) {
            if (hasAnnotation(param, JmsEvent.class)) {
                event = param;
                break;
            }
        }

        if (event == null) {
            log.warn("no JmsEvent define on method parameters : {}.{}", method.getDeclaringClass(), method.getName());
        }

        final String eventPayload = event == null ? null : buildEventPayload(event);

        final LinkedHashMap<String, Serializable> additionalInfo = new LinkedHashMap<>();
        processIfNotEmpty(sender.destination(), value -> additionalInfo.put("destination", value));
        processIfNotEmpty(eventPayload, value -> additionalInfo.put("event", eventPayload));


        final Node result = Node.builder()
                                .type(SERVICE)
                                .name(name)
                                .uid(name)
                                .properties(additionalInfo)
                                .build();

        onData.accept(result, properties);
    }


}
