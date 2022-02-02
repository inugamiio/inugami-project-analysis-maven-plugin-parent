package io.inugami.maven.plugin.analysis.front.core.renderers;

import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestHelper.assertText;
import static io.inugami.commons.test.UnitTestHelper.loadJsonReference;

public class PluginsModuleRendererTest {
    @Test
    public void render_nominal() {
        final PluginsModuleRenderer renderer = new PluginsModuleRenderer();

        final String indexHtml = renderer.render();
        assertText(loadJsonReference("META-INF/resources/app/modules/plugins.module.ts"), indexHtml);
    }
}