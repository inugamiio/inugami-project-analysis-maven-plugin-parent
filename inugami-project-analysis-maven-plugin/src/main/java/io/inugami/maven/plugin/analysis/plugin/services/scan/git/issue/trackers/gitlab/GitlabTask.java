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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.gitlab;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inugami.commons.connectors.HttpBasicConnector;
import io.inugami.commons.connectors.HttpConnectorResult;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.Callable;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

@Slf4j
@RequiredArgsConstructor
public class GitlabTask implements Callable<ScanNeo4jResult> {

    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_LABELS      = "labels";
    public static final String HAS_LABEL         = "HAS_LABEL";
    public static final String FIELD_IID         = "iid";
    public static final String UUID              = "id";
    public static final String FIELD_PROJECT_ID  = "project_id";
    public static final String HAS_ISSUE_LINK = "HAS_ISSUE_LINK";

    // =========================================================================
    // ENUM
    // =========================================================================
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    public static enum TicketType {
        MERGE_REQUEST("MergeRequest", "pr_"),
        ISSUE("Issue", "issue_"),
        ISSUE_LABEL("IssueLabel", "issue_label_");

        private final String nodeType;
        private final String nodePrefix;
    }

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FIELD_TITLE      = "title";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_MERGED_AT  = "merged_at";
    public static final String FIELD_CLOSED_AT  = "closed_at";
    public static final String FIELD_WEB_URL    = "web_url";
    public static final String FIELD_URL        = "url";
    public static final String FIELD_DIFF_REFS  = "diff_refs";
    public static final String FIELD_BASE_SHA   = "base_sha";
    public static final String FIELD_HEAD_SHA   = "head_sha";
    public static final String FIELD_START_SHA  = "start_sha";
    public static final String HAVE_TICKET      = "HAVE_TICKET";

