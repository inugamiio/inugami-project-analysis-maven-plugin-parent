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
package io.inugami.maven.plugin.analysis.plugin.services.info.error;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;

import java.io.Serializable;
import java.util.*;

import static io.inugami.maven.plugin.analysis.api.tools.rendering.Neo4jRenderingUtils.*;

@Slf4j
public class ErrorDisplay implements ProjectInformation, QueryConfigurator {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final  String       ERROR_CODE  = "errorCode";
    public static final  String       PAYLOAD     = "payload";
    private static final List<String> QUERIES     = List.of(
            "META-INF/queries/search_errors.cql"
                                                           );
    public static final  String       SHORT_NAME  = "shortName";
    public static final  String       ERROR_TYPE  = "errorType";
    public static final  String       STATUS_CODE = "statusCode";

    // =========================================================================
    // QUERIES
    // =========================================================================
    @Override
    public boolean accept(final String queryPath) {
        return QUERIES.contains(queryPath);
    }

    @Override
    public ConfigHandler<String, String> configure(final String queryPath, final Gav gav,
                                                   final ConfigHandler<String, String> configuration) {
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(Map.ofEntries(
                Map.entry("groupId", gav.getGroupId()),
                Map.entry("artifactId", gav.getArtifactId()),
                Map.entry("version", gav.getVersion())
                                   ));
        return config;
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final Neo4jDao dao       = new Neo4jDao(configuration);
        final Gav      gav       = convertMavenProjectToGav(project);
        final String   queryPath = QUERIES.get(0);
        final String query = TemplateRendering.render(QueriesLoader.getQuery(queryPath),
                                                      configure(queryPath,
                                                                gav,
                                                                configuration));
        log.info("query:\n{}", query);
        final List<Record> resultSet = dao.search(query);

        final Map<String, Collection<DataRow>> data = new LinkedHashMap<>();
        if (resultSet != null || !resultSet.isEmpty()) {
            buildModels(data, resultSet);
        }
        log.info("\n{}", rendering(data,configuration,"errorCodes"));

        dao.shutdown();
    }


    // =========================================================================
    // BUILDER
    // =========================================================================
    private void buildModels(final Map<String, Collection<DataRow>> data,
                             final List<Record> resultSet) {

        Set<String> knowKeys = null;
        for (final Record record : resultSet) {
            final Map<String, Object> recordData = record != null ? record.asMap() : null;
            if (recordData == null) {
                continue;
            }
            final Node dependency = (Node) recordData.get("dependency");
            final Node error      = (Node) recordData.get("error");

            final String dependencyName = getNodeName(dependency);

            Collection<DataRow> savedDependency = data.get(dependencyName);
            if (savedDependency == null) {
                savedDependency = new HashSet<>();
                data.put(dependencyName, savedDependency);
            }

            if (error != null) {
                final DataRow dataRow = new DataRow();

                final String type = retrieve(ERROR_TYPE, error);
                if (type != null) {
                    switch (type.toUpperCase()) {
                        case "TECHNICAL":
                            dataRow.setRowColor(ConsoleColors.RED);
                            break;
                        case "SECURITY":
                            dataRow.setRowColor(ConsoleColors.YELLOW);
                            break;
                        case "CONFIG":
                        case "CONFIGURATION":
                            dataRow.setRowColor(ConsoleColors.PURPLE);
                            break;
                    }
                }

                final Map<String, Object> properties = error.asMap();
                knowKeys = orderErrorProperties(properties.keySet(), knowKeys);

                for (final String property : knowKeys) {
                    final Object propertyValueRaw = properties.get(property);
                    Serializable propertyValue    = null;
                    if (propertyValueRaw instanceof Serializable) {
                        propertyValue = (Serializable) propertyValueRaw;
                    }
                    else if (propertyValueRaw == null) {
                        propertyValue = "";
                    }
                    else {
                        propertyValue = String.valueOf(propertyValueRaw);
                    }
                    dataRow.put(property, propertyValue);

                }
                final String name = getNodeName(error);
                dataRow.setUid(name == null ? "undefine" : name);
                savedDependency.add(dataRow);

            }


        }

    }

    private Set<String> orderErrorProperties(final Set<String> keys, final Set<String> knowKeys) {
        final Set<String> result    = new LinkedHashSet<>();
        final Set<String> firstPass = new LinkedHashSet<>();
        final Set<String> values    = new LinkedHashSet<>();

        if (keys != null) {
            values.addAll(keys);
        }

        if (!values.isEmpty()) {
            if (values.contains(SHORT_NAME)) {
                firstPass.add(SHORT_NAME);
                values.remove(SHORT_NAME);
            }
            if (values.contains(ERROR_TYPE)) {
                firstPass.add(ERROR_TYPE);
                values.remove(ERROR_TYPE);
            }
            if (values.contains(STATUS_CODE)) {
                firstPass.add(STATUS_CODE);
                values.remove(STATUS_CODE);
            }
            if (values.contains(ERROR_CODE)) {
                values.remove(ERROR_CODE);
            }
            if (values.contains(PAYLOAD)) {
                values.remove(PAYLOAD);
            }
            for (final String key : values) {
                firstPass.add(key);
            }
        }

        if (knowKeys != null) {
            result.addAll(knowKeys);
            for (final String key : firstPass) {
                if (!knowKeys.contains(key)) {
                    result.add(key);
                }
            }
        }
        else {
            result.addAll(firstPass);
        }
        return result;
    }


}
