package io.inugami.maven.plugin.analysis.front.core.renderers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestHelper.assertText;
import static io.inugami.commons.test.UnitTestHelper.loadJsonReference;

@Slf4j
class IndexRendererTest {

    @Test
    public void render_nominal() {
        final IndexRenderer renderer = new IndexRenderer("/analysis");

        final String indexHtml = renderer.render();
        assertText(loadJsonReference("META-INF/resources/index.html"), indexHtml);
    }
}