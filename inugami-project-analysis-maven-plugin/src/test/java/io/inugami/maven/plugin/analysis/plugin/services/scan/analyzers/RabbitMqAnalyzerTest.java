package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.annotations.RabbitMqEvent;
import io.inugami.maven.plugin.analysis.annotations.RabbitMqHandlerInfo;
import io.inugami.maven.plugin.analysis.annotations.RabbitMqSender;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RabbitMqAnalyzerTest {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Mock
    private MavenProject mavenProject;
    @Mock
    private ScanConext   context;


    @BeforeEach
    public void setup() {
        lenient().when(mavenProject.getGroupId()).thenReturn("io.inugami.test");
        lenient().when(mavenProject.getArtifactId()).thenReturn("basic-artifact");
        lenient().when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        lenient().when(mavenProject.getPackaging()).thenReturn("jar");
        lenient().when(context.getProject()).thenReturn(mavenProject);

        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap(
                Map.ofEntries(Map.entry(JmsListenerAnalyzer.FEATURE, "true"))
        );
        lenient().when(context.getConfiguration()).thenReturn(configuration);
    }

    // =========================================================================
    // TESTS
    // =========================================================================
    @Test
    public void accept_withRabbitListenerOrSender_shouldAccept() {
        final RabbitMqAnalyzer analyzer = new RabbitMqAnalyzer();
        assertThat(analyzer.accept(RabbitMqAnalyzerTest.ClassListener.class, context)).isTrue();
        assertThat(analyzer.accept(RabbitMqAnalyzerTest.MethodListener.class, context)).isTrue();
    }

    @Test
    public void analyze_withRabbitListenerOrSender_shouldFindServices() {
        final RabbitMqAnalyzer analyzer = new RabbitMqAnalyzer();

        final ScanNeo4jResult neo4jResult = extractResult(
                analyzer.analyze(RabbitMqAnalyzerTest.ClassListener.class, context));
        assertTextRelative(neo4jResult, "services/scan/analyzers/rabbitMq/rabbitListener_result_01.json");
    }

    @Test
    public void analyze_withRabbitMethodListenerOrSender_shouldFindServices() {
        final RabbitMqAnalyzer analyzer = new RabbitMqAnalyzer();

        final ScanNeo4jResult neo4jResult = extractResult(
                analyzer.analyze(RabbitMqAnalyzerTest.MethodListener.class, context));
        assertTextRelative(neo4jResult, "services/scan/analyzers/rabbitMq/rabbitListener_result_02.json");
    }

    @Test
    public void analyze_withRabbitSender_shouldFindServices() {
        final RabbitMqAnalyzer analyzer = new RabbitMqAnalyzer();

        final ScanNeo4jResult neo4jResult = extractResult(
                analyzer.analyze(RabbitMqAnalyzerTest.MethodSender.class, context));
        assertTextRelative(neo4jResult, "services/scan/analyzers/rabbitMq/rabbitSender_result_01.json");
    }

    @Test
    public void analyze_withRabbitSenderOneParam_shouldFindServices() {
        final RabbitMqAnalyzer analyzer = new RabbitMqAnalyzer();

        final ScanNeo4jResult neo4jResult = extractResult(
                analyzer.analyze(RabbitMqAnalyzerTest.MethodSenderOneParam.class, context));
        assertTextRelative(neo4jResult, "services/scan/analyzers/rabbitMq/rabbitSender_result_02.json");
    }

    @Test
    public void cleanValue_withProperty_shouldCleanValue() {
        final RabbitMqAnalyzer analyzer = new RabbitMqAnalyzer();

        assertThat(analyzer.cleanValue("${my.property}")).isEqualTo("my.property");
        assertThat(analyzer.cleanValue("my.property")).isEqualTo("my.property");
        assertThat(analyzer.cleanValue(null)).isNull();
    }

    private ScanNeo4jResult extractResult(final List<JsonObject> result) {
        assertThat(result).isNotNull();
        AssertionsForInterfaceTypes.assertThat(result).size().isEqualTo(1);
        final ScanNeo4jResult neo4jResult = (ScanNeo4jResult) result.get(0);
        neo4jResult.getNodes().sort((value, ref) -> value.getUid().compareTo(ref.getUid()));

        neo4jResult.getRelationships().sort((value, ref) -> String.join("->", value.getFrom(), value.getTo()).compareTo(
                String.join("->", ref.getFrom(), ref.getTo())));
        return neo4jResult;
    }

    // =========================================================================
    // Rabbit Listener on class
    // =========================================================================
    @RabbitListener(
            id = "class-listener",
            containerFactory = "myContainerFactory",
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${events.user.queueName}", durable = "true", autoDelete = "false",
                            arguments = {
                                    @Argument(name = "x-dead-letter-exchange"),
                                    @Argument(name = "x-dead-letter-routing-key", value = "${events.user.dlqName}")
                            }
                    ),
                    exchange = @Exchange(value = "${events.exchangeName}", type = ExchangeTypes.TOPIC),
                    key = {
                            "${events.user.created.routingKey}",
                            "${events.user.authenticated.routingKey}",
                    }
            ),
            autoStartup = "true",
            errorHandler = "myListenerErrorHandler"
    )
    private static class ClassListener {
        @RabbitMqHandlerInfo(id = "user.created",
                routingKey = "${events.user.created.routingKey}",
                typeId = "${events.user.created.typeId}")
        @RabbitHandler
        public void onUpdate(final UserCreatedEvent event) {

        }

        @RabbitMqHandlerInfo(routingKey = "${events.user.authenticated.routingKey}",
                typeId = "${events.user.authenticated.typeId}")
        @RabbitHandler
        public void onAuthenticated(final UserAuthenticatedEvent event) {

        }
    }

    private static class MethodListener {
        @RabbitListener(
                id = "method-listener",
                containerFactory = "myContainerFactory",
                bindings = @QueueBinding(
                        value = @Queue(
                                value = "${events.method.user.queueName}", durable = "true", autoDelete = "false",
                                arguments = {
                                        @Argument(name = "x-dead-letter-exchange"),
                                        @Argument(name = "x-dead-letter-routing-key", value = "${events.method.user.dlqName}")
                                }
                        ),
                        exchange = @Exchange(value = "${events.method.exchangeName}", type = ExchangeTypes.TOPIC),
                        key = {
                                "${events.user.method.created.routingKey}"
                        }
                ),
                autoStartup = "true",
                errorHandler = "myListenerErrorHandler"
        )
        public void onUpdate(final UserCreatedEvent event) {

        }
    }

    private static class MethodSender {

        @RabbitMqSender(echangeName = "${events.exchangeName}",
                queue = "${events.method.user.queueName}",
                routingKey = "${events.user.method.created.routingKey}"
        )
        public void fireEvent(final String someValue, @RabbitMqEvent final UserCreatedEvent event) {

        }
    }


    private static class MethodSenderOneParam {
        @RabbitMqSender(echangeName = "${events.exchangeName}",
                queue = "${events.method.user.queueName}",
                routingKey = "${events.user.method.created.routingKey}"
        )
        public void fireEvent(@RabbitMqEvent final UserCreatedEvent event) {

        }
    }

    // =========================================================================
    // DTO
    // =========================================================================
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @ToString
    @Setter
    @Getter
    private static class UserCreatedEvent {
        private String        uid;
        private String        userName;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.sssZ")
        private LocalDateTime created;
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @ToString
    @Setter
    @Getter
    private static class UserAuthenticatedEvent {
        private String        userName;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.sssZ")
        private LocalDateTime date;
    }
}