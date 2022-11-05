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
package io.inugami.maven.plugin.analysis.api.tools;

import io.inugami.api.models.JsonBuilder;
import io.inugami.commons.security.EncryptionUtils;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.rest.RestEndpoint;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.utils.reflection.DescriptionDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.function.Function;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.*;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestAnalyzerUtils {

    // =========================================================================
    // API
    // =========================================================================
    public static final  String DOT                       = ".";
    public static final  String VERB                      = "verb";
    public static final  String SERVICE                   = "Service";
    public static final  String HEADER                    = "header";
    public static final  String ACCEPT                    = "accept";
    public static final  String CONTENT_TYPE              = "contentType";
    public static final  String REQUEST_PAYLOAD           = "requestPayload";
    public static final  String RESPONSE_PAYLOAD          = "responsePayload";
    public static final  String LINE                      = "\n";
    public static final  String EMPTY                     = "";
    public static final  String TAB                       = "\t";
    public static final  String DOUBLE_URL_SEP            = "//";
    public static final  String URI_SEP                   = "/";
    public static final  String QUOT                      = "\"";
    public static final  String SIMPLE_QUOT               = "'";
    public static final  String NICKNAME                  = "nickname";
    private static final String METHOD                    = "method";
    public static final  String IDENTIFIER                = "identifier";
    public static final  String URI                       = "uri";
    public static final  String DESCRIPTION               = "description";
    public static final  String DESCRIPTION_DETAIL        = "descriptionDetail";
    public static final  String DESCRIPTION_URL           = "descriptionUrl";
    public static final  String DESCRIPTION_EXAMPLE       = "descriptionExample";
    public static final  String EXPOSE                    = "EXPOSE";
    public static final  String SEPARATOR                 = ",";
    public static final  String REST                      = "Rest";
    public static final  String SERVICE_TYPE              = "ServiceType";
    public static final  String SERVICE_TYPE_RELATIONSHIP = "SERVICE_TYPE";
    public static final  String ADDITIONAL_IDENTIFIER     = "additionalIdentifier";


    // =========================================================================
    // API
    // =========================================================================
    public static Node convertEndpointToNeo4j(final RestEndpoint endpoint) {
        return convertEndpointToNeo4j(endpoint, null);
    }


    public static Node convertEndpointToNeo4j(final RestEndpoint endpoint,
                                              Function<RestEndpoint, String> identifierAdditionalInfo) {
        final String uid = buildServiceUid(endpoint, identifierAdditionalInfo);
        return Node.builder()
                   .uid(encodeSha1(uid))
                   .name(buildName(endpoint))
                   .type(SERVICE)
                   .properties(buildProperties(endpoint, uid))
                   .build();
    }


    public static String buildServiceUid(final RestEndpoint endpoint,
                                         final Function<RestEndpoint, String> identifierAdditionalInfo) {
        final JsonBuilder json = new JsonBuilder();
        json.addField(VERB).valueQuot(endpoint.getVerb()).addSeparator();
        json.addField(URI).valueQuot(endpoint.getUri()).addSeparator();

        //@formatter:off
        processIfNotNull(endpoint.getHeaders(),     (value)-> json.addField(HEADER).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getConsume(),     (value)-> json.addField(ACCEPT).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getProduce(),     (value)-> json.addField(CONTENT_TYPE).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getBodyRequireOnly(),        (value)-> json.addField(REQUEST_PAYLOAD).valueQuot(value).addSeparator());
        processIfNotNull(endpoint.getResponseTypeRequireOnly(),(value)-> json.addField(RESPONSE_PAYLOAD).valueQuot(value).addSeparator());
        processIfNotNull(identifierAdditionalInfo, function->{
            processIfNotNull(function.apply(endpoint), value-> json.addField(ADDITIONAL_IDENTIFIER).valueQuot(value));
        });
        //@formatter:on

        return json.toString()
                   .replaceAll(LINE, EMPTY)
                   .replaceAll(TAB, EMPTY)
                   .replaceAll(DOUBLE_URL_SEP, URI_SEP)
                   .replaceAll(QUOT, SIMPLE_QUOT);
    }

    public static String encodeSha1(final String value) {
        return value == null ? null : new EncryptionUtils().encodeSha1(value);
    }

    public static String buildName(final RestEndpoint endpoint) {
        String result = endpoint.getNickname();

        if (result == null || result.trim().isEmpty()) {
            result = String.format("[%s]%s", endpoint.getVerb(), endpoint.getUri());
        }
        return result;
    }

    public static LinkedHashMap<String, Serializable> buildProperties(final RestEndpoint endpoint,
                                                                      final String identifier) {
        final LinkedHashMap<String, Serializable> result = new LinkedHashMap<>();
        result.put(VERB, cleanLines(endpoint.getVerb()));
        result.put(URI, cleanLines(endpoint.getUri()));
        result.put(IDENTIFIER, identifier);

        //@formatter:off
        processIfNotEmpty(endpoint.getNickname(),     (value)->result.put(NICKNAME, value));
        processIfNotEmpty(endpoint.getMethod(),       (value)->result.put(METHOD, value));
        processIfNotEmpty(endpoint.getHeaders(),      (value)->result.put(HEADER, value));
        processIfNotEmpty(endpoint.getConsume(),      (value)->result.put(ACCEPT, value));
        processIfNotEmpty(endpoint.getProduce(),      (value)->result.put(CONTENT_TYPE, value));
        processIfNotEmpty(endpoint.getBody(),         (value)->result.put(REQUEST_PAYLOAD, value));
        processIfNotEmpty(endpoint.getResponseType(), (value)->result.put(RESPONSE_PAYLOAD, value));
        processIfNotEmpty(endpoint.getDescription(),  (value)->result.put(DESCRIPTION, value));
        //@formatter:on

        if (endpoint.getDescriptionDetail() != null) {
            final DescriptionDTO detail = endpoint.getDescriptionDetail();
            processIfNotEmpty(detail.getContent(), (value) -> result.put(DESCRIPTION_DETAIL, value));
            processIfNotEmpty(detail.getUrl(), (value) -> result.put(DESCRIPTION_URL, value));
            processIfNotEmpty(detail.getExample(), (value) -> result.put(DESCRIPTION_EXAMPLE, value));
        }

        return result;
    }

    public static boolean existingNode(final Node node,
                                       final Neo4jDao neo4jDao,
                                       final String relationshipType) {
        boolean result = false;
        if (node != null && !EXPOSE.equals(relationshipType) && neo4jDao != null) {
            final org.neo4j.driver.types.Node savedNode = neo4jDao.getNode(node.getUid(), node.getType());
            result = savedNode != null;
        }
        return result;
    }
}
