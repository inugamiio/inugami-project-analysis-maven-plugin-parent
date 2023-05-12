package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.annotations.EntityDatabase;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.entities.IssueEntity;
import org.apache.maven.project.MavenProject;
import org.assertj.core.api.AssertionsForInterfaceTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelatif;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EntitiesAnalyzerTest {
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
        final EntitiesAnalyzer analyzer = new EntitiesAnalyzer();
        assertThat(analyzer.accept(EntitiesAnalyzerTest.User.class, context)).isTrue();
    }

    @Test
    public void analyze_withEntity_shouldFindIt() {
        final EntitiesAnalyzer analyzer = new EntitiesAnalyzer();
        final ScanNeo4jResult neo4jResult = extractResult(
                analyzer.analyze(EntitiesAnalyzerTest.User.class, context));
        assertTextRelatif(neo4jResult, "services/scan/analyzers/entity/entity_result.json");
    }

    @Test
    public void normalizeEntityName_withEntity_shouldNormalizeName() {
        final EntitiesAnalyzer analyzer = new EntitiesAnalyzer();

        assertThat(analyzer.normalizeEntityName("SQL_MAIN_DATABASE_APP_USER")).isEqualTo("SQL_MAIN_DATABASE_APP_USER");
        assertThat(analyzer.normalizeEntityName("my_entity")).isEqualTo("MY_ENTITY");
        assertThat(analyzer.normalizeEntityName("MyEntity")).isEqualTo("MY_ENTITY");
        assertThat(analyzer.normalizeEntityName("myEntity")).isEqualTo("MY_ENTITY");
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

    @Test
    public void analyze_withRecursiveEntities_shouldFindIt() {
        final EntitiesAnalyzer analyzer = new EntitiesAnalyzer();
        final ScanNeo4jResult neo4jResult = extractResult(
                analyzer.analyze(IssueEntity.class, context));

        neo4jResult.getNodes().sort((value, ref) -> value.getUid().compareTo(ref.getUid()));
        assertTextRelatif(neo4jResult,
                          "services/scan/analyzers/entity/analyze_withRecursiveEntities_shouldFindIt.json");
    }


    // =========================================================================
    // ENTITIES
    // =========================================================================
    @EntityDatabase("mainDatabase")
    @Table(name = "app_user")
    @Entity
    private static class User {

        @Id
        @GeneratedValue(strategy = GenerationType.TABLE)
        private Long uid;

        @NotNull
        private String username;

        private String firstName;
        private String lastName;

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "address_uid")
        private Address address;
    }

    @Entity
    private static class Address {

        @Id
        @GeneratedValue(strategy = GenerationType.TABLE)
        private Long uid;

        private String street;
        private String number;
        private String city;
        private String country;
        private String zipcode;
    }

}