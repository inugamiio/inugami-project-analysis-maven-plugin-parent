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
package io.inugami.maven.plugin.analysis.api.tools;

import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.Node;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.*;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.extractTag;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
@SuppressWarnings({"java:S5838","java:S5838","java:S5853"})
@ExtendWith(MockitoExtension.class)
class BuilderToolsTest {

    @Mock
    private MavenProject mavenProject;
    private Gav          gav;

    @BeforeEach
    public void setup() {
        gav = Gav.builder()
                 .groupId("io.inugami.test")
                 .artifactId("basic-artifact")
                 .version("1.0.0-SNAPSHOT")
                 .type("jar")
                 .build();

        lenient().when(mavenProject.getGroupId()).thenReturn(gav.getGroupId());
        lenient().when(mavenProject.getArtifactId()).thenReturn(gav.getArtifactId());
        lenient().when(mavenProject.getVersion()).thenReturn(gav.getVersion());
        lenient().when(mavenProject.getPackaging()).thenReturn(gav.getType());
    }


    @Test
    void buildNodeVersion_withGav_shouldCreateNode() {
        final Node nodeGav     = buildNodeVersion(gav);
        final Node nodeProject = buildNodeVersion(mavenProject);
        assertThat(nodeGav.getUid()).isEqualTo(nodeProject.getUid());
        assertThat(nodeGav.getUid()).isEqualTo("io.inugami.test:basic-artifact:1.0.0-SNAPSHOT:jar");
    }

    @Test
    void buildGavNodeArtifact_withGav_shouldBuildNode() {
        final Node nodeGav     = buildGavNodeArtifact(gav);
        final Node nodeProject = buildArtifactNode(mavenProject);
        assertThat(nodeGav.getUid()).isEqualTo(nodeProject.getUid());
        assertThat(nodeGav.getUid()).isEqualTo("io.inugami.test:basic-artifact:jar");
    }

    @Test
    void testExtractMajorVersion() {
        assertThat(extractMajorVersion("3.17.1")).isEqualTo(3);
        assertThat(extractMajorVersion("3-FINAL")).isEqualTo(3);
    }

    @Test
    void testExtractMinorVersion() {
        assertThat(extractMinorVersion("3.17.1")).isEqualTo(17);
        assertThat(extractMinorVersion("3-FINAL")).isEqualTo(0);
    }

    @Test
    void testExtractPatchVersion() {
        assertThat(extractPatchVersion("3.17.1")).isEqualTo(1);
        assertThat(extractPatchVersion("3.17")).isEqualTo(0);
    }


    @Test
    void testExtractTag() {
        assertThat(extractTag("3.17.1")).isEqualTo("");
        assertThat(extractTag("2.2.4.RELEASE")).isEqualTo("RELEASE");
        assertThat(extractTag("2.2.4.RC3")).isEqualTo("RC3");
    }

    @Test
    void testBuildMoreInformation() {
        final String json = "{\n" +
                "  \"projectType\": \"microservice\",\n" +
                "  \"jdk\": \"11\",\n" +
                "  \"like\": 3,\n" +
                "  \"forPrd\": true,\n" +
                "  \"sub\": {\n" +
                "    \"keyA\": true\n" +
                "  },\n" +
                "  \"children\": [\n" +
                "    {\n" +
                "      \"name\": \"aaa\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"name\": \"bbb\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";


        final Map<String, Serializable> result = BuilderTools.buildMoreInformation(json);
        assertThat(result).isNotEmpty();
        assertThat(result).hasSize(7);

        assertThat(result.get("projectType")).isEqualTo("microservice");
        assertThat(result.get("jdk")).isEqualTo("11");
        assertThat(result.get("like")).isEqualTo(3);
        assertThat(result.get("forPrd")).isEqualTo(Boolean.TRUE);
        assertThat(result.get("sub_keyA")).isEqualTo(Boolean.TRUE);
        assertThat(result.get("children_0_name")).isEqualTo("aaa");
        assertThat(result.get("children_1_name")).isEqualTo("bbb");

    }
}