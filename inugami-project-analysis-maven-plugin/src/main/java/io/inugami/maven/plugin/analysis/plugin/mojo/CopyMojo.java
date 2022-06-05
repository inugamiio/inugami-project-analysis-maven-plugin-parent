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

import io.inugami.maven.plugin.analysis.api.models.Resource;
import io.inugami.maven.plugin.analysis.plugin.services.MavenArtifactResolver;
import io.inugami.maven.plugin.analysis.plugin.services.build.BasicBuildService;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.impl.ArtifactResolver;

import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Mojo(name = "copy", defaultPhase = LifecyclePhase.INSTALL)
public class CopyMojo extends AbstractMojo {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Parameter
    private List<Resource>      resources;
    @Parameter
    private List<String>        filteredExtensions;
    @Parameter
    private Map<String, String> properties;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter
    private boolean failSafe;

    @Parameter(defaultValue = "true")
    private boolean filtering;

    @Parameter(defaultValue = "true")
    private boolean mavenFiltering;

    @Component
    private ArtifactResolver artifactResolver;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repoSession;

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Map<String, String> currentProperties = MojoUtils.convertToMap(project);
        if (properties != null) {
            currentProperties.putAll(properties);
        }

        try {
            new BasicBuildService().copyResources(resources,
                                                  currentProperties,
                                                  filtering,
                                                  mavenFiltering,
                                                  new MavenArtifactResolver(repoSession, artifactResolver),
                                                  filteredExtensions);
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
