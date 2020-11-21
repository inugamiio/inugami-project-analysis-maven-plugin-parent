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
import io.inugami.maven.plugin.analysis.api.tools.ProjectInformationTools;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.Record;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public interface ProjectInformation extends NamedSpi {
    String GROUP_ID    = ProjectInformationTools.GROUP_ID;
    String ARTIFACT_ID = ProjectInformationTools.ARTIFACT_ID;
    String TYPE        = ProjectInformationTools.TYPE;
    String VERSION     = ProjectInformationTools.VERSION;

    void process(MavenProject project, ConfigHandler<String, String> configuration);

    default void shutdown() {
    }

    default Gav convertMavenProjectToGav(final MavenProject project) {
        return ProjectInformationTools.convertMavenProjectToGav(project);
    }

    default String ifNull(final String value, final Supplier<String> handler) {
        return ProjectInformationTools.ifNull(value, handler);
    }


    default Node buildArtifactVersion(final MavenProject project, final ConfigHandler<String, String> configuration) {
        return ProjectInformationTools.buildArtifactVersion(project, configuration);
    }

    default Gav buildGav(final MavenProject project, final ConfigHandler<String, String> configuration) {
        return ProjectInformationTools.buildGav(project, configuration);
    }


    default Map<String, Collection<DataRow>> extractDataFromResultSet(final List<Record> resultSet,
                                                                      final BiConsumer<Map<String, Collection<DataRow>>, Map<String, Object>> consumer) {

        return ProjectInformationTools.extractDataFromResultSet(resultSet, consumer);
    }


}
