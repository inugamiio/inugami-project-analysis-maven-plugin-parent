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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers.asciidoc;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.writers.asciidoc.AsciidocInfoWriter;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.ServiceExtractor;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.ServiceDto;

import java.util.*;

import static io.inugami.maven.plugin.analysis.api.tools.StringTools.orElse;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;
import static io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.ServiceExtractor.*;

public class ServiceAsciidocWriter implements AsciidocInfoWriter {
    public static final String METHODS       = "*Methods :* ";
    public static final String CONSUMERS     = "*Consumers :* ";
    public static final String PRODUCERS     = "*Producers :* ";
    public static final String BINDING       = "*Binding :* ";
    public static final String QUEUE         = "*Queue :* ";
    public static final String HEADERS       = "*Headers :* ";
    public static final String PAYLOAD_TITLE = "*Payload :* ";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public String getParagraphName() {
        return "services";
    }

    @Override
    public String getfeatureName() {
        return "io.inugami.maven.plugin.analysis.asciidoc.services.enabled";
    }

    @Override
    public LinkedHashMap<String, String> rendering(final ReleaseNoteResult releaseNote, final boolean notSplitFile,
                                                   final InfoContext context) {
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();

        result.put("services_base", renderBase());

        if (releaseNote != null) {
            renderingService("exposed services", "exposed_service",
                             releaseNote.getDifferentials().get(ServiceExtractor.EXPOSED_SERVICES), result,
                             notSplitFile);
            renderingService("consumed services", "consumed_service",
                             releaseNote.getDifferentials().get(ServiceExtractor.CONSUMED_SERVICES), result,
                             notSplitFile);
        }

        return result;
    }


    // =========================================================================
    // RENDERING
    // =========================================================================
    private String renderBase() {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("== Services").line();
        return writer.toString();
    }

    private void renderingService(final String title,
                                  final String paragraphPrefix,
                                  final Differential differential,
                                  final LinkedHashMap<String, String> result,
                                  final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();

        if (notSplitFile) {
            writer.write("=== ").write(title.substring(0, 1).toUpperCase()).write(title.substring(1)).line();
        }
        result.put(paragraphPrefix, writer.toString());
        //@formatter:off
        result.put(paragraphPrefix + "_new", processServiceRendering(notNull(differential.getNewValues()), "New " + title, notSplitFile));
        result.put(paragraphPrefix + "_deleted", processServiceRendering(notNull(differential.getDeletedValues()), "Deleted " + title, notSplitFile));
        result.put(paragraphPrefix + "_same", processServiceRendering(notNull(differential.getSameValues()), "Same " + title, notSplitFile));
        //@formatter:on
    }

    private String processServiceRendering(final List<JsonObject> values, final String title,
                                           final boolean notSplitFile) {
        final JsonBuilder                   writer   = new JsonBuilder();
        final Map<String, List<ServiceDto>> services = convertToServiceDto(values);
        if (notSplitFile) {
            writer.write(H4).write(title).line();
        }


        final List<String> types = new ArrayList<>(services.keySet());
        Collections.sort(types);

        for (final String type : types) {
            final List<ServiceDto> servicesByType = services.get(type);
            switch (type) {

                case TYPE_REST:
                    writer.write(renderRestServices(servicesByType));
                    break;
                case TYPE_JMS:
                    writer.write(renderJmsService(servicesByType));
                    break;
                case TYPE_RABBITMQ:
                    writer.write(renderRabbitMq(servicesByType));
                    break;
                default:
                    writer.write(renderDefaultService(servicesByType));
                    break;
            }
        }
        return writer.toString();
    }


