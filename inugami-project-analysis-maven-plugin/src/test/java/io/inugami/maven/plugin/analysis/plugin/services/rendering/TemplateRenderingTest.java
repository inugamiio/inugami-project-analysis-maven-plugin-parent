package io.inugami.maven.plugin.analysis.plugin.services.rendering;

import io.inugami.commons.test.UnitTestHelper;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TemplateRenderingTest {

    @Test
    public void rendering_nominal_shouldRendered() {
        TemplateRenderer renderer = new TemplateRenderer();
        String           template = UnitTestHelper.loadJsonReference("rendering/rendering.with.maven.filtering.txt");

        Map<String, String> mavenProperties = new LinkedHashMap<>();
        mavenProperties.put("project.groupId", "io.inugami");
        mavenProperties.put("project.artifactId", "inugami-project-analysis-maven-plugin");
        mavenProperties.put("project.version", "1.6.3-SNAPSHOT");

        Map<String, String> additionalProperties = new LinkedHashMap<>();
        additionalProperties.put("title", "hello the world");

        final String result = renderer.render("test1", template, mavenProperties, additionalProperties, true);

        UnitTestHelper.assertText(
                UnitTestHelper.loadJsonReference("rendering/rendering.with.maven.filtering.result.txt"),
                result);
    }

    @Test
    public void rendering_withoutMavenProperties_shouldRendered() {
        TemplateRenderer renderer = new TemplateRenderer();
        String           template = UnitTestHelper.loadJsonReference("rendering/rendering.with.maven.filtering.txt");

        Map<String, String> mavenProperties = new LinkedHashMap<>();
        mavenProperties.put("project.groupId", "io.inugami");
        mavenProperties.put("project.artifactId", "inugami-project-analysis-maven-plugin");
        mavenProperties.put("project.version", "1.6.3-SNAPSHOT");

        Map<String, String> additionalProperties = new LinkedHashMap<>();
        additionalProperties.put("title", "hello the world");

        final String result = renderer.render("test1", template, mavenProperties, additionalProperties, false);

        UnitTestHelper.assertText(
                UnitTestHelper.loadJsonReference("rendering/rendering.with.maven.filtering.withoutMaven.result.txt"),
                result);
    }

    @Test
    public void replaceMavenProperties_withMavenProperties_shouldReplace() {
        TemplateRenderer renderer = new TemplateRenderer();
        assertThat(renderer.replaceMavenProperties("hello ${foobar}")).isEqualTo("hello {{foobar}}");
    }
}