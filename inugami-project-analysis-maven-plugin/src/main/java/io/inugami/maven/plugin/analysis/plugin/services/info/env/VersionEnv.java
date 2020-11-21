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
package io.inugami.maven.plugin.analysis.plugin.services.info.env;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import io.inugami.maven.plugin.analysis.api.tools.rendering.Neo4jRenderingUtils;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.types.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
public class VersionEnv implements ProjectInformation, QueryConfigurator {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String ENVIRONMENTS = "environments";

    // =========================================================================
    // QUERIES
    // =========================================================================
    private static final List<String> QUERIES                   = List.of(
            "META-INF/queries/search_deploy_artifact.cql"
                                                                         );
    public static final  String       PROJECT_DEPENDENCIES      = "Project dependencies";
    public static final  String       PROJECT_CONSUMED_SERVICES = "Project consumed services";
    public static final  String       DATE_UTC                  = "dateUtc";

    @Override
    public boolean accept(final String queryPath) {
        return QUERIES.contains(queryPath);
    }

    @Override
    public ConfigHandler<String, String> configure(final String queryPath, final Gav gav,
                                                   final ConfigHandler<String, String> configuration) {
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(gavToMap(gav));
        return config;
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final Neo4jDao dao       = new Neo4jDao(configuration);
        final String   queryPath = QUERIES.get(0);
        final String query = TemplateRendering.render(QueriesLoader.getQuery(queryPath),
                                                      configure(queryPath,
                                                                buildGav(project, configuration),
                                                                configuration));
        log.info("query:\n{}", query);
        final String result = renderResultSet(dao.search(query), this::buildModels);
        log.info("\n{}", result);
        dao.shutdown();
    }

    // =========================================================================
    // BUILDER
    // =========================================================================
    private void buildModels(final Map<String, Collection<DataRow>> data,
                             final Map<String, Object> record) {

        log.debug("record : {}", record);
        Collection<DataRow> lines = data.get(ENVIRONMENTS);
        if (lines == null) {
            lines = new ArrayList<>();
            data.put(ENVIRONMENTS, lines);
        }

        buildCurrentProjectEnv(data, record);
        buildProjectDependenciesEnv(data, record);
        buildProjectConsumersEnv(data, record);


    }


    private void buildCurrentProjectEnv(final Map<String, Collection<DataRow>> data, final Map<String, Object> record) {
        buildLine("version", "env", "deploy", data, record, ConsoleColors.CYAN);
    }

    private void buildProjectDependenciesEnv(final Map<String, Collection<DataRow>> data,
                                             final Map<String, Object> record) {
        buildLine("dep", "envDep", "deployDep", data, record);
    }

    private void buildProjectConsumersEnv(final Map<String, Collection<DataRow>> data,
                                          final Map<String, Object> record) {
        buildLine("depProducer", "envProducer", "deployProducer", data, record);
    }


    private void buildLine(final String artifactNodeName,
                           final String envNodeName,
                           final String relationshipName,
                           final Map<String, Collection<DataRow>> data,
                           final Map<String, Object> record) {
        buildLine(artifactNodeName,
                  envNodeName,
                  relationshipName,
                  data,
                  record,
                  null);
    }

    private void buildLine(final String artifactNodeName,
                           final String envNodeName,
                           final String relationshipName,
                           final Map<String, Collection<DataRow>> data,
                           final Map<String, Object> record,
                           final String color) {
        final List<DataRow> lines        = (List<DataRow>) data.get(ENVIRONMENTS);
        final String        artifactName = buildArtifactName(record.get(artifactNodeName));
        if (artifactName == null) {
            return;
        }

        final DataRow projectLine = new DataRow(artifactName);
        DataRow       currentRow  = null;
        final int     index       = lines.indexOf(projectLine);
        if (index >= 0) {
            currentRow = lines.get(index);
        }
        else {
            currentRow = projectLine;
            if(color!=null){
                currentRow.setRowColor(color);
            }
            currentRow.put("artifact", artifactName);
            lines.add(currentRow);
        }

        final String deployVersion = renderDeployVersion(record.get(artifactNodeName), record.get(relationshipName));
        final String envName       = Neo4jRenderingUtils.getNodeName(record.get(envNodeName));
        currentRow.put(envName, deployVersion);
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private String buildArtifactName(final Object nodeRaw) {
        String result = null;
        if (nodeRaw instanceof Node) {
            final Map<String, Object> properties = ((Node) nodeRaw).asMap();
            final String              groupId    = asString(properties.get(GROUP_ID));
            final String              artifactId = asString(properties.get(ARTIFACT_ID));
            result = new StringBuilder().append(groupId).append(':').append(artifactId).toString();

        }
        return result;
    }

    private String renderDeployVersion(final Object nodeRaw, final Object relationshipRaw) {
        String version = "";
        String date    = null;
        String dateUtc = null;
        if (nodeRaw instanceof Node) {
            final Map<String, Object> properties = ((Node) nodeRaw).asMap();
            version = asString(properties.get(VERSION));
        }

        if (relationshipRaw instanceof InternalRelationship) {
            final Map<String, Object> properties = ((InternalRelationship) relationshipRaw).asMap();
            dateUtc = asString(properties.get(DATE_UTC));
        }

        if (dateUtc != null) {
            date = dateUtc.split("[.]")[0];
        }

        final StringBuilder result = new StringBuilder();
        result.append(version);
        if (date != null) {
            result.append("(").append(date).append(")");
        }
        return result.toString();
    }

    private String asString(final Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
