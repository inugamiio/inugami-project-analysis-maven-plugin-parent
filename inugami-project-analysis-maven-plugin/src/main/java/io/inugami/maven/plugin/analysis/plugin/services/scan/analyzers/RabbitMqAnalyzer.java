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

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.annotations.RabbitMqEvent;
import io.inugami.maven.plugin.analysis.annotations.RabbitMqHandlerInfo;
import io.inugami.maven.plugin.analysis.annotations.RabbitMqSender;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.tools.BuilderTools;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.*;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildMethodNode;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.*;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService.*;
import static org.springframework.core.annotation.AnnotatedElementUtils.hasAnnotation;

@Slf4j
public class RabbitMqAnalyzer implements ClassAnalyzer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final  String                   FEATURE_NAME              = "inugami.maven.plugin.analysis.analyzer.jms";
    public static final  String                   FEATURE                   = FEATURE_NAME + ".enable";
    private static final String                   SERVICE_TYPE              = "ServiceType";
    public static final  String                   SERVICE                   = "Service";
    private static final String                   RABBIT_MQ                 = "rabbitMq";
    private static final String                   EXPOSE                    = "EXPOSE";
    private static final String                   CONSUME                   = "CONSUME";
    private static final String                   SERVICE_TYPE_RELATIONSHIP = "SERVICE_TYPE";
    private static final SpringPropertiesAnalyzer PROPERTIES_ANALYZER       = new SpringPropertiesAnalyzer();

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return isEnable(FEATURE, context, true) && containsListenerOrSender(clazz);
    }

    private boolean containsListenerOrSender(final Class<?> clazz) {
        boolean result = hasAnnotation(clazz, RabbitListener.class);

        if (!result) {
            final List<Method> methods = loadAllMethods(clazz);

            for (final Method method : methods) {
                result = ReflectionService.hasAnnotation(method, RabbitListener.class, RabbitMqSender.class);
                if (result) {
                    break;
                }
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
        final ScanNeo4jResult result         = ScanNeo4jResult.builder().build();
        final RabbitListener  rabbitListener = clazz.getAnnotation(RabbitListener.class);

        final Node projectNode = buildNodeVersion(context.getProject());
        final Node serviceType = Node.builder().type(SERVICE_TYPE).uid(RABBIT_MQ).name(RABBIT_MQ).build();
        result.addNode(projectNode, serviceType);


        final List<Method> methods = loadAllMethods(clazz);
        for (final Method method : methods) {

            if (ReflectionService.hasAnnotation(method, RabbitListener.class, RabbitHandler.class)) {
                buildListenerNode(method, rabbitListener, (node, properties) -> {
                    result.addNode(node);
                    result.addNode(properties);

                    final Node methodNode = buildMethodNode(clazz, method);
                    result.addNode(methodNode);
                    result.addRelationship(
                            buildRelationships(node, CONSUME, projectNode, serviceType, methodNode, properties,
                                               "consume"));
                });
            }
            else if (ReflectionService.hasAnnotation(method, RabbitMqSender.class)) {
                buildSenderNode(method, (node, properties) -> {
                    result.addNode(node);
                    result.addNode(properties);

                    final Node methodNode = buildMethodNode(clazz, method);
                    result.addNode(methodNode);
                    result.addRelationship(
                            buildRelationships(node, CONSUME, projectNode, serviceType, methodNode, properties,
                                               "produce"));
                });
            }
        }

        return List.of(result);
    }

    // =========================================================================
    // BUILDERS SENDERS
    // =========================================================================
    private void buildSenderNode(final Method method,
                                 final BiConsumer<Node, List<Node>> onData) {

        final RabbitMqSender            sender         = method.getAnnotation(RabbitMqSender.class);
        final Map<String, Serializable> additionalInfo = new LinkedHashMap<>();
        final Set<Node>                 properties     = new HashSet<>();

        String uid = null;
        if (hasText(sender.id())) {
            uid = cleanValue(sender.id());
        }
        else {

            final String prefix = notNullValue(sender.echangeName()).isEmpty() ? notNullValue(
                    sender.queue()) : notNullValue(sender.echangeName());
            uid = String.join("_",
                              cleanValue(prefix),
                              cleanValue(notNullValue(sender.routingKey())));
        }

        final Parameter event = resolveRabbitEvent(method);
        if (event == null) {
            log.warn("no RabbitMqEvent define on method parameters : {}.{}", method.getDeclaringClass(),
                     method.getName());
        }
        final String eventPayload = event == null ? null : buildEventPayload(event);
        processIfNotNull(eventPayload, value -> additionalInfo.put("payload", value));

        final Node result = Node.builder()
                                .type(SERVICE)
                                .name(uid)
                                .uid(uid)
                                .properties(additionalInfo)
                                .build();


        buildPropNode(sender.id(), properties::add);
        buildPropNode(sender.echangeName(), properties::add);
        buildPropNode(sender.queue(), properties::add);
        buildPropNode(sender.routingKey(), properties::add);

        onData.accept(result, new ArrayList<>(properties));
    }

    private Parameter resolveRabbitEvent(final Method method) {
        Parameter event = null;
        for (final Parameter param : method.getParameters()) {
            if (ReflectionService.hasAnnotation(param, RabbitMqEvent.class)) {
                event = param;
                break;
            }
        }

        if (event == null) {
            for (final Parameter param : method.getParameters()) {
                if (param.getAnnotations() == null ||
                        param.getAnnotations().length == 0) {
                    event = param;
                    break;
                }
            }
        }
        return event;
    }

    private String notNullValue(final String value) {
        return value == null ? "" : value;
    }

    // =========================================================================
    // BUILDERS LISTENER
    // =========================================================================
    private void buildListenerNode(final Method method,
                                   final RabbitListener parentRabbitListener,
                                   final BiConsumer<Node, List<Node>> onData) {

        final List<Node>                properties       = new ArrayList<>();
        final Map<String, Serializable> additionalInfo   = new LinkedHashMap<>();
        final RabbitListener            methodAnnotation = method.getAnnotation(RabbitListener.class);
        final RabbitListener            rabbit           = methodAnnotation == null ? parentRabbitListener : methodAnnotation;
        final RabbitMqHandlerInfo       handlerInfo      = method.getAnnotation(RabbitMqHandlerInfo.class);

        if (handlerInfo == null && log.isDebugEnabled()) {
            log.warn("cann't resolve all rabbitMQ information without RabbitMqHandlerInfo on method : {},{}",
                     method.getDeclaringClass(), method.getName());
        }


        final Parameter event = resolveRabbitEvent(method);

        final String uid          = resolveUid(rabbit, handlerInfo);
        final String eventPayload = buildEventPayload(event);

        //@formatter:off
        processIfNotNull(eventPayload,                          value -> additionalInfo.put("payload", value));
        processIfNotEmpty(rabbit.id(),                          value -> additionalInfo.put("listenerId", value));
        processIfNotEmpty(rabbit.containerFactory(),            value -> additionalInfo.put("containerFactory", value));
        processIfNotNull(rabbit.queues(),                       value -> additionalInfo.put("queue", String.join(";", value)));
        processIfNotNull(rabbit.queuesToDeclare(),              value -> additionalInfo.put("queuesToDeclare", renderQueuesToDeclare(value)));
        processIfNotNull(rabbit.exclusive(),                    value -> additionalInfo.put("exclusive", value));
        processIfNotEmpty(rabbit.priority(),                    value -> additionalInfo.put("priority", value));
        processIfNotEmpty(rabbit.admin(),                       value -> additionalInfo.put("admin", value));
        processIfNotEmpty(rabbit.group(),                       value -> additionalInfo.put("group", value));
        processIfNotEmpty(rabbit.returnExceptions(),            value -> additionalInfo.put("returnExceptions", value));
        processIfNotEmpty(rabbit.errorHandler(),                value -> additionalInfo.put("errorHandler", value));
        processIfNotEmpty(rabbit.concurrency(),                 value -> additionalInfo.put("concurrency", value));
        processIfNotEmpty(rabbit.errorHandler(),                value -> additionalInfo.put("errorHandler", value));
        processIfNotEmpty(rabbit.concurrency(),                 value -> additionalInfo.put("concurrency", value));
        processIfNotEmpty(rabbit.autoStartup(),                 value -> additionalInfo.put("autoStartup", value));
        processIfNotEmpty(rabbit.executor(),                    value -> additionalInfo.put("executor", value));
        processIfNotEmpty(rabbit.ackMode(),                     value -> additionalInfo.put("ackMode", value));
        processIfNotEmpty(rabbit.replyPostProcessor(),          value -> additionalInfo.put("replyPostProcessor", value));
        processIfNotEmpty(rabbit.messageConverter(),            value -> additionalInfo.put("messageConverter", value));
        processIfNotEmpty(rabbit.replyContentType(),            value -> additionalInfo.put("replyContentType", value));
        processIfNotEmpty(rabbit.converterWinsContentType(),    value -> additionalInfo.put("converterWinsContentType", value));
        processIfNotNull(rabbit.bindings(),                     value -> additionalInfo.put("bindings", renderBinding(value)));
        //@formatter:on

        final Node result = Node.builder()
                                .type(SERVICE)
                                .name(uid)
                                .uid(uid)
                                .properties(additionalInfo)
                                .build();

        final List<Node> propertiesNodes = buildProperties(rabbit, handlerInfo);

        onData.accept(result, propertiesNodes);
    }


    private String resolveUid(final RabbitListener rabbitListener, final RabbitMqHandlerInfo handlerInfo) {
        final String routingKey = handlerInfo == null ? null : handlerInfo.routingKey();

        final Set<String> echanges    = new HashSet<>();
        final Set<String> queues      = new HashSet<>();
        final Set<String> routingKeys = new HashSet<>();

        if (rabbitListener != null) {
            for (final QueueBinding queueBinding : Optional.ofNullable(rabbitListener.bindings())
                                                           .orElse(new QueueBinding[]{})) {
                if (queueBinding.value() != null) {
                    processIfNotEmpty(cleanValue(queueBinding.value().value()), queues::add);
                }
                if (queueBinding.exchange() != null) {
                    processIfNotEmpty(cleanValue(queueBinding.exchange().value()), echanges::add);
                }

                for (final String key : Optional.ofNullable(queueBinding.key())
                                                .orElse(new String[]{})) {
                    processIfNotEmpty(cleanValue(key), routingKeys::add);
                }
            }
        }

        final String prefix = echanges.isEmpty() ? String.join(";", queues) : String.join(";", echanges);
        return String.join("_",
                           prefix,
                           routingKey == null ? String.join(";", routingKeys) : cleanValue(routingKey));
    }

    private List<Node> buildProperties(final RabbitListener rabbit, final RabbitMqHandlerInfo handlerInfo) {
        final Set<Node> result = new HashSet<>();


        if (handlerInfo != null) {
            buildPropNode(handlerInfo.routingKey(), result::add);
            buildPropNode(handlerInfo.typeId(), result::add);
        }

        if (rabbit != null) {
            buildPropNode(rabbit.id(), result::add);
            buildPropNode(rabbit.containerFactory(), result::add);
            buildPropNode(rabbit.queues(), result::addAll);
            buildPropNode(rabbit.priority(), result::add);
            buildPropNode(rabbit.admin(), result::add);
            buildPropNode(rabbit.group(), result::add);
            buildPropNode(rabbit.returnExceptions(), result::add);
            buildPropNode(rabbit.errorHandler(), result::add);
            buildPropNode(rabbit.concurrency(), result::add);
            buildPropNode(rabbit.autoStartup(), result::add);
            buildPropNode(rabbit.executor(), result::add);
            buildPropNode(rabbit.ackMode(), result::add);
            buildPropNode(rabbit.replyPostProcessor(), result::add);
            buildPropNode(rabbit.messageConverter(), result::add);
            buildPropNode(rabbit.replyContentType(), result::add);
            buildPropNode(rabbit.converterWinsContentType(), result::add);


            if (rabbit.queuesToDeclare() != null) {
                for (final Queue queue : rabbit.queuesToDeclare()) {
                    buildPropNode(queue, result::addAll);
                }
            }
            if (rabbit.bindings() != null) {
                for (final QueueBinding queue : rabbit.bindings()) {
                    buildPropNode(queue.value(), result::addAll);
                    buildPropNode(queue.key(), result::addAll);
                    buildPropNode(queue.ignoreDeclarationExceptions(), result::add);
                    buildPropNode(queue.arguments(), result::addAll);
                    buildPropNode(queue.declare(), result::add);
                    buildPropNode(queue.admins(), result::addAll);

                    if (queue.exchange() != null) {
                        buildPropNode(queue.exchange().value(), result::add);
                        buildPropNode(queue.exchange().name(), result::add);
                        buildPropNode(queue.exchange().type(), result::add);
                        buildPropNode(queue.exchange().durable(), result::add);
                        buildPropNode(queue.exchange().autoDelete(), result::add);
                        buildPropNode(queue.exchange().internal(), result::add);
                        buildPropNode(queue.exchange().ignoreDeclarationExceptions(), result::add);
                        buildPropNode(queue.exchange().delayed(), result::add);
                        buildPropNode(queue.exchange().arguments(), result::addAll);
                        buildPropNode(queue.exchange().declare(), result::add);
                        buildPropNode(queue.exchange().admins(), result::addAll);
                    }
                }
            }
        }

        return new ArrayList<>(result);
    }

    private void buildPropNode(final String value, final Consumer<Node> consumer) {
        if (hasText(value)) {
            processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, value), consumer::accept);
        }
    }

    private void buildPropNode(final String[] values, final Consumer<Collection<Node>> consumer) {
        if (values != null) {
            final Set<Node> result = new HashSet<>();
            for (final String value : values) {
                if (hasText(value)) {
                    processIfNotNull(PROPERTIES_ANALYZER.buildPropertyNode(String.class, value), result::add);
                }
            }
            consumer.accept(result);
        }
    }

    private void buildPropNode(final Queue queue, final Consumer<Collection<Node>> consumer) {
        if (queue != null) {
            final Set<Node> result = new HashSet<>();
            buildPropNode(queue.name(), result::add);
            buildPropNode(queue.value(), result::add);
            buildPropNode(queue.durable(), result::add);
            buildPropNode(queue.exclusive(), result::add);
            buildPropNode(queue.autoDelete(), result::add);
            buildPropNode(queue.ignoreDeclarationExceptions(), result::add);
            buildPropNode(queue.declare(), result::add);
            buildPropNode(queue.admins(), result::addAll);
            buildPropNode(queue.arguments(), result::addAll);
            consumer.accept(result);
        }
    }

    private void buildPropNode(final Argument[] values, final Consumer<Collection<Node>> consumer) {
        if (values != null) {
            final Set<Node> result = new HashSet<>();
            for (final Argument value : values) {
                buildPropNode(value.name(), result::add);
                buildPropNode(value.value(), result::add);
                buildPropNode(value.type(), result::add);
            }
            consumer.accept(result);
        }
    }


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
                               .properties(Map.of("linkType", useByType))
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


    private String buildEventPayload(final Parameter parameter) {
        JsonNode result = null;
        if (ReflectionService.hasAnnotation(parameter, RabbitMqEvent.class) && parameter
                .getAnnotation(RabbitMqEvent.class)
                .value() != RabbitMqEvent.None.class) {
            result = renderType(parameter.getAnnotation(RabbitMqEvent.class)
                                         .value(), null, null);
        }
        else {
            result = renderParameterType(parameter);
        }
        return result == null ? null : result.convertToJson();
    }


    // =========================================================================
    // RENDER
    // =========================================================================

    private String renderQueuesToDeclare(final Queue[] value) {
        final JsonBuilder json = new JsonBuilder();
        json.openList();

        final Iterator<Queue> iterator = Arrays.asList(value).iterator();
        while (iterator.hasNext()) {
            final Queue queue = iterator.next();
            json.openObject();
            json.addField("name").valueQuot(queue.name()).addSeparator();
            json.addField("value").valueQuot(queue.value()).addSeparator();
            json.addField("name").valueQuot(queue.name()).addSeparator();
            json.addField("durable").valueQuot(queue.durable()).addSeparator();
            json.addField("exclusive").valueQuot(queue.exclusive()).addSeparator();
            json.addField("autoDelete").valueQuot(queue.autoDelete()).addSeparator();
            json.addField("ignoreDeclarationExceptions").valueQuot(queue.ignoreDeclarationExceptions()).addSeparator();
            json.addField("declare").valueQuot(queue.declare()).addSeparator();

            json.addField("arguments");
            json.write(renderAgrs(queue.arguments()));
            json.addSeparator();

            json.addField("admins");
            json.write(renderStringArray(queue.admins()));
            json.closeObject();
            if (iterator.hasNext()) {
                json.addSeparator();
            }
        }

        json.closeList();
        return json.toString();
    }

    private String renderBinding(final QueueBinding[] value) {
        final JsonBuilder json = new JsonBuilder();
        json.openList();
        final Iterator<QueueBinding> iterator = Arrays.asList(value).iterator();
        while (iterator.hasNext()) {
            final QueueBinding queue = iterator.next();
            json.openObject();
            json.addField("queue").write(renderQueue(queue.value())).addSeparator();
            json.addField("key").write(renderStringArray(queue.key())).addSeparator();
            json.addField("exchange").write(renderEchange(queue.exchange())).addSeparator();
            json.addField("declare").valueQuot(queue.declare()).addSeparator();
            json.addField("ignoreDeclarationExceptions").valueQuot(queue.ignoreDeclarationExceptions()).addSeparator();
            json.addField("admins").write(renderStringArray(queue.admins()));
            json.closeObject();
            if (iterator.hasNext()) {
                json.addSeparator();
            }
        }

        json.closeList();
        return json.toString();
    }

    private String renderEchange(final Exchange exchange) {
        final JsonBuilder json = new JsonBuilder();
        if (exchange == null) {
            json.valueNull();
        }
        else {
            json.openObject();
            json.addField("name").valueQuot(hasText(exchange.name()) ? exchange.name() : exchange.value());
            json.addSeparator();
            json.addField("type").valueQuot(exchange.type()).addSeparator();
            json.addField("durable").valueQuot(exchange.durable()).addSeparator();
            json.addField("autoDelete").valueQuot(exchange.autoDelete()).addSeparator();
            json.addField("internal").valueQuot(exchange.internal()).addSeparator();
            json.addField("ignoreDeclarationExceptions").valueQuot(exchange.ignoreDeclarationExceptions());
            json.addSeparator();
            json.addField("delayed").valueQuot(exchange.delayed()).addSeparator();
            json.addField("declare").valueQuot(exchange.declare()).addSeparator();
            json.addField("arguments");
            json.write(renderAgrs(exchange.arguments()));
            json.addSeparator();
            json.addField("admins");
            json.write(renderStringArray(exchange.admins()));
            json.closeObject();
        }
        return json.toString();
    }

    private String renderQueue(final Queue value) {
        final JsonBuilder json = new JsonBuilder();
        json.openObject();
        json.addField("name").valueQuot(hasText(value.name()) ? value.name() : value.value()).addSeparator();
        json.addField("durable").valueQuot(value.durable()).addSeparator();
        json.addField("exclusive").valueQuot(value.exclusive()).addSeparator();
        json.addField("autoDelete").valueQuot(value.autoDelete()).addSeparator();
        json.addField("ignoreDeclarationExceptions").valueQuot(value.ignoreDeclarationExceptions()).addSeparator();
        json.addField("declare").valueQuot(value.declare()).addSeparator();

        json.addField("arguments");
        json.write(renderAgrs(value.arguments()));
        json.addSeparator();

        json.addField("admins");
        json.write(renderStringArray(value.admins()));
        json.closeObject();
        return json.toString();
    }


    private String renderAgrs(final Argument[] values) {
        final JsonBuilder json = new JsonBuilder();
        if (values == null || values.length == 0) {
            json.valueNull();
        }
        else {
            json.openList();
            final Iterator<Argument> iterator = Arrays.asList(values).iterator();
            while (iterator.hasNext()) {
                final Argument arg = iterator.next();
                json.openObject();
                json.addField("name").valueQuot(arg.name()).addSeparator();
                json.addField("value").valueQuot(arg.value()).addSeparator();
                json.addField("type").valueQuot(arg.type());
                json.closeObject();
                if (iterator.hasNext()) {
                    json.addSeparator();
                }
            }
            json.closeList();
        }
        return json.toString();
    }

    private String renderStringArray(final String[] values) {
        final JsonBuilder json = new JsonBuilder();
        if (values == null || values.length == 0) {
            json.valueNull();
        }
        else {
            json.openList();
            final Iterator<String> iterator = Arrays.asList(values).iterator();
            while (iterator.hasNext()) {
                final String admin = iterator.next();
                json.valueQuot(admin);
                if (iterator.hasNext()) {
                    json.addSeparator();
                }
            }
            json.closeList();
        }
        return json.toString();
    }


    protected String cleanValue(final String value) {
        String result = value;
        if (result != null && result.contains("${")) {
            result = result.replaceAll("[{$}]", "");
        }
        return result;
    }

}
