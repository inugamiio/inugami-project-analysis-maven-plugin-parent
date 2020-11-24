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

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.spi.SpiLoader;
import io.inugami.commons.files.FilesUtils;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.PropertiesInitialization;
import io.inugami.maven.plugin.analysis.api.exceptions.ConfigurationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcher;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Mojo(name = "retrieveInformation")
public class MavenInfo extends AbstractMojo {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    @Component
    private SecDispatcher secDispatcher;

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        log.info("Retrieve information for project : {}:{}:{}:", project.getGroupId(), project.getArtifactId(),
                 project.getVersion());
        final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap();
        configuration.putAll(extractProperties(project.getProperties()));
        configuration.putAll(extractProperties(System.getProperties()));
        configuration.put("project.basedir", project.getBasedir().getAbsolutePath());
        configuration.put("project.build.directory", FilesUtils.buildFile(project.getBasedir(), "target")
                                                               .getAbsolutePath());
        configuration.put("interactive", isInteractive(configuration));

        final List<PropertiesInitialization> propertiesInitializers = SpiLoader.INSTANCE
                .loadSpiServicesByPriority(PropertiesInitialization.class);
        for (final PropertiesInitialization propsInitializer : propertiesInitializers) {
            propsInitializer.initialize(configuration, project, settings, secDispatcher);
        }


        ProjectInformation handler = null;

        final String action = configuration.get("action");

        if (action != null) {
            handler = SpiLoader.INSTANCE.loadSpiService(String.valueOf(action), ProjectInformation.class, true);
            try {
                handler.process(project, configuration);
            }
            catch (final Exception e) {
                if (e instanceof ConfigurationException) {
                    log.error(e.getMessage());
                }
                else {
                    log.error(e.getMessage(), e);
                }

                throw e;
            }
            finally {
                handler.shutdown();
            }
        }
        else {
            displayHelp();
        }
    }


    private String isInteractive(final ConfigHandler<String, String> configuration) {
        final Boolean result = configuration.containsKey("i") || configuration.containsKey("interactive");
        return String.valueOf(result);
    }

    private void displayHelp() throws MojoFailureException {
        final List<ProjectInformation> actions = SpiLoader.INSTANCE.loadSpiServicesByPriority(ProjectInformation.class);
        final JsonBuilder              help    = new JsonBuilder();
        help.line().write("No action define. Actions available : ").line();
        for (final ProjectInformation action : actions) {
            help.write("\t-Daction=").write(action.getName()).line();
        }

        throw new MojoFailureException(help.toString());
    }


    private Map<String, String> extractProperties(final Properties properties) {
        final Map<String, String> result = new LinkedHashMap<>();

        if (properties != null) {
            for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
        return result;
    }

}
