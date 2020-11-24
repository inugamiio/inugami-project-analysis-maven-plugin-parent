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
package io.inugami.maven.plugin.analysis.plugin.services.info;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.exceptions.ConfigurationException;
import io.inugami.maven.plugin.analysis.api.models.QueryDefinition;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.util.List;

@Slf4j
public class QueryDisplay implements ProjectInformation {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final String queryName = configuration.get("query");

        if (queryName == null) {
            final JsonBuilder help = new JsonBuilder();
            help.line().write("No selected define. Queries available : ").line();

            for (final QueryDefinition query : QueriesLoader.loadQueries()) {
                help.write("\t-Dquery=").write(query.getName()).line();
                help.write("\t\tdescription :").write(query.getDescription()).line();
                if (query.getParameters() != null) {
                    help.write("\t\tparameters :").line();
                    for (final String param : query.getParameters()) {
                        help.write("\t\t\t").write(param).line();
                    }
                }
                help.line();
            }
            throw new ConfigurationException(help.toString());
        }
        else {
            final QueryDefinition queryDef = QueriesLoader.getQueryByName(queryName);

            final List<QueryConfigurator> configurators = SpiLoader.INSTANCE
                    .loadSpiServicesByPriority(QueryConfigurator.class);
            final QueryConfigurator configurator = configurators.stream()
                                                                .filter(item -> item.accept(queryDef.getPath()))
                                                                .findFirst()
                                                                .orElse(null);

            ConfigHandler<String, String> currentConfiguration = configuration;
            if (configurator != null) {
                currentConfiguration = configurator
                        .configure(queryDef.getPath(), convertMavenProjectToGav(project), configuration);
            }
            final String query = TemplateRendering.render(queryDef, currentConfiguration);

            log.info("selected query : \n{}", query);
        }
    }


}
