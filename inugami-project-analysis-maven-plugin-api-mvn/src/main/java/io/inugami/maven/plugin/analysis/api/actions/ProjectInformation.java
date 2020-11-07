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
import org.apache.maven.project.MavenProject;

public interface ProjectInformation extends NamedSpi {

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
}
