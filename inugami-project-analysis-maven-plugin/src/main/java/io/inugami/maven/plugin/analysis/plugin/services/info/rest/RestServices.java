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
package io.inugami.maven.plugin.analysis.plugin.services.info.rest;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.models.neo4j.RestEndpointConvertor;
import io.inugami.maven.plugin.analysis.api.models.neo4j.VersionConvertor;
import io.inugami.maven.plugin.analysis.api.models.neo4j.VersionNode;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.api.utils.NodeUtils;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;

import java.util.*;
import java.util.function.BiConsumer;

@Slf4j
public class RestServices implements ProjectInformation, QueryConfigurator {

    private static final List<String> QUERIES = List.of(
            "META-INF/queries/search_consumers.cql",
            "META-INF/queries/search_produce.cql",
            "META-INF/queries/search_services_rest.cql"
                                                       );

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
                Map.entry("version", gav.getVersion()),
                Map.entry("serviceType", "Rest")
                                   ));
        return config;
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final InfoContext context) {
        final Neo4jDao dao = new Neo4jDao(context.getConfiguration());
        final Gav      gav = convertMavenProjectToGav(context.getProject());
        final DependencyRest consumeDependencies = searchConsumedService(gav, dao,
                                                                         context.getConfiguration());
        final DependencyRest exposedDependencies = searchExposedService(gav, dao,
                                                                        context.getConfiguration());

        renderDependencies(gav, consumeDependencies, exposedDependencies);

        dao.shutdown();
    }


    private DependencyRest searchConsumedService(final Gav gav, final Neo4jDao dao,
                                                 final ConfigHandler<String, String> config) {
        //@formatter:off
        final DependencyRest  result = new DependencyRest();
        final String          queryPath    = "META-INF/queries/search_consumers.cql";
        final String          query        = TemplateRendering.render(QueriesLoader.getQuery(queryPath), configure(queryPath, gav, config));
        final List<Record>    resultSet    = dao.search(query);
        //@formatter:on

        if (!resultSet.isEmpty()) {
            final Map<RestEndpoint, DependencyRest> endpoints = new LinkedHashMap<>();
            for (final Record record : resultSet) {
                final Map<RestEndpoint, DependencyRest> localeEndpoints = extractAllEndpoint(resultSet,
                                                                                             this::extractConsumeEndpoint);
                if (localeEndpoints != null) {
                    localeEndpoints.entrySet().forEach((entry -> {
                        result.addProducers(entry.getValue().getProducers());
                        result.addConsumers(entry.getValue().getConsumers());
                    }));
                    endpoints.putAll(localeEndpoints);
                }
            }
            log.info(
                    "\n====================================\nCONSUMED REST ENDPOINTS\n====================================\n{}",
                    renderConsume(endpoints));

        }

        return result;
    }

    private DependencyRest searchExposedService(final Gav gav, final Neo4jDao dao,
                                                final ConfigHandler<String, String> config) {

        //@formatter:off
        final DependencyRest  result = new DependencyRest();
        final String       queryPath    = "META-INF/queries/search_produce.cql";
        final String       query        = TemplateRendering.render(QueriesLoader.getQuery(queryPath), configure(queryPath, gav, config));
        log.info(query);
        final List<Record> resultSet    = dao.search(query);
        //@formatter:on

        if (!resultSet.isEmpty()) {

            final Map<RestEndpoint, DependencyRest> endpoints = new LinkedHashMap<>();
            for (final Record record : resultSet) {
                final Map<RestEndpoint, DependencyRest> localeEndpoints = extractAllEndpoint(resultSet,
                                                                                             this::extractExposedEndpoint);
                if (localeEndpoints != null) {
                    localeEndpoints.entrySet().forEach((entry -> {
                        result.addProducers(entry.getValue().getProducers());
                        result.addConsumers(entry.getValue().getConsumers());
                    }));
                    endpoints.putAll(localeEndpoints);
                }
            }
            log.info(
                    "\n====================================\nEXPOSE REST ENDPOINTS\n====================================\n{}",
                    renderConsume(endpoints));
        }

        return result;
    }

    private void renderDependencies(final Gav gav, final DependencyRest consumeDependencies,
                                    final DependencyRest exposedDependencies) {

        final JsonBuilder json = new JsonBuilder();
        json.line();
        json.write("====================================\nDEPENDENCIES\n====================================");
        json.line();

        if (consumeDependencies != null && !consumeDependencies.getProducers().isEmpty()) {
            json.write("------------------------------\nCONSUME By CURRENT PROJECT\n------------------------------");
            json.line();
            for (final String appExpose : consumeDependencies.getProducers()) {
                json.write(ConsoleColors.BLUE_BOLD).write(gav.getHash()).write(" -> ").write(appExpose)
                    .write(ConsoleColors.RESET);
            }
        }


        if (exposedDependencies != null && !exposedDependencies.getConsumers().isEmpty()) {
            json.write("------------------------------\nPRODUCE BY CURRENT PROJECT\n------------------------------");
            json.line();
            for (final String appConsumer : exposedDependencies.getConsumers()) {
                json.write(ConsoleColors.YELLOW_BOLD).write(gav.getHash()).write(" <- ").write(appConsumer)
                    .write(ConsoleColors.RESET);
            }
        }

        json.line();
        json.line();
        log.info(json.toString());
    }

    // =========================================================================
    // EXTRACT
    // =========================================================================
    private Map<RestEndpoint, DependencyRest> extractAllEndpoint(final List<Record> resultSet,
                                                                 final BiConsumer<Map<RestEndpoint, DependencyRest>, Map<String, Object>> handler) {

        final Map<RestEndpoint, DependencyRest> result = new LinkedHashMap<>();
        for (final Record record : resultSet) {
            handler.accept(result, record.asMap());
        }
        return result;
    }

    private void extractConsumeEndpoint(final Map<RestEndpoint, DependencyRest> buffer,
                                        final Map<String, Object> queryNodes) {
        final RestEndpoint endpoint = RestEndpointConvertor.build((Node) queryNodes.get("serviceConsume"));

        if (endpoint != null) {
            final VersionNode appProducer = VersionConvertor.build((Node) queryNodes.get("depProducer"));
            final VersionNode appConsumer = VersionConvertor.build((Node) queryNodes.get("depConsumer"));

            final DependencyRest savedDependencies = buffer.get(endpoint);

            if (savedDependencies == null) {
                buffer.put(endpoint, new DependencyRest(appProducer == null ? null : appProducer.getName(),
                                                        appConsumer == null ? null : appConsumer.getName()));
            }
            else {
                savedDependencies.addConsumer(appProducer == null ? null : appProducer.getName());
                savedDependencies.addConsumer(appConsumer == null ? null : appConsumer.getName());
            }
        }

    }

    private void extractExposedEndpoint(final Map<RestEndpoint, DependencyRest> buffer,
                                        final Map<String, Object> queryNodes) {
        final RestEndpoint endpoint = RestEndpointConvertor.build((Node) queryNodes.get("service"));
        if (endpoint != null) {
            final VersionNode appProducer = VersionConvertor.build((Node) queryNodes.get("depProducer"));
            final VersionNode appConsumer = VersionConvertor.build((Node) queryNodes.get("depConsumer"));


            final DependencyRest savedDependencies = buffer.get(endpoint);

            if (savedDependencies == null) {
                buffer.put(endpoint, new DependencyRest(appProducer == null ? null : appProducer.getName(),
                                                        appConsumer == null ? null : appConsumer.getName()));
            }
            else {
                savedDependencies.addConsumer(appProducer == null ? null : appProducer.getName());
                savedDependencies.addConsumer(appConsumer == null ? null : appConsumer.getName());
            }
        }
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    private String renderConsume(final Map<RestEndpoint, DependencyRest> endpoints) {
        final JsonBuilder result = new JsonBuilder();


        if (endpoints == null || endpoints.isEmpty()) {
            result.write("no endpoint consumed");
        }
        else {
            final List<RestEndpoint> endpointsKeySet = new ArrayList<>(endpoints.keySet());
            Collections.sort(endpointsKeySet);
            for (final RestEndpoint endpoint : endpointsKeySet) {
                result.write(renderEndpoint(endpoint, endpoints.get(endpoint)));
            }
        }

        return result.toString();
    }


    private String renderEndpoint(final RestEndpoint endpoint, final DependencyRest dependencies) {
        final JsonBuilder result = new JsonBuilder();

        result.write(encodeColor(endpoint.getVerb()));
        result.write("[").write(endpoint.getVerb()).write("] ").write(endpoint.getUri()).write(ConsoleColors.RESET)
              .line();
        result.write("\tuid:").write(endpoint.getUid()).line();
        NodeUtils.processIfNotEmptyForce(endpoint.getMethod(),
                                         (value) -> result.write("\tMethod:").write(value).line());

        NodeUtils.processIfNotEmptyForce(endpoint.getDescription(),
                                         (value) -> result.write("\tDescription:").write(value).line());
        NodeUtils
                .processIfNotEmptyForce(endpoint.getHeaders(),
                                        (value) -> result.write("\tHeader:").write(value).line());
        NodeUtils
                .processIfNotEmptyForce(endpoint.getConsume(),
                                        (value) -> result.write("\tAccept:").write(value).line());
        NodeUtils.processIfNotEmptyForce(endpoint.getProduce(),
                                         (value) -> result.write("\tContent-Type:").write(value).line());
        NodeUtils.processIfNotEmptyForce(endpoint.getBody(),
                                         (value) -> result.write("\trequest payload:\n")
                                                          .write(spacePayload(value, "\t\t"))
                                                          .line());
        NodeUtils.processIfNotEmptyForce(endpoint.getResponseType(),
                                         (value) -> result.write("\tresponse payload:\n")
                                                          .write(spacePayload(value, "\t\t"))
                                                          .line());

        if (!dependencies.getProducers().isEmpty()) {
            result.write("\tProduce by:").line();
            result.write(ConsoleColors.YELLOW);
            for (final String producer : dependencies.getProducers()) {
                result.write("\t\t- ").write(producer).line();
            }
            result.write(ConsoleColors.RESET);
        }
        if (!dependencies.getConsumers().isEmpty()) {
            result.write("\tConsume by:").line();
            result.write(ConsoleColors.YELLOW);
            for (final String consumer : dependencies.getConsumers()) {
                result.write("\t\t- ").write(consumer).line();
            }
            result.write(ConsoleColors.RESET);
        }
        return result.toString();
    }

    private String spacePayload(final String value, final String space) {
        return space + value.replaceAll("\n", "\n" + space);
    }

    private String encodeColor(final String verb) {
        String result = ConsoleColors.RESET;
        if (verb != null) {
            switch (verb) {
                case "GET":
                    result = ConsoleColors.BLUE_BOLD;
                    break;
                case "POST":
                    result = ConsoleColors.GREEN_BOLD;
                    break;
                case "PUT":
                    result = ConsoleColors.YELLOW_BOLD;
                    break;
                case "DELETE":
                    result = ConsoleColors.RED_BOLD;
                    break;
                case "OPTION":
                    result = ConsoleColors.PURPLE_BOLD;
                    break;
            }
        }
        return result;
    }


}
