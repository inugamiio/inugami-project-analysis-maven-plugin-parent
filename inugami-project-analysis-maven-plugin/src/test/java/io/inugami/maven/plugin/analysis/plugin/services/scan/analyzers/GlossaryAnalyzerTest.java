package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import io.inugami.api.documentation.Glossaries;
import io.inugami.api.documentation.Glossary;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertTextRelative;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class GlossaryAnalyzerTest {
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
    // ANALYSE
    // =========================================================================
    @Test
    public void analyze() {
        final GlossaryAnalyzer analyzer = new GlossaryAnalyzer();

        final List<JsonObject> result = analyzer.analyze(Person.class, context);
        assertTextRelative(result, "services/scan/analyzers/glossaryAnalyzer/analyze.json");
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    @Glossaries({
            @Glossary("Personne"),
            @Glossary(value = "Personne", language = "fr")
    })
    private static class Person {
        @Glossary(value = "adresse", language = "fr", description = "lorem ipsum")
        private String address;
    }
}