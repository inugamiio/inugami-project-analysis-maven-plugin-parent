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

import java.io.Serializable;
import java.util.*;

import static io.inugami.maven.plugin.analysis.api.tools.rendering.Neo4jRenderingUtils.ifPropertyNotNull;

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
            "META-INF/queries/search_deploy_artifact.cql",
            "META-INF/queries/search_missing_service.cql"
                                                                         );
    public static final  String       PROJECT_DEPENDENCIES      = "Project dependencies";
    public static final  String       PROJECT_CONSUMED_SERVICES = "Project consumed services";
    public static final  String       DATE_UTC                  = "dateUtc";
    public static final  String       ARTIFACT                  = "artifact";
    public static final  String       EMPTY                     = "";
    public static final  String       MISSING_SERVICES          = "Missing services";

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
        final Neo4jDao dao = new Neo4jDao(configuration);

        final Gav gav = buildGav(project, configuration);
        final String query = TemplateRendering.render(QueriesLoader.getQuery(QUERIES.get(0)),
                                                      configure(QUERIES.get(0),
                                                                gav,
                                                                configuration));
        log.info("query:\n{}", query);
        final Map<String, Long> envs = new LinkedHashMap<>();
        final Map<String, Collection<DataRow>> firstPass = extractDataFromResultSet(dao.search(query),
                                                                                    (data, record) -> this
                                                                                            .buildModels(data, record,
                                                                                                         envs));


        final Map<String, Collection<DataRow>> data = sortData(firstPass, envs, extractProjectEnvs(firstPass, gav));

        final String queryMissingService = TemplateRendering.render(QueriesLoader.getQuery(QUERIES.get(1)),
                                                                    configure(QUERIES.get(1),
                                                                              gav,
                                                                              configuration));

        final Map<String, Collection<DataRow>> missingServices = extractDataFromResultSet(
                dao.search(queryMissingService),
                this::buildMissingService);

        if (missingServices != null && !missingServices.isEmpty()) {
            data.putAll(missingServices);
        }

        log.info("\n{}", Neo4jRenderingUtils.rendering(data,configuration,"versionEnv"));


        dao.shutdown();
    }


    // =========================================================================
    // BUILDER
    // =========================================================================
    public void buildModels(final Map<String, Collection<DataRow>> data,
                            final Map<String, Object> record,
                            final Map<String, Long> envs) {

        log.debug("record : {}", record);
        Collection<DataRow> lines = data.get(ENVIRONMENTS);
        if (lines == null) {
            lines = new ArrayList<>();
            data.put(ENVIRONMENTS, lines);
        }

        buildCurrentProjectEnv(data, record, envs);
        buildProjectDependenciesEnv(data, record, envs);
        buildProjectConsumersEnv(data, record, envs);


    }


    private void buildCurrentProjectEnv(final Map<String, Collection<DataRow>> data, final Map<String, Object> record,
                                        final Map<String, Long> envs) {
        buildLine("version", "env", "deploy", data, record, ConsoleColors.CYAN, envs);
    }

    private void buildProjectDependenciesEnv(final Map<String, Collection<DataRow>> data,
                                             final Map<String, Object> record,
                                             final Map<String, Long> envs) {
        buildLine("dep", "envDep", "deployDep", data, record, envs);
    }

    private void buildProjectConsumersEnv(final Map<String, Collection<DataRow>> data,
                                          final Map<String, Object> record,
                                          final Map<String, Long> envs) {
        buildLine("depProducer", "envProducer", "deployProducer", data, record, envs);
    }


    public void buildLine(final String artifactNodeName,
                          final String envNodeName,
                          final String relationshipName,
                          final Map<String, Collection<DataRow>> data,
                          final Map<String, Object> record,
                          final Map<String, Long> envs) {
        buildLine(artifactNodeName,
                  envNodeName,
                  relationshipName,
                  data,
                  record,
                  null,
                  envs);
    }

    public void buildLine(final String artifactNodeName,
                          final String envNodeName,
                          final String relationshipName,
                          final Map<String, Collection<DataRow>> data,
                          final Map<String, Object> record,
                          final String color,
                          final Map<String, Long> envs) {
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
            if (color != null) {
                currentRow.setRowColor(color);
            }
            currentRow.put(ARTIFACT, artifactName);
            lines.add(currentRow);
        }
        final Object              envNode       = record.get(envNodeName);
        final String              envName       = Neo4jRenderingUtils.getNodeName(envNode);
        final Map<String, Object> envProperties = envNode instanceof Node ? ((Node) envNode).asMap() : null;
        if (envProperties != null) {
            final Long level = envProperties.get("level") instanceof Long ? (Long) envProperties
                    .get("level") : 0L;
            envs.put(envName, level);
        }
        final String deployVersion = renderDeployVersion(record.get(artifactNodeName), record.get(relationshipName));

        currentRow.put(envName, deployVersion);
    }

    // =========================================================================
    // MISSING SERVICE
    // =========================================================================
    private void buildMissingService(final Map<String, Collection<DataRow>> data,
                                     final Map<String, Object> record) {
        Collection<DataRow> currentData = data.get(MISSING_SERVICES);
        if (currentData == null) {
            currentData = new LinkedHashSet<>();
            data.put(MISSING_SERVICES, currentData);
        }

        final Node   service = Neo4jRenderingUtils.getNode(record.get("service"));
        final String type    = Neo4jRenderingUtils.getNodeName(record.get("serviceType"));

        if(service != null){
            final DataRow row = new DataRow();
            row.setRowColor(ConsoleColors.RED);
            row.setUid(Neo4jRenderingUtils.getNodeName(record.get("service")));

            switch (type == null ? "" : type) {
                case "Rest":
                    row.setProperties(buildRestProperties(service, type));
                    break;
                case "JMS":
                    row.setProperties(buildJmsProperties(service, type));
                    break;
                case "rabbitMq":
                    row.setProperties(buildRabbitProperties(service, type));
                    break;
                default:
                    row.setProperties(buildOtherProperties(service, type));
                    break;
            }

            currentData.add(row);
        }
    }

    private Map<String, Serializable> buildRestProperties(final Node service, final String type) {
        final Map<String, Serializable> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("id", service.id());

        ifPropertyNotNull("shortName", service, (value) -> result.put("shortName", String.valueOf(value)));
        ifPropertyNotNull("payload", service, (value) -> result.put("payload", String.valueOf(value)));
        ifPropertyNotNull("responsePayload", service, (value) -> result.put("responsePayload", String.valueOf(value)));

        return result;
    }

    private Map<String, Serializable> buildJmsProperties(final Node service, final String type) {
        final Map<String, Serializable> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("id", service.id());
        ifPropertyNotNull("shortName", service, (value) -> result.put("shortName", String.valueOf(value)));
        ifPropertyNotNull("event", service, (value) -> result.put("payload", String.valueOf(value)));
        return result;
    }

    private Map<String, Serializable> buildRabbitProperties(final Node service, final String type) {
        final Map<String, Serializable> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("id", service.id());
        ifPropertyNotNull("shortName", service, (value) -> result.put("shortName", String.valueOf(value)));
        ifPropertyNotNull("payload", service, (value) -> result.put("payload", String.valueOf(value)));
        return result;
    }

    private Map<String, Serializable> buildOtherProperties(final Node service, final String type) {
        final Map<String, Serializable> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("id", service.id());
        ifPropertyNotNull("shortName", service, (value) -> result.put("shortName", String.valueOf(value)));
        return result;
    }

    // =========================================================================
    // SORT
    // =========================================================================
    public Map<String, Collection<DataRow>> sortData(final Map<String, Collection<DataRow>> firstPass,
                                                     final Map<String, Long> envs,
                                                     final List<String> projectEnv) {
        final Map<String, Collection<DataRow>> result = new LinkedHashMap<>();

        if (envs.isEmpty() || firstPass == null || firstPass.isEmpty()) {
            return firstPass;
        }

        final List<String> envNames = sortEnvironment(envs);

        for (final Map.Entry<String, Collection<DataRow>> entry : firstPass.entrySet()) {
            final List<DataRow> lines = new ArrayList<>();
            for (final DataRow row : entry.getValue()) {
                final DataRow                   newRow     = new DataRow();
                final Map<String, Serializable> properties = new LinkedHashMap<>();
                final Map<String, Serializable> props      = row.getProperties();
                newRow.setRowColor(row.getRowColor());
                properties.put(ARTIFACT, props.get(ARTIFACT));

                for (final String env : envNames) {
                    final Serializable envValue = props.get(env);
                    if (envValue == null) {
                        properties.put(env, EMPTY);
                        if (projectEnv != null && projectEnv.contains(env)) {
                            newRow.setRowColor(ConsoleColors.RED);
                        }
                    }
                    else {
                        properties.put(env, envValue);
                    }
                }


                newRow.setUid(row.getUid());

                newRow.setProperties(properties);
                lines.add(newRow);
            }

            result.put(entry.getKey(), lines);
        }
        return result;
    }

    public List<String> sortEnvironment(final Map<String, Long> envs) {
        final List<String> result = new ArrayList<>();

        if (envs != null && !envs.isEmpty()) {
            final Map<Long, Set<String>> buffer = new LinkedHashMap<>();
            for (final Map.Entry<String, Long> entry : envs.entrySet()) {
                Set<String> index = buffer.get(entry.getValue());
                if (index == null) {
                    index = new LinkedHashSet<>();
                    buffer.put(entry.getValue(), index);
                }
                index.add(entry.getKey());
            }

            final List<Long> indexies = new ArrayList<>(buffer.keySet());
            Collections.sort(indexies);

            for (final Long index : indexies) {
                final List<String> indexEnvs = new ArrayList<>(buffer.get(index));
                Collections.sort(indexEnvs);
                result.addAll(indexEnvs);
            }
        }


        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public String buildArtifactName(final Object nodeRaw) {
        String result = null;
        if (nodeRaw instanceof Node) {
            final Map<String, Object> properties = ((Node) nodeRaw).asMap();
            final String              groupId    = asString(properties.get(GROUP_ID));
            final String              artifactId = asString(properties.get(ARTIFACT_ID));
            result = new StringBuilder().append(groupId).append(':').append(artifactId).toString();

        }
        return result;
    }

    public String renderDeployVersion(final Object nodeRaw, final Object relationshipRaw) {
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

    private List<String> extractProjectEnvs(final Map<String, Collection<DataRow>> data, final Gav gav) {
        final List<String> result = new ArrayList<>();
        if (data != null) {
            final String name = String.join(":", gav.getGroupId(), gav.getArtifactId());
            for (final Map.Entry<String, Collection<DataRow>> entry : data.entrySet()) {
                for (final DataRow row : entry.getValue()) {
                    if (name.equals(row.getUid())) {
                        final Set<String> envs = Optional.ofNullable(row.getProperties()).orElse(new LinkedHashMap<>())
                                                         .keySet();
                        for (final String env : envs) {
                            if (!ARTIFACT.equals(env)) {
                                result.add(env);
                            }
                        }

                    }
                }
            }
        }
        return result;
    }

    private String asString(final Object value) {
        return value == null ? null : String.valueOf(value);
    }




}
