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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.github;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inugami.commons.connectors.HttpBasicConnector;
import io.inugami.commons.connectors.HttpConnectorResult;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.utils.CacheUtils;
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.IssueTrackerCommons;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.IssueTrackerCommons.*;

@Slf4j
@RequiredArgsConstructor
public class GitHubTask implements Callable<ScanNeo4jResult> {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FIELD_IID        = "number";
    public static final String FIELD_TITLE      = "title";
    public static final String FIELD_WEB_URL    = "html_url";
    public static final String FIELD_BODY       = "body";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_LABELS     = "labels";
    public static final String FIELD_BASE       = "base";
    public static final String FIELD_HEAD       = "head";
    public static final String FIELD_NAME       = "name";

    private final String                         id;
    private final IssueTrackerCommons.TicketType ticketType;
    private final String                         token;
    private final String                         url;
    private final String                         urlPr;
    private final String                         versionUid;
    private final String                         projectSha;

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public ScanNeo4jResult call() throws Exception {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        final Map<String, String> headers = Map.ofEntries(Map.entry("PRIVATE-TOKEN", token),
                                                          Map.entry("Content-Type", "application/json"));
        final ObjectMapper objectMapper = ObjectMapperBuilder.build();
        Node               node         = null;
        switch (ticketType) {
            case MERGE_REQUEST:
                node = processMergeRequest(headers, objectMapper);
                break;
            default:
                node = processIssue(headers, objectMapper, result);
                processIssueLinks(headers, objectMapper, node, result);
                break;
        }

        if (node != null) {
            result.addNode(node);
            result.addRelationship(Relationship.builder()
                                               .from(versionUid)
                                               .to(node.getUid())
                                               .type(HAVE_TICKET)
                                               .build());
            result.addRelationship(Relationship.builder()
                                               .from(node.getUid())
                                               .to(versionUid)
                                               .type(TICKET_HAVE_VERSION)
                                               .build());
        }
        return result;
    }

    private Node processIssue(final Map<String, String> headers, final ObjectMapper objectMapper,
                              final ScanNeo4jResult resultNeo4J) {
        final Node          result  = null;
        final StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(url);
        fullUrl.append("/issues/");
        fullUrl.append(id);

        final JsonNode json = callGitHub(fullUrl.toString(), headers, objectMapper);
        return buildIssueNode(json, resultNeo4J);
    }

    private Node buildIssueNode(final JsonNode json, final ScanNeo4jResult resultNeo4J) {
        Node result = null;
        if (json != null) {

            final String name = IssueTrackerCommons.TicketType.ISSUE.getNodePrefix() +
                    extract(FIELD_IID, json);
            final String uid = projectSha + "_" + name;

            final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();

            processIfNotNull(extract(FIELD_IID, json), value -> properties.put(UUID, value));
            processIfNotNull(extract(FIELD_TITLE, json), value -> properties.put(FIELD_TITLE, value));
            processIfNotNull(extract(FIELD_BODY, json), value -> properties.put(FIELD_DESCRIPTION, value));
            processIfNotNull(extract(FIELD_CREATED_AT, json), value -> properties.put(FIELD_CREATED_AT, value));
            processIfNotNull(extract(FIELD_WEB_URL, json), value -> properties.put(FIELD_URL, value));

            final JsonNode labels = json.get(FIELD_LABELS);
            if (labels != null && labels.isArray()) {
                final List<String> labelNames = new ArrayList<>();
                labels.spliterator().forEachRemaining(item -> {
                    if (!item.isNull()) {
                        final String labelName = extractLabelName(item);
                        processIfNotNull(labelName, labelNames::add);
                    }
                });

                processIfNotNull(extract(FIELD_LABELS, json), value -> properties.put(FIELD_LABELS, value));

                for (final String label : labelNames) {
                    resultNeo4J.addNode(Node.builder()
                                            .type(IssueTrackerCommons.TicketType.ISSUE_LABEL.getNodeType())
                                            .uid(IssueTrackerCommons.TicketType.ISSUE_LABEL.getNodePrefix() + label)
                                            .name(label)
                                            .build());

                    resultNeo4J.addRelationship(Relationship.builder()
                                                            .from(uid)
                                                            .to(IssueTrackerCommons.TicketType.ISSUE_LABEL
                                                                        .getNodePrefix() + label)
                                                            .type(HAS_LABEL)
                                                            .build());
                }
            }

            result = Node.builder().type(TicketType.ISSUE.getNodeType())
                         .uid(uid)
                         .name(name)
                         .properties(properties)
                         .build();
        }

        return result;
    }