    private final String             id;
    private final TicketType         ticketType;
    private final String             token;
    private final String             url;
    private final String             versionUid;


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public ScanNeo4jResult call() throws Exception {
        final ScanNeo4jResult     result       = ScanNeo4jResult.builder().build();
        final Map<String, String> headers      = Map.ofEntries(Map.entry("PRIVATE-TOKEN", token),
                                                               Map.entry("Content-Type", "application/json"));
        final ObjectMapper        objectMapper = ObjectMapperBuilder.build();
        Node                      node         = null;
        switch (ticketType) {
            case MERGE_REQUEST:
                node = processMergeRequest(headers, objectMapper);
                break;
            default:
                node = processIssue(headers, objectMapper, result);
                processIssueLinks(headers, objectMapper, node.getUid(), result);
                break;
        }

        if (node != null) {
            result.addNode(node);
            result.addRelationship(Relationship.builder()
                                               .from(versionUid)
                                               .to(node.getUid())
                                               .type(HAVE_TICKET)
                                               .build());
        }
        return result;
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private Node processIssue(final Map<String, String> headers, final ObjectMapper objectMapper,
                              final ScanNeo4jResult resultNeo4J) {
        final Node          result  = null;
        final StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(url);
        fullUrl.append("/issues/");
        fullUrl.append(id);

        final JsonNode json = callGitLab(fullUrl.toString(), headers, objectMapper);
        return buildIssueNode(json, resultNeo4J);
    }

    private Node buildIssueNode(final JsonNode json, final ScanNeo4jResult resultNeo4J) {
        Node result = null;
        if (json != null) {
            final String                              uid        = TicketType.ISSUE.getNodePrefix() + extract(FIELD_IID,
                                                                                                              json);
            final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();
            processIfNotNull(extract(UUID, json), value -> properties.put(UUID, value));
            processIfNotNull(extract(FIELD_PROJECT_ID, json), value -> properties.put(FIELD_PROJECT_ID, value));
            processIfNotNull(extract(FIELD_TITLE, json), value -> properties.put(FIELD_TITLE, value));
            processIfNotNull(extract(FIELD_DESCRIPTION, json), value -> properties.put(FIELD_DESCRIPTION, value));
            processIfNotNull(extract(FIELD_CREATED_AT, json), value -> properties.put(FIELD_CREATED_AT, value));
            processIfNotNull(extract(FIELD_WEB_URL, json), value -> properties.put(FIELD_URL, value));

            final JsonNode labels = json.get(FIELD_LABELS);
            if (labels != null && labels.isArray()) {
                final List<String> labelNames = new ArrayList<>();
                labels.spliterator().forEachRemaining(item -> {
                    if (!item.isNull()) {
                        labelNames.add(item.asText());
                    }
                });

                processIfNotNull(extract(FIELD_LABELS, json), value -> properties.put(FIELD_LABELS, value));

                for (final String label : labelNames) {
                    resultNeo4J.addNode(Node.builder()
                                            .type(TicketType.ISSUE_LABEL.getNodeType())
                                            .uid(TicketType.ISSUE_LABEL.getNodePrefix() + label)
                                            .name(label)
                                            .build());

                    resultNeo4J.addRelationship(Relationship.builder()
                                                            .from(uid)
                                                            .to(TicketType.ISSUE_LABEL.getNodePrefix() + label)
                                                            .type(HAS_LABEL)
                                                            .build());
                }
            }

            result = Node.builder().type(TicketType.ISSUE.getNodeType())
                         .uid(uid)
                         .name(uid)
                         .properties(properties)
                         .build();
        }

        return result;
    }

    private void processIssueLinks(final Map<String, String> headers, final ObjectMapper objectMapper,
                                   final String uid,
                                   final ScanNeo4jResult resultNeo4J) {

        final StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(url);
        fullUrl.append("/issues/");
        fullUrl.append(id);
        fullUrl.append("/links");

        final JsonNode json = callGitLab(fullUrl.toString(), headers, objectMapper);
        if (json != null && json.isArray()) {
            final Iterator<JsonNode> iterator = json.elements();
            while (iterator.hasNext()) {
                final JsonNode link          = iterator.next();
                final Node     linkNeo4JNode = buildIssueNode(link, resultNeo4J);
                if(linkNeo4JNode!=null){
                    resultNeo4J.addRelationship(Relationship.builder()
                                                            .from(uid)
                                                            .to(linkNeo4JNode.getUid())
                                                            .type(HAS_ISSUE_LINK)
                                                            .build());
                }
            }
        }
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private Node processMergeRequest(final Map<String, String> headers, final ObjectMapper objectMapper) {
        Node                result  = null;
        final StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(url);
        fullUrl.append("/merge_requests/");
        fullUrl.append(id);

        final JsonNode json = callGitLab(fullUrl.toString(), headers, objectMapper);
        if (json != null) {
            final String                              uid        = TicketType.MERGE_REQUEST.getNodePrefix() + id;
            final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();

            processIfNotNull(extract(FIELD_TITLE, json), value -> properties.put(FIELD_TITLE, value));
            processIfNotNull(extract(FIELD_DESCRIPTION, json), value -> properties.put(FIELD_DESCRIPTION, value));
            processIfNotNull(extract(FIELD_CREATED_AT, json), value -> properties.put(FIELD_CREATED_AT, value));
            processIfNotNull(extract(FIELD_MERGED_AT, json), value -> properties.put(FIELD_MERGED_AT, value));
            processIfNotNull(extract(FIELD_CLOSED_AT, json), value -> properties.put(FIELD_CLOSED_AT, value));
            processIfNotNull(extract(FIELD_WEB_URL, json), value -> properties.put(FIELD_URL, value));

            final JsonNode diffRefs = json.get(FIELD_DIFF_REFS);
            if (diffRefs != null) {
                processIfNotNull(extract(FIELD_BASE_SHA, diffRefs), value -> properties.put(FIELD_BASE_SHA, value));
                processIfNotNull(extract(FIELD_HEAD_SHA, diffRefs), value -> properties.put(FIELD_HEAD_SHA, value));
                processIfNotNull(extract(FIELD_START_SHA, diffRefs), value -> properties.put(FIELD_START_SHA, value));
            }
            result = Node.builder().type(TicketType.MERGE_REQUEST.getNodeType())
                         .uid(uid)
                         .name(uid)
                         .properties(properties)
                         .build();
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

    // =========================================================================
    // TOOLS
    // =========================================================================
    private JsonNode callGitLab(final String fullUrl, final Map<String, String> headers,
                                final ObjectMapper objectMapper) {
        JsonNode            result     = null;
        HttpConnectorResult httpResult = null;
        final HttpBasicConnector http = new HttpBasicConnector();
        try {
            log.info("calling {}", fullUrl);

                    httpResult = http.get(fullUrl, headers);
        }
        catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
        finally {
            log.debug("[{}]{} ({}ms)", httpResult == null ? 500 : httpResult.getStatusCode(), fullUrl,
                      httpResult==null?0:httpResult.getDelais());
            http.close();
        }

        if (httpResult == null || httpResult.getStatusCode() != 200) {
            log.error("can't call : {}", fullUrl.toString());
        }
        else {
            try {
                result = objectMapper.readTree(new String(httpResult.getData()));
            }
            catch (final JsonProcessingException e) {
                log.error("can't read response from : {}\npayload:{}", fullUrl, new String(httpResult.getData()));
            }
        }
        return result;
    }
}