    private String renderRestServices(final List<ServiceDto> services) {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("===== REST services ").line();

        for (final ServiceDto service : services) {
            writer.write(H6).write(service.getVerb()).write(" ").write(service.getUri()).line();
            processIfNotNull(service.getHeaders(), value -> writer.line().write(HEADERS).write(value).line());
            processIfNotNull(service.getConsumeContentType(),
                             value -> writer.line().write("*Consume content-type :* ").write(value).line());
            processIfNotNull(service.getContentType(),
                             value -> writer.line().write("*Content-type :* ").write(value).line());
            processIfNotNull(service.getPayload(),
                             value -> writer.line().write(PAYLOAD_TITLE).write(renderPayload(value)).line());
            processIfNotNull(service.getResponsePayload(),
                             value -> writer.line().write("*Response payload :* ").write(renderPayload(value)).line());
            processIfNotNull(service.getProducers(), values -> {
                writer.line().write(PRODUCERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getConsumers(), values -> {
                writer.line().write(CONSUMERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getMethods(), values -> {
                writer.line().write(METHODS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            writer.line();
        }
        return writer.toString();
    }


    private String renderJmsService(final List<ServiceDto> services) {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("===== JMS services ").line();
        for (final ServiceDto service : services) {
            writer.write(H6).write(orElse(service.getShortName(), UNRESOLVED)).line();
            processIfNotNull(service.getUri(), value -> writer.line().write(QUEUE).write(value).line());
            processIfNotNull(service.getHeaders(), value -> writer.line().write(HEADERS).write(value).line());
            processIfNotNull(service.getPayload(),
                             value -> writer.line().write(PAYLOAD_TITLE).write(renderPayload(value)).line());
            processIfNotNull(service.getProducers(), values -> {
                writer.line().write(PRODUCERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getConsumers(), values -> {
                writer.line().write(CONSUMERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getMethods(), values -> {
                writer.line().write(METHODS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            writer.line();
        }
        return writer.toString();
    }

    private String renderRabbitMq(final List<ServiceDto> services) {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("===== RabbitMq services ").line();
        for (final ServiceDto service : services) {
            writer.write(H6).write(orElse(service.getShortName(), UNRESOLVED)).line();
            processIfNotNull(service.getUri(), value -> writer.line().write(QUEUE).write(value).line());
            processIfNotNull(service.getHeaders(), value -> writer.line().write(HEADERS).write(value).line());
            processIfNotNull(service.getPayload(),
                             value -> writer.line().write(PAYLOAD_TITLE).write(renderPayload(value)).line());
            processIfNotNull(service.getAdditionalInfo(),
                             value -> writer.line().write(BINDING).write(renderPayload(value)).line());
            processIfNotNull(service.getProducers(), values -> {
                writer.line().write(PRODUCERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getConsumers(), values -> {
                writer.line().write(CONSUMERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getMethods(), values -> {
                writer.line().write(METHODS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            writer.line();
        }
        return writer.toString();
    }

    private String renderDefaultService(final List<ServiceDto> services) {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("===== Others services ").line();
        for (final ServiceDto service : services) {
            writer.write(H6).write(orElse(service.getShortName(), UNRESOLVED)).line();

            processIfNotNull(service.getUri(), value -> writer.line().write(QUEUE).write(value).line());
            processIfNotNull(service.getHeaders(), value -> writer.line().write("*headers :* ").write(value).line());
            processIfNotNull(service.getConsumeContentType(),
                             value -> writer.line().write("*Consume content-type :* ").write(value).line());
            processIfNotNull(service.getContentType(),
                             value -> writer.line().write("*Content-type :* ").write(value).line());
            processIfNotNull(service.getPayload(),
                             value -> writer.line().write(PAYLOAD_TITLE).write(renderPayload(value)).line());
            processIfNotNull(service.getResponsePayload(),
                             value -> writer.line().write("*Response payload :* ").write(renderPayload(value)).line());
            processIfNotNull(service.getAdditionalInfo(),
                             value -> writer.line().write("*Other information :* ").write(renderPayload(value)).line());
            processIfNotNull(service.getProducers(), values -> {
                writer.line().write(PRODUCERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getConsumers(), values -> {
                writer.line().write(CONSUMERS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            processIfNotNull(service.getMethods(), values -> {
                writer.line().write(METHODS).line();
                values.forEach(value -> writer.line().write(LIST_DECO).write(value).line());
            });
            writer.line();
        }
        return writer.toString();
    }


    // =========================================================================
    // TOOLS
    // =========================================================================
    private Map<String, List<ServiceDto>> convertToServiceDto(final List<JsonObject> values) {
        final Map<String, List<ServiceDto>> result = new LinkedHashMap<>();
        for (final JsonObject object : values) {
            if (object instanceof ServiceDto) {
                final ServiceDto service = (ServiceDto) object;
                List<ServiceDto> bucket  = result.get(service.getType());
                if (bucket == null) {
                    bucket = new ArrayList<>();
                    result.put(service.getType(), bucket);
                }
                bucket.add(service);
            }
        }


        for (final Map.Entry<String, List<ServiceDto>> entry : result.entrySet()) {
            Collections.sort(entry.getValue());
        }
        return result;
    }
}