    private void processIssueLinks(final Map<String, String> headers, final ObjectMapper objectMapper,
                                   final Node node,
                                   final ScanNeo4jResult resultNeo4J) {

        final Set<String> links = extractLinks(node);

        for (final String linkId : links) {
            final StringBuilder fullUrl = new StringBuilder();
            fullUrl.append(url);
            fullUrl.append("/issues/");
            fullUrl.append(linkId.substring(1));

            final JsonNode link = callGitHub(fullUrl.toString(), headers, objectMapper);
            if (link != null) {
                final Node linkNeo4JNode = buildIssueNode(link, resultNeo4J);
                resultNeo4J.addNode(linkNeo4JNode);
                if (linkNeo4JNode != null) {
                    resultNeo4J.addRelationship(Relationship.builder()
                                                            .from(node.getUid())
                                                            .to(linkNeo4JNode.getUid())
                                                            .type(HAS_ISSUE_LINK)
                                                            .build());
                }

            }
        }
    }

    private Set<String> extractLinks(final Node node) {
        final Set<String> result = new LinkedHashSet<>();
        if (node.getProperties().get(FIELD_DESCRIPTION) != null) {

            final Set<String> tickets = new GitHubIssueTrackerProvider()
                    .extractTicketNumber(String.valueOf(node.getProperties().get(FIELD_DESCRIPTION)));
            if (tickets != null) {
                result.addAll(tickets);
            }
        }
        return result;
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private Node processMergeRequest(final Map<String, String> headers, final ObjectMapper objectMapper) {
        Node                result  = null;
        final StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(urlPr);
        fullUrl.append("/pulls/");
        fullUrl.append(id);

        final JsonNode json = callGitHub(fullUrl.toString(), headers, objectMapper);
        if (json != null) {
            final String                              name       = TicketType.MERGE_REQUEST.getNodePrefix() + id;
            final String                              uid        = projectSha + "_" + name;
            final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();

            processIfNotNull(extract(FIELD_TITLE, json), value -> properties.put(FIELD_TITLE, value));
            processIfNotNull(extract(FIELD_BODY, json), value -> properties.put(FIELD_BODY, value));
            processIfNotNull(extract(FIELD_CREATED_AT, json), value -> properties.put(FIELD_CREATED_AT, value));
            processIfNotNull(extract(FIELD_MERGED_AT, json), value -> properties.put(FIELD_MERGED_AT, value));
            processIfNotNull(extract(FIELD_CLOSED_AT, json), value -> properties.put(FIELD_CLOSED_AT, value));
            processIfNotNull(extract(FIELD_WEB_URL, json), value -> properties.put(FIELD_URL, value));


            processIfNotNull(extractBaseSha(json), value -> properties.put(FIELD_BASE_SHA, value));
            processIfNotNull(extractHeadSha(json), value -> properties.put(FIELD_HEAD_SHA, value));

            result = Node.builder().type(TicketType.MERGE_REQUEST.getNodeType())
                         .uid(uid)
                         .name(name)
                         .properties(properties)
                         .build();
        }
        return result;
    }

    private String extractBaseSha(final JsonNode json) {
        final JsonNode jsonNode = json.get(FIELD_BASE);
        return extractSha(jsonNode);
    }

    private String extractHeadSha(final JsonNode json) {
        final JsonNode jsonNode = json.get(FIELD_HEAD);
        return extractSha(jsonNode);
    }

    private String extractSha(final JsonNode jsonNode) {
        String   result = null;
        JsonNode sha    = null;
        if (jsonNode != null && !jsonNode.isNull()) {
            sha = jsonNode.get("sha");
        }
        if (sha != null && !sha.isNull()) {
            result = sha.asText();
        }
        return result;
    }


    private String extract(final String key, final JsonNode json) {
        String result = null;
        if (json != null) {
            final JsonNode node = json.get(key);
            if (node != null && !node.isNull()) {
                result = node.asText();
            }
        }
        return result;
    }

    private String extractLabelName(final JsonNode labelNode) {
        String   result = null;
        JsonNode name   = null;
        if (labelNode != null && !labelNode.isNull()) {
            name = labelNode.get(FIELD_NAME);
        }
        if (name != null && !name.isNull()) {
            result = name.asText();
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private JsonNode callGitHub(final String fullUrl, final Map<String, String> headers,
                                final ObjectMapper objectMapper) {
        JsonNode result = CacheUtils.get(fullUrl);
        if (result == null) {
            HttpConnectorResult      httpResult = null;
            final HttpBasicConnector http       = new HttpBasicConnector();
            try {
                log.info("calling {}", fullUrl);

                httpResult = http.get(fullUrl, headers);
            }
            catch (final Exception e) {
                log.error(e.getMessage(), e);
            }
            finally {
                log.debug("[{}]{} ({}ms)", httpResult == null ? 500 : httpResult.getStatusCode(), fullUrl,
                          httpResult == null ? 0 : httpResult.getDelais());
                http.close();
            }

            if (httpResult == null || httpResult.getStatusCode() != 200) {
                log.error("can't call : {}", fullUrl);
            }
            else {
                try {
                    result = objectMapper.readTree(new String(httpResult.getData()));
                }
                catch (final JsonProcessingException e) {
                    log.error("can't read response from : {}\npayload:{}", fullUrl, new String(httpResult.getData()));
                }
            }
            if (result != null) {
                CacheUtils.put(fullUrl, result);
            }
        }
        else {
            log.info("loading github information from cache");
        }


        return result;
    }
}
