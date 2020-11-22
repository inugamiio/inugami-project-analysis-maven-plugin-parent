package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.annotations.JmsEvent;
import io.inugami.maven.plugin.analysis.annotations.JmsSender;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.annotation.JmsListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelatif;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class JmsListenerAnalyzerTest {

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
    public void accept_withJmsListenerOrSender_shouldAccept() {
        final JmsListenerAnalyzer analyzer = new JmsListenerAnalyzer();
        assertThat(analyzer.accept(Listener.class, context)).isTrue();
    }

    @Test
    public void analyze_withJmsListenerOrSender_shouldFindServices() {
        final JmsListenerAnalyzer analyzer = new JmsListenerAnalyzer();

        final List<JsonObject> result = analyzer.analyze(Listener.class, context);

        assertThat(result).size().isEqualTo(1);
        final ScanNeo4jResult neo4jResult = (ScanNeo4jResult) result.get(0);
        neo4jResult.getNodes().sort((value, ref) -> compareNodes(value, ref));

        neo4jResult.getRelationships().sort((value, ref) -> sortRelationship(value, ref));
        assertTextRelatif(neo4jResult, "services/scan/analyzers/jmsListener_result.json");
    }


    @Test
    public void analyze_withJSenderOnly_shouldFindServices() {
        final JmsListenerAnalyzer analyzer = new JmsListenerAnalyzer();

        final List<JsonObject> result = analyzer.analyze(SenderOnly.class, context);

        assertThat(result).size().isEqualTo(1);
        final ScanNeo4jResult neo4jResult = (ScanNeo4jResult) result.get(0);
        neo4jResult.getNodes().sort((value, ref) -> compareNodes(value, ref));

        neo4jResult.getRelationships().sort((value, ref) -> sortRelationship(value, ref));
        assertTextRelatif(neo4jResult, "services/scan/analyzers/jmsSenderOnly_result.json");
    }

    private int compareNodes(final Node value, final Node ref) {
        return value.convertToJson().compareTo(ref.convertToJson());
    }

    private int sortRelationship(final Relationship value, final Relationship ref) {
        return value.convertToJson().compareTo(ref.convertToJson());
    }


    // =========================================================================
    // BASIC PROPERTIES
    // =========================================================================
    private static class Listener {
        @JmsListener(
                id = "create.user.queue",
                destination = "${my.activeMq.onUserCreated.queue}",
                containerFactory = "myContainerFactory")
        public void onUserCreate(final User user) {

        }

        @JmsListener(
                id = "onCreateComment",
                destination = "${my.activeMq.onCreated.queue}",
                subscription = "${my.activeMq.subscription}",
                selector = "${my.activeMq.selector}",
                containerFactory = "myContainerFactory")
        public void onCreate(final CommentEvent comment) {

        }

        @JmsSender(destination = "${my.activeMq.onUserCreated.queue}", id = "create.user.queue")
        public void sendCreateUser(final String someParameter, @JmsEvent final User user) {

        }
    }

    private static class SenderOnly {
        @JmsSender(destination = "${my.activeMq.onUserCreated.queue}", id = "create.user.queue")
        public void sendCreateUser(final String someParameter, @JmsEvent final User user) {

        }
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @ToString
    @Setter
    @Getter
    private static class CommentEvent {
        @EqualsAndHashCode.Include
        private Long               uid;
        private String             content;
        private User               user;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.sssZ")
        private LocalDateTime      created;
        private List<CommentEvent> responses;
    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @ToString
    @Setter
    @Getter
    private static class User {
        private String uid;
        private String userName;
    }
}