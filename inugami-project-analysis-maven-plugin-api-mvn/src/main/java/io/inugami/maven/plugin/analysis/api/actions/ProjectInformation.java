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
package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.spi.NamedSpi;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.tools.ConsoleTools;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.Record;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;

public interface ProjectInformation extends NamedSpi {
    String GROUP_ID    = "groupId";
    String ARTIFACT_ID = "artifactId";
    String TYPE        = "type";
    String VERSION     = "version";

    void process(MavenProject project, ConfigHandler<String, String> configuration);

    default void shutdown() {
    }

    default Gav convertMavenProjectToGav(final MavenProject project) {
        return project == null ? null : Gav.builder()
                                           .groupId(project.getGroupId())
                                           .artifactId(project.getArtifactId())
                                           .version(project.getVersion())
                                           .type(project.getPackaging())
                                           .build();
    }

    default String ifNull(final String value, final Supplier<String> handler) {
        String result = value;
        if (result == null) {
            result = handler.get();
        }
        return result;
    }


    default Node buildArtifactVersion(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final boolean useMavenProject = Boolean.parseBoolean(configuration.grabOrDefault("useMavenProject", "false"));
        final Node artifactNode = useMavenProject ? buildNodeVersion(project)
                                                  : buildNodeVersion(buildGav(project, configuration));
        return artifactNode;
    }

    default Gav buildGav(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final boolean useMavenProject = Boolean.parseBoolean(configuration.grabOrDefault("useMavenProject", "false"));

        if (useMavenProject) {
            return Gav.builder()
                      .groupId(project.getGroupId())
                      .artifactId(project.getArtifactId())
                      .version(project.getVersion())
                      .type(project.getPackaging())
                      .build();
        }
        else {
            final String groupId = ifNull(configuration.get(GROUP_ID),
                                          () -> ConsoleTools.askQuestion("groupId ?", project.getGroupId()));

            final String artifactId = ifNull(configuration.get(ARTIFACT_ID),
                                             () -> ConsoleTools.askQuestion("artifactId ?", project.getArtifactId()));

            final String type = ifNull(configuration.get(TYPE),
                                       () -> ConsoleTools.askQuestion("type ?", project.getPackaging()));

            final String version = ifNull(configuration.get(VERSION),
                                          () -> ConsoleTools.askQuestion("version ?", project.getVersion()));
            return Gav.builder()
                      .groupId(groupId)
                      .artifactId(artifactId)
                      .version(version)
                      .type(type)
                      .build();
        }
    }


    default Map<String, Collection<DataRow>> extractDataFromResultSet(final List<Record> resultSet,
                                   final BiConsumer<Map<String, Collection<DataRow>>, Map<String, Object>> consumer) {

        final Map<String, Collection<DataRow>> data = new LinkedHashMap<>();
        if (resultSet != null || !resultSet.isEmpty()) {
            for (final Record record : resultSet) {
                if (record != null) {
                    final Map<String, Object> recordData = record.asMap();
                    consumer.accept(data, recordData);
                }
            }
        }
        return data;
    }


}
