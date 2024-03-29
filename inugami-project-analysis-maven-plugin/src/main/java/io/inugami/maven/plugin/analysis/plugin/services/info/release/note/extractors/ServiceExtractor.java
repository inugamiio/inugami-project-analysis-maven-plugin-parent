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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.ServiceDto;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.extractNode;
import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.getNodeName;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_ALL_CONSUMED_SERVICES;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_ALL_EXPOSED_SERVICES;

@SuppressWarnings("java:S6213")
@Slf4j
public class ServiceExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperBuilder.build();

    public static final String EXPOSED_SERVICES      = "exposedService";
    public static final String CONSUMED_SERVICES     = "consumedService";
    public static final String NODE_SERVICE          = "service";
    public static final String NODE_SERVICE_TYPE     = "serviceType";
    public static final String NODE_SERVICE_CONSUMER = "consumer";
    public static final String NODE_SERVICE_EXPOSE   = "producer";
    public static final String NAME                  = "name";
    public static final String SHORT_NAME            = "shortName";
    public static final String PAYLOAD               = "payload";
    public static final String EVENT                 = "event";
    public static final String DESTINATION           = "destination";
    public static final String BINDINGS              = "bindings";
    public static final String URI                   = "uri";
    public static final String METHOD                = "method";
    public static final String VERB                  = "verb";
    public static final String RESPONSE_PAYLOAD      = "responsePayload";
    public static final String TYPE_REST             = "REST";
    public static final String TYPE_JMS              = "JMS";
    public static final String TYPE_RABBITMQ         = "RABBITMQ";
    public static final String NODE_METHOD           = "method";
    public static final String METHOD_ARTIFACT       = "methodArtifact";
    public static final String REQUEST_PAYLOAD       = "requestPayload";
    public static final String CONTENT_TYPE          = "contentType";
    public static final String CONSUME_CONTENT_TYPE  = "accept";
    public static final String HEADER                = "header";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        manageExposeServices(releaseNoteResult, currentVersion, previousVersion, dao, context);
        manageConsumeServices(releaseNoteResult, currentVersion, previousVersion, dao, context);

    }


    private void manageExposeServices(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                      final Gav previousVersion, final Neo4jDao dao,
                                      final InfoContext context) {

        final List<JsonObject> currentServices = search(QUERIES_SEARCH_ALL_EXPOSED_SERVICES, currentVersion,
                                                        context.getConfiguration(), dao, this::convert);

        final List<JsonObject> previousService = previousVersion == null ? null : search(
                QUERIES_SEARCH_ALL_EXPOSED_SERVICES,
                previousVersion,
                context.getConfiguration(),
                dao,
                this::convert);

        releaseNoteResult.addDifferential(EXPOSED_SERVICES, Differential
                .buildDifferential(newSet(currentServices), newSet(previousService)));

    }

    private void manageConsumeServices(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                       final Gav previousVersion, final Neo4jDao dao,
                                       final InfoContext context) {

        final List<JsonObject> currentServices = search(QUERIES_SEARCH_ALL_CONSUMED_SERVICES, currentVersion,
                                                        context.getConfiguration(), dao, this::convert);

        final List<JsonObject> previousService = previousVersion == null ? null : search(
                QUERIES_SEARCH_ALL_CONSUMED_SERVICES,
                previousVersion,
                context.getConfiguration(),
                dao,
                this::convert);

        releaseNoteResult.addDifferential(CONSUMED_SERVICES, Differential
                .buildDifferential(newSet(currentServices), newSet(previousService)));

    }

    // =========================================================================
    // CONVERT
    // =========================================================================
    private List<JsonObject> convert(final List<Record> resultSet) {
        final List<JsonObject> result = new ArrayList<>();

        final Map<String, ServiceDto> buffer = new LinkedHashMap<>();
        for (final Record record : resultSet) {
            final NodeValue property = extractNode(NODE_SERVICE, record);
            final String type = String.valueOf(getNodeName(extractNode(NODE_SERVICE_TYPE, record)))
                                      .toUpperCase();
            if (property != null) {
                ServiceDto                itemResult = null;
                final Map<String, Object> properties = property.asMap();

                switch (type) {
                    case TYPE_REST:
                        itemResult = convertRestService(properties, record, type);
                        break;
                    case TYPE_JMS:
                        itemResult = convertJMSService(properties, record, type);
                        break;
                    case TYPE_RABBITMQ:
                        itemResult = convertRabbitMqService(properties, record, type);
                        break;
                    default:
                        itemResult = convertGenericService(properties, record, type);
                        break;
                }

                if (buffer.containsKey(itemResult.getName())) {
                    buffer.get(itemResult.getName()).mergeConsumers(itemResult.getConsumers());
                    buffer.get(itemResult.getName()).mergeProducers(itemResult.getProducers());
                    buffer.get(itemResult.getName()).mergeMethods(itemResult.getMethods());
                } else {
                    buffer.put(itemResult.getName(), itemResult);
                }
            }


        }

        if (!buffer.isEmpty()) {
            buffer.entrySet().stream().map(Map.Entry::getValue).forEach(result::add);
        }
        return result;
    }


    private ServiceDto convertRestService(final Map<String, Object> properties, final Record record,
                                          final String type) {
        final String serviceExpose  = getNodeName(extractNode(NODE_SERVICE_EXPOSE, record));
        final String serviceConsume = getNodeName(extractNode(NODE_SERVICE_CONSUMER, record));
        final String method = buildMethod(extractNode(NODE_METHOD, record),
                                          extractNode(METHOD_ARTIFACT, record));

        return ServiceDto.builder()
                         .name(String.valueOf(properties.get(NAME)))
                         .shortName(String.valueOf(properties.get(SHORT_NAME)))
                         .uri(String.valueOf(properties.get(URI)))
                         .verb(String.valueOf(properties.get(VERB)))
                         .payload((String) properties.get(REQUEST_PAYLOAD))
                         .responsePayload((String) properties.get(RESPONSE_PAYLOAD))
                         .contentType((String) properties.get(CONTENT_TYPE))
                         .consumeContentType((String) properties.get(CONSUME_CONTENT_TYPE))
                         .headers((String) properties.get(HEADER))
                         .type(type)
                         .build()
                         .addConsumer(serviceConsume)
                         .addProducer(serviceExpose)
                         .addMethod((String) (properties.get(METHOD)))
                         .addMethod(method);
    }


    private ServiceDto convertJMSService(final Map<String, Object> properties, final Record record, final String type) {
        final String serviceExpose  = getNodeName(extractNode(NODE_SERVICE_EXPOSE, record));
        final String serviceConsume = getNodeName(extractNode(NODE_SERVICE_CONSUMER, record));
        final String method = buildMethod(extractNode(NODE_METHOD, record),
                                          extractNode(METHOD_ARTIFACT, record));

        return ServiceDto.builder()
                         .type(type)
                         .name(String.valueOf(properties.get(NAME)))
                         .shortName(String.valueOf(properties.get(SHORT_NAME)))
                         .payload(String.valueOf(properties.get(EVENT)))
                         .uri(String.valueOf(properties.get(DESTINATION)))
                         .build()
                         .addConsumer(serviceConsume)
                         .addProducer(serviceExpose)
                         .addMethod(method);
    }

    private ServiceDto convertRabbitMqService(final Map<String, Object> properties, final Record record,
                                              final String type) {
        final String serviceExpose  = getNodeName(extractNode(NODE_SERVICE_EXPOSE, record));
        final String serviceConsume = getNodeName(extractNode(NODE_SERVICE_CONSUMER, record));
        final String method = buildMethod(extractNode(NODE_METHOD, record),
                                          extractNode(METHOD_ARTIFACT, record));

        final String binding        = String.valueOf(properties.get(BINDINGS));
        String       additionalInfo = null;
        if (binding != null) {
            try {
                final JsonNode jsonNode = OBJECT_MAPPER.readTree(binding);
                additionalInfo = OBJECT_MAPPER.writeValueAsString(jsonNode);
            } catch (final JsonProcessingException e) {
                log.error("{} :{}", e.getMessage(), binding, e);
            }
        }

        return ServiceDto.builder()
                         .type(type)
                         .name(String.valueOf(properties.get(NAME)))
                         .shortName(String.valueOf(properties.get(SHORT_NAME)))
                         .payload(String.valueOf(properties.get(PAYLOAD)))
                         .additionalInfo(additionalInfo)
                         .build()
                         .addConsumer(serviceConsume)
                         .addProducer(serviceExpose)
                         .addMethod(method);
    }

    private ServiceDto convertGenericService(final Map<String, Object> properties, final Record record,
                                             final String type) {
        final String serviceExpose  = getNodeName(extractNode(NODE_SERVICE_EXPOSE, record));
        final String serviceConsume = getNodeName(extractNode(NODE_SERVICE_CONSUMER, record));
        final String method = buildMethod(extractNode(NODE_METHOD, record),
                                          extractNode(METHOD_ARTIFACT, record));

        return ServiceDto.builder()
                         .type(type)
                         .name(String.valueOf(properties.get(NAME)))
                         .shortName(String.valueOf(properties.get(SHORT_NAME)))
                         .payload(String.valueOf(properties.get(PAYLOAD)))
                         .build()
                         .addConsumer(serviceConsume)
                         .addProducer(serviceExpose)
                         .addMethod(method);
    }

    private String buildMethod(final NodeValue nodeMethod, final NodeValue nodeArtifact) {
        final StringBuilder buffer = new StringBuilder();
        if (nodeArtifact != null) {
            buffer.append(getNodeName(nodeArtifact)).append(":");
        }
        if (nodeMethod != null) {
            buffer.append(getNodeName(nodeMethod));
        }
        final String result = buffer.toString();
        return result.isEmpty() ? null : result;
    }


}
