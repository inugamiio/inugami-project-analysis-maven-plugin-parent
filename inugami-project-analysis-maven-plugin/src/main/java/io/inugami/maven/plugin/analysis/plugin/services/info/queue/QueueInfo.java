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
package io.inugami.maven.plugin.analysis.plugin.services.info.queue;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.DefaultNeo4jDao;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;

import java.util.*;
import java.util.stream.Collectors;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

@SuppressWarnings({"java:S6213"})
@Slf4j
public class QueueInfo implements ProjectInformation, QueryConfigurator {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final List<String> QUERIES     = List.of(
            "META-INF/queries/search_services_queue_expose.cql",
            "META-INF/queries/search_services_queue_consume.cql"
    );
    public static final  String       DOUBLE_TAB  = "\t\t";
    public static final  String       LIST_LEVEL2 = "\t\t- ";
    public static final  String       PACKAGING   = "packaging";
    public static final  String       JAR         = "jar";

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
                Map.entry(GROUP_ID, gav.getGroupId()),
                Map.entry(ARTIFACT_ID, gav.getArtifactId()),
                Map.entry(VERSION, gav.getVersion())
        ));
        return config;
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final InfoContext context) {
        final DefaultNeo4jDao       dao  = new DefaultNeo4jDao(context.getConfiguration());
        final Gav                   gav  = convertMavenProjectToGav(context.getProject());
        final List<ArtifactService> data = searchService(gav, dao, context.getConfiguration());

        if (data != null) {
            render(data);
        }
        dao.shutdown();
    }

    // =========================================================================
    // SEARCH
    // =========================================================================
    private List<ArtifactService> searchService(final Gav gav, final DefaultNeo4jDao dao,
                                                final ConfigHandler<String, String> config) {
        final Map<String, ArtifactService> result = new LinkedHashMap<>();


        retrieveData("META-INF/queries/search_services_queue_expose.cql", gav, dao, config, result, true);
        retrieveData("META-INF/queries/search_services_queue_consume.cql", gav, dao, config, result, false);

        return result.entrySet()
                     .stream()
                     .map(Map.Entry::getValue)
                     .collect(Collectors.toList());
    }

    private void retrieveData(final String queryPath, final Gav gav, final DefaultNeo4jDao dao,
                              final ConfigHandler<String, String> config,
                              final Map<String, ArtifactService> result,
                              final boolean expose) {
        final String query = TemplateRendering
                .render(QueriesLoader.getQuery(queryPath), configure(queryPath, gav, config));
        log.info(query);
        final List<Record> resultSet = dao.search(query);
        //@formatter:on

        if (!resultSet.isEmpty()) {

            for (final Record record : resultSet) {
                retrieveDataOnRecord(result, expose, record);
            }

        }
    }

    protected void retrieveDataOnRecord(final Map<String, ArtifactService> result, final boolean expose, final Record record) {
        final String    dependency      = convertToGav(record.get("dep"));
        ArtifactService artifactservice = result.get(dependency);

        if (artifactservice == null) {
            artifactservice = new ArtifactService();

            artifactservice.setGav(dependency);
            result.put(dependency, artifactservice);
        }


        String serviceName = extractName(record.get("service"));
        if (serviceName == null) {
            serviceName = "undefine";
        }
        final Map<String, Object> service     = extractMap(record.get("service"));
        final String              serviceType = extractName(record.get("serviceType"));
        final String depConsumer = record.containsKey("depConsumer") ?
                convertToGav(record.get("depConsumer")) : null;
        final String depExposer = record.containsKey("depExposer") ?
                convertToGav(record.get("depExposer")) : null;

        final String method = extractName(record.get("method"));

        Service currentService = artifactservice.getServices().get(serviceName);
        if (currentService == null) {
            currentService = new Service();
            currentService.setName(serviceName);
            currentService.setType(serviceType);
            artifactservice.getServices().put(serviceName, currentService);
        }

        processIfNotNull(service, currentService.getData()::putAll);
        processIfNotNull(depConsumer, currentService.getConsumers()::add);
        processIfNotNull(depExposer, currentService.getProducers()::add);
        processIfNotNull(method, currentService.getMethods()::add);
        if (expose) {
            currentService.getProducers().add(dependency);
        } else {
            currentService.getConsumers().add(dependency);
        }
        log.debug("currentService : {}", currentService);
    }

    private String convertToGav(final Value node) {
        String result = null;
        if (node != null && !node.isNull()) {
            final Map<String, Object> data = node.asMap();
            result = Gav.builder()
                        .artifactId(extractValue(ARTIFACT_ID, EMPTY, data))
                        .groupId(extractValue(GROUP_ID, EMPTY, data))
                        .version(extractValue(VERSION, EMPTY, data))
                        .type(extractValue(PACKAGING, JAR, data))
                        .build()
                        .getHash();
        }
        return result;
    }

    private String extractName(final Value value) {
        Map<String, Object>    data   = null;
        if (value != null && !value.isNull()) {
            value.asNode().labels();
            data = value.asMap();
        }
        return data == null ? null : extractValue(NAME, null, data);
    }

    private String extractValue(final String key, final String defaultValue, final Map<String, Object> data) {
        String result = null;
        if (data != null && data.containsKey(key)) {
            result = String.valueOf(data.get(key));
        }
        return result == null ? defaultValue : result;
    }

    private Map<String, Object> extractMap(final Value node) {
        Map<String, Object> result = null;
        if (node != null && !node.isNull()) {
            result = node.asMap();
        }
        return result;
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    private void render(final List<ArtifactService> data) {
        final JsonBuilder writer = new JsonBuilder();
        data.sort((ref, value) -> {
            int result = 0;
            if ((ref == null || ref.getGav() == null) && value != null) {
                result = 1;
            } else if (ref != null && ref.getGav() == null && value != null) {
                result = ref.getGav().compareTo(value.getGav());
            } else {
                result = -1;
            }
            return result;
        });
        for (final ArtifactService artifactService : data) {
            writer.write(renderArtifactService(artifactService)).line();
        }

        log.info("\n{}", writer.toString());
    }

    private String renderArtifactService(final ArtifactService artifactService) {
        final JsonBuilder writer = new JsonBuilder();
        if (artifactService == null || artifactService.getGav() == null) {
            return writer.toString();
        }
        writer.write(ConsoleColors.CYAN);
        writer.write(ConsoleColors.createLine("=", 80)).line();
        writer.write(artifactService.gav).line();
        writer.write(ConsoleColors.createLine("=", 80)).line();
        writer.write(ConsoleColors.RESET);

        for (final String key : orderKeys(artifactService.getServices().keySet())) {
            final Service service = artifactService.getServices().get(key);
            if (service == null || service.getName() == null) {
                continue;
            }
            writer.line();
            writer.write(ConsoleColors.RED);
            writer.write("<").write(service.getType()).write("> ");
            writer.write(service.getName()).write(ConsoleColors.RESET).line();
            writer.write("\t properties:").line();

            for (final String propKey : orderKeys(service.getData().keySet())) {
                final String[] propLines = String.valueOf(service.getData().get(propKey)).split("\n");
                writer.write(DOUBLE_TAB)
                      .write(propKey).write(":")
                      .write(String.join("\n\t\t" + ConsoleColors.createLine(SPACE, propKey.length() + 1), propLines));
                writer.line();
            }

            writer.write(ConsoleColors.GREEN);
            writer.write("\t consumers:").line();
            for (final String consumer : orderKeys(service.getConsumers())) {
                writer.write(LIST_LEVEL2).write(consumer).line();
            }
            writer.write(ConsoleColors.RESET);

            writer.write(ConsoleColors.YELLOW);
            writer.write("\t producers:").line();
            for (final String producer : orderKeys(service.getProducers())) {
                writer.write(LIST_LEVEL2).write(producer).line();
            }
            writer.write(ConsoleColors.RESET);

            writer.write("\t methods:").line();
            for (final String method : orderKeys(service.getMethods())) {
                writer.write(LIST_LEVEL2).write(method).line();
            }

        }
        return writer.toString();
    }

    private List<String> orderKeys(final Collection<String> keySet) {
        final List<String> result = new ArrayList<>(keySet);
        result.sort(Comparable::compareTo);
        return result;
    }

    // =========================================================================
    // DATA
    // =========================================================================
    @ToString
    @NoArgsConstructor
    @Getter
    @Setter
    private static class ArtifactService {
        String               gav;
        Map<String, Service> services = new HashMap<>();
    }

    @ToString
    @NoArgsConstructor
    @Getter
    @Setter
    private static class Service {
        String              name;
        String              type;
        Map<String, Object> data      = new HashMap<>();
        Set<String>         consumers = new LinkedHashSet<>();
        Set<String>         producers = new LinkedHashSet<>();
        Set<String>         methods   = new LinkedHashSet<>();
    }


}
