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
package io.inugami.maven.plugin.analysis.plugin.mojo;

import io.inugami.maven.plugin.analysis.api.models.PropertiesResources;
import io.inugami.maven.plugin.analysis.plugin.services.build.BasicBuildService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Mojo(name = "loadProperties", defaultPhase = LifecyclePhase.INSTALL)
public class LoadPropertiesMojo extends AbstractMojo {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Parameter
    private List<PropertiesResources> resources;

    @Parameter
    private Map<String, String> properties;

    @Parameter
    private boolean failSafe;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            new BasicBuildService().loadProperties(resources, properties, project.getProperties());
        }
        catch (Exception e) {
            if (failSafe) {
                log.error(e.getMessage(), e);
            }
            else {
                throw new MojoExecutionException(e.getMessage(), e);
            }

        }
    }

}
