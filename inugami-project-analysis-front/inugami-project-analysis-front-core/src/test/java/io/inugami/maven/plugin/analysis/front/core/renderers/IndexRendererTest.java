package io.inugami.maven.plugin.analysis.front.core.renderers;

import org.junit.jupiter.api.Test;

import static io.inugami.commons.test.UnitTestHelper.assertText;
import static io.inugami.commons.test.UnitTestHelper.loadJsonReference;

class IndexRendererTest {

    @Test
    public void render_nominal(){
        IndexRenderer renderer = new IndexRenderer("/analysis");
        assertText(loadJsonReference("renderers/IndexRendererTest-render-nominal.html"),
                   renderer.render());
    }
}