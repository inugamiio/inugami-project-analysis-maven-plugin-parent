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
package io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.jira;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.inugami.api.tools.StringTools;
import io.inugami.commons.connectors.HttpBasicConnector;
import io.inugami.commons.connectors.HttpConnectorResult;
import io.inugami.maven.plugin.analysis.api.connectors.HttpConnectorBuilder;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.scan.issue.tracker.JiraCustomFieldsAppender;
import io.inugami.maven.plugin.analysis.api.utils.CacheUtils;
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.IssueTrackerCommons;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.git.issue.trackers.IssueTrackerCommons.*;

@Slf4j
@RequiredArgsConstructor
public class JiraTask implements Callable<ScanNeo4jResult> {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================

    public static final  String                         REST_ISSUE_PATH       = "/rest/api/2/issue/";
    public static final  String                         AUTHORIZATION         = "Authorization";
    private static final String                         FIELD_KEY             = "key";
    public static final  String                         FIELDS                = "fields";
    public static final  String                         NAME                  = "name";
    public static final  String                         ISSUE_PREFIX          = "issue_";
    public static final  String                         FIELD_CREATED         = "created";
    public static final  String                         FIELD_ISSUE_TYPE      = "issueType";
    public static final  String                         DISPLAY_ISSUE_PATH    = "browse";
    public static final  String                         FIELD_ISSUE_TYPE_NODE = "issuetype";
    public static final  String                         FIELD_SUMMARY         = "summary";
    public static final  String                         LABELS                = "labels";
    public static final  String                         ISSUE_LINKS           = "issuelinks";
    public static final  String                         FIELD_OUTWARD_ISSUE   = "outwardIssue";
    public static final  String                         FIELD_SUBTASKS        = "subtasks";
    public static final  String                         CHARSET               = "charset";
    public static final  String                         HEADER_SEP            = ";";
    public static final  String                         HEADER_VALUE_SEP      = "=";
    private final        String                         id;
    private final        String                         username;
    private final        String                         password;
    private final        String                         url;
    private final        HttpConnectorBuilder           httpConnectorBuilder;
    private final        String                         versionUid;
    private final        List<JiraCustomFieldsAppender> customFieldsAppenders;

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public ScanNeo4jResult call() throws Exception {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();
        final JsonNode        json   = callJira(id);

        Node node = null;
        if (json != null) {
            node = buildNode(json, result, true);
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

    // =========================================================================
    // BUILD NODE
    // =========================================================================
    private Node buildNode(final JsonNode json,
                           final ScanNeo4jResult result,
                           final boolean enableLinks) {

        final String name = extract(FIELD_KEY, json);

        final JsonNode                            fields      = json.get(FIELDS);
        String                                    projectName = null;
        final LinkedHashMap<String, Serializable> properties  = new LinkedHashMap<>();

        if (isNotNull(fields)) {
            projectName = extractProjectName(fields);
        }
        final String uid = ISSUE_PREFIX + (projectName == null ? "" : projectName + "_") + name;

        processIfNotNull(extractIssueType(fields), value -> properties.put(FIELD_ISSUE_TYPE, value));
        processIfNotNull(extract(FIELD_SUMMARY, fields), value -> properties.put(FIELD_TITLE, value));
        processIfNotNull(extract(FIELD_DESCRIPTION, fields), value -> properties.put(FIELD_DESCRIPTION, value));
        processIfNotNull(extract(FIELD_CREATED, json), value -> properties.put(FIELD_CREATED_AT, value));
        processIfNotNull(buildUrl(name), value -> properties.put(FIELD_URL, value));

        buildLabels(fields, uid, result);
        if (enableLinks) {
            buildIssueLinks(fields, uid, result);
            buildSubTasks(fields, uid, result);
        }

        addCustomFields(uid, json, properties, result);

        return Node.builder()
                   .name(name)
                   .uid(uid)
                   .type(IssueTrackerCommons.TicketType.ISSUE.getNodeType())
                   .properties(properties)
                   .build();
    }


    // =========================================================================
    // BUILDERS
    // =========================================================================
    private void addCustomFields(final String uid, final JsonNode json,
                                 final LinkedHashMap<String, Serializable> properties,
                                 final ScanNeo4jResult result) {
        try {
            for (final JiraCustomFieldsAppender appender : customFieldsAppenders) {
                appender.append(uid, json, properties, result);
            }
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    private String buildUrl(final String name) {
        return String.join("/", url, DISPLAY_ISSUE_PATH, name);
    }


    private void buildLabels(final JsonNode fields, final String uid,
                             final ScanNeo4jResult resultNeo4J) {
        final Set<String> labels = new LinkedHashSet<>();

        if (isNotNull(fields)) {
            processIfNotNull(extractIssueType(fields), labels::add);

            final JsonNode labelsNodes = fields.get(LABELS);
            if (isNotNull(labelsNodes) && labelsNodes.isArray()) {
                final Iterator<JsonNode> iterator = labelsNodes.iterator();
                while (iterator.hasNext()) {
                    final JsonNode labelsNode = iterator.next();
                    if (isNotNull(labelsNode) && labelsNode.isTextual()) {
                        labels.add(labelsNode.asText());
                    }
                }
            }
        }

        if (!labels.isEmpty()) {
            for (final String label : labels) {
                final String labelUid = TicketType.ISSUE_LABEL.getNodePrefix() + label;

                resultNeo4J.addNode(Node.builder()
                                        .type(IssueTrackerCommons.TicketType.ISSUE_LABEL.getNodeType())
                                        .uid(labelUid)
                                        .name(label)
                                        .build());

                resultNeo4J.addRelationship(Relationship.builder()
                                                        .from(uid)
                                                        .to(labelUid)
                                                        .type(HAS_LABEL)
                                                        .build());
            }
        }
    }

    private void buildIssueLinks(final JsonNode fields, final String uid, final ScanNeo4jResult result) {
        JsonNode issueLinksNode = null;
        if (isNotNull(fields)) {
            issueLinksNode = fields.get(ISSUE_LINKS);
        }
        if (issueLinksNode == null) {
            return;
        }

        if (isNotNull(issueLinksNode) && issueLinksNode.isArray()) {
            final Iterator<JsonNode> iterator = issueLinksNode.iterator();
            while (iterator.hasNext()) {
                final JsonNode linkNode = iterator.next();
                if (isNotNull(linkNode)) {
                    try {
                        buildIssueLink(linkNode, uid, result);
                    } catch (final Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            }
        }
    }

    private void buildIssueLink(final JsonNode linkNode, final String uid, final ScanNeo4jResult result) {
        final JsonNode linkInfo = linkNode.get(FIELD_OUTWARD_ISSUE);
        String         linkId   = null;
        if (isNotNull(linkInfo)) {
            linkId = extract(FIELD_KEY, linkInfo);
        }
        JsonNode json = null;
        if (linkId != null) {
            json = callJira(linkId);
        }

        Node linkNeo4jNode = null;
        if (json != null) {
            linkNeo4jNode = buildNode(json, result, false);
        }

        if (linkNeo4jNode != null) {
            result.addNode(linkNeo4jNode);
            result.addRelationship(
                    Relationship.builder()
                                .from(uid)
                                .to(linkNeo4jNode.getUid())
                                .type(HAS_ISSUE_LINK)
                                .build()
            );
        }
    }

    private void buildSubTasks(final JsonNode fields, final String uid, final ScanNeo4jResult result) {
        JsonNode subtasks = null;
        if (isNotNull(fields)) {
            subtasks = fields.get(FIELD_SUBTASKS);
        }
        if (subtasks == null) {
            return;
        }

        if (isNotNull(subtasks) && subtasks.isArray()) {
            final Iterator<JsonNode> iterator = subtasks.iterator();
            while (iterator.hasNext()) {
                final JsonNode subtask = iterator.next();
                if (isNotNull(subtask)) {
                    try {

                    } catch (final Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    buildSubTask(subtask, uid, result);
                }
            }
        }
    }

    private void buildSubTask(final JsonNode jsonNode, final String uid, final ScanNeo4jResult result) {
        final String subtaskId = extract(FIELD_KEY, jsonNode);
        JsonNode     json      = null;
        if (subtaskId != null) {
            json = callJira(subtaskId);
        }

        Node subTaskNode = null;
        if (json != null) {
            subTaskNode = buildNode(json, result, false);
        }

        if (subTaskNode != null) {
            result.addNode(subTaskNode);
            result.addRelationship(
                    Relationship.builder()
                                .from(uid)
                                .to(subTaskNode.getUid())
                                .type(HAS_ISSUE_LINK)
                                .build()
            );
        }
    }

    // =========================================================================
    // EXTRACTORS
    // =========================================================================
    private String extractProjectName(final JsonNode jsonNode) {
        String         result  = null;
        final JsonNode project = jsonNode.get("project");
        if (isNotNull(project)) {
            result = extract(NAME, project);
        }
        return result;
    }

    private String extractIssueType(final JsonNode fields) {
        String   result        = null;
        JsonNode issueTypeNode = null;
        if (isNotNull(fields)) {
            issueTypeNode = fields.get(JiraTask.FIELD_ISSUE_TYPE_NODE);
        }
        if (isNotNull(issueTypeNode)) {
            result = extract(NAME, issueTypeNode);
        }
        if (result != null) {
            result = StringTools.convertToAscii(result).trim().toLowerCase();
        }
        return result;
    }

    // =========================================================================
    // CALL JIRA
    // =========================================================================
    private JsonNode callJira(final String issueId) {
        final StringBuilder fullUrl = new StringBuilder();
        fullUrl.append(url);
        fullUrl.append(REST_ISSUE_PATH);
        fullUrl.append(issueId);

        final Map<String, String> headers = new HashMap<>();
        headers.put(AUTHORIZATION, buildBasicAuth());
        return invokeHttp(fullUrl.toString(), headers);
    }


    private String buildBasicAuth() {
        final String token = Base64.getEncoder().encodeToString(String.join(":", username, password).getBytes());
        return "Basic " + token;
    }

    private JsonNode invokeHttp(final String fullUrl, final Map<String, String> headers) {
        JsonNode result = CacheUtils.get(fullUrl);

        if (result == null) {
            HttpConnectorResult      httpResult = null;
            final HttpBasicConnector http       = httpConnectorBuilder.buildHttpConnector();
            try {
                log.info("calling {}", fullUrl);

                httpResult = http.get(fullUrl, headers);
            } catch (final Exception e) {
                log.error(e.getMessage(), e);
            } finally {
                log.debug("[{}]{} ({}ms)", httpResult == null ? 500 : httpResult.getStatusCode(), fullUrl,
                          httpResult == null ? 0 : httpResult.getDelais());
                http.close();
            }

            if (httpResult == null || httpResult.getStatusCode() != 200) {
                log.error("can't call : {}", fullUrl);
            } else {
                try {
                    final ObjectMapper objectMapper = ObjectMapperBuilder.build();
                    final String       content      = extractContent(httpResult);
                    result = objectMapper.readTree(content);
                } catch (final JsonProcessingException e) {
                    log.error("can't read response from : {}\npayload:{}", fullUrl, new String(httpResult.getData()));
                }
            }
            if (result != null) {
                CacheUtils.put(fullUrl, result);
            }
        } else {
            log.info("loading jira information from cache");
        }

        return result;
    }

    private String extractContent(final HttpConnectorResult httpResult) {

        Charset charset = extractResponseCharset(httpResult.getResponseHeaders());
        if (charset == null && httpResult.getCharset() != null) {
            charset = httpResult.getCharset();
        }
        return new String(httpResult.getData(), charset == null ? StandardCharsets.UTF_8 : charset);
    }

    protected Charset extractResponseCharset(final Map<String, String> responseHeaders) {
        Charset result      = null;
        String  contentType = null;
        try {
            if (responseHeaders != null) {
                for (final Map.Entry<String, String> header : responseHeaders.entrySet()) {
                    if (header.getKey().equalsIgnoreCase("content-type")) {
                        contentType = header.getValue();
                        break;
                    }
                }
            }

            String charset = null;
            if (contentType != null) {
                final String[] values = contentType.split(HEADER_SEP);
                for (final String value : values) {
                    if (value.toLowerCase().contains(CHARSET)) {
                        charset = value;
                        break;
                    }
                }
            }

            if (charset != null) {
                final String charsetValue = charset.split(HEADER_VALUE_SEP)[1].toUpperCase();
                result = Charset.forName(charsetValue);
            }
        } catch (final Throwable e) {
            log.error(e.getMessage(), e);
        }

        return result == null ? StandardCharsets.UTF_8 : result;
    }
}
