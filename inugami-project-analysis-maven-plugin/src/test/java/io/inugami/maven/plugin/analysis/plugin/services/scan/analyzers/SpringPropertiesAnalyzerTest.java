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
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static io.inugami.maven.plugin.analysis.plugin.services.scan.UnitTestHelper.assertText;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.UnitTestHelper.loadJsonReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SpringPropertiesAnalyzerTest {

    @Mock
    private ScanConext context;

    @Mock
    private MavenProject mavenProject;

    @BeforeEach
    public void setup() {
        lenient().when(mavenProject.getGroupId()).thenReturn("io.inugami.test");
        lenient().when(mavenProject.getArtifactId()).thenReturn("basic-artifact");
        lenient().when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        lenient().when(mavenProject.getPackaging()).thenReturn("jar");

        lenient().when(context.getProject()).thenReturn(mavenProject);
    }

    @Test
    void accept() {
        assertTrue(new SpringPropertiesAnalyzer().accept(null, null));
    }

    @Test
    void analyze_withBasicProperties_shouldFoundThem() {
        final List<JsonObject> result = new SpringPropertiesAnalyzer().analyze(Example.class, context);
        assertThat(result).isNotNull().size().isEqualTo(1);
        final ScanNeo4jResult nodesResult = (ScanNeo4jResult) result.get(0);
        assertText(nodesResult, loadJsonReference("services/scan/analyzers/properties_result.json"));
    }

    // =========================================================================
    // BASIC PROPERTIES
    // =========================================================================
    private static class ParentExample {
        @Value("${parent.value}")
        private String parentValue;

        public void action(@Value("${parent.timeout}") final long timeout) {

        }
    }

    private static class Example extends ParentExample {
        @Value("${current.name:#{someBean.value}")
        private String name;

        @Value("${current.enable:true}")
        private Boolean enable;

        @Value("${current.timeout:5000}")
        private long timeout;

        private final String provider;

        public Example(@Value("${current.provider}") final String provider) {
            this.provider = provider;
        }

        public void processing(@Value("${current.sender}") final String sender) {

        }
    }
}