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
package io.inugami.maven.plugin.analysis.plugin.services.scan.flyway;

import edu.emory.mathcs.backport.java.util.Collections;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.test.UnitTestHelper;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestHelper.assertText;
import static io.inugami.commons.test.UnitTestHelper.loadJsonReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class FlywayScanTest {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Mock
    private ScanConext context;


    @Mock
    private MavenProject mavenProject;

    // =========================================================================
    // SETUP
    // =========================================================================
    @BeforeEach
    public void setup() {
        lenient().when(mavenProject.getGroupId()).thenReturn("io.inugami.test");
        lenient().when(mavenProject.getArtifactId()).thenReturn("basic-artifact");
        lenient().when(mavenProject.getVersion()).thenReturn("1.0.0-SNAPSHOT");
        lenient().when(mavenProject.getPackaging()).thenReturn("jar");

        lenient().when(context.getProject()).thenReturn(mavenProject);

        final File dir = UnitTestHelper.buildTestFilePath("services/scan/flyway/mysql");
        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap(
                Map.ofEntries(
                        Map.entry(FlywayScan.FEATURE_ENABLED, "true"),
                        Map.entry(FlywayScan.SCRIPTS_PATHS, dir.getAbsolutePath()),
                        Map.entry(FlywayScan.DEFAULT_DB, "MySql")
                )
        );
        lenient().when(context.getConfiguration()).thenReturn(configuration);
    }

    // =========================================================================
    // API
    // =========================================================================
    @Test
    void scan_withScript_shouldCreateNodes() {
        final FlywayScan flywayScan = new FlywayScan();

        final List<JsonObject> result = flywayScan.scan(context);
        assertThat(result).size().isEqualTo(1);

        final ScanNeo4jResult nodes = (ScanNeo4jResult) result.get(0);
        Collections.sort(nodes.getNodes());
        Collections.sort(nodes.getRelationships());
        assertText(nodes, loadJsonReference("services/scan/flyway/scan_result.json"));
    }

    @Test
    void resolveExtension_nominal_shouldResolveExtension() {
        final FlywayScan flywayScan = new FlywayScan();

        assertThat(flywayScan.resolveExtension("v1_0_1_init_issue_table.sql")).isEqualTo("sql");
        assertThat(flywayScan.resolveExtension("v1.0.1.init_issue_table.json")).isEqualTo("json");
        assertThat(flywayScan.resolveExtension("v1_0_1_init_issue_table")).isNull();
    }


}
