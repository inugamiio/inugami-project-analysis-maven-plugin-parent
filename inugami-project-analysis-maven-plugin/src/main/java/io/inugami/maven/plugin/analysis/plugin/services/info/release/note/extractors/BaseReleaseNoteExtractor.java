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

import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.*;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.api.utils.Constants;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.isNotNull;
import static io.inugami.maven.plugin.analysis.api.utils.Constants.*;
import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_RELEASE_NOTE_SIMPLE_CQL;

@Slf4j
public class BaseReleaseNoteExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String SCM              = "scm";
    public static final String MERGE_REQUEST    = "mergeRequest";
    public static final String ISSUE            = "issue";
    public static final String ISSUE_LABEL      = "issueLabel";
    public static final String ISSUE_LINK       = "issueLink";
    public static final String ISSUE_LINK_LABEL = "issueLinkLabel";
    public static final String AUTHOR           = "author";
    public static final String COMMIT           = "commit";
    public static final String TITLE            = "title";
    public static final String MERGED_AT        = "merged_at";
    public static final String URL              = "url";
    public static final String SHORT_NAME       = "shortName";
    public static final String CREATED_AT       = "created_at";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult,
                                   final Gav currentVersion,
                                   final Gav previousVersion,
                                   final Neo4jDao dao,
                                   final List<Replacement> replacements,
                                   final InfoContext context) {

        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(context.getConfiguration());
        config.putAll(Map.ofEntries(
                Map.entry(GROUP_ID, currentVersion.getGroupId()),
                Map.entry(ARTIFACT_ID, currentVersion.getArtifactId()),
                Map.entry(VERSION, currentVersion.getVersion())
                                   ));
        final String query = TemplateRendering.render(QueriesLoader.getQuery(QUERIES_SEARCH_RELEASE_NOTE_SIMPLE_CQL),
                                                      config);

        log.info("query:\n{}", query);
        final List<Record> resultSet = dao.search(query);


        releaseNoteResult.setGav(GavInfo.builder()
                                        .groupId(currentVersion.getGroupId())
                                        .artifactId(currentVersion.getArtifactId())
                                        .version(currentVersion.getVersion())
                                        .scanDate(LocalDateTime.now())
                                        .build());
        convertToReleaseNote(releaseNoteResult, resultSet, replacements);
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private void convertToReleaseNote(
            final ReleaseNoteResult releaseNote,
            final List<Record> resultSet,
            final List<Replacement> replacements) {

        final Map<String, Serializable> cache = new HashMap<>();
        if (resultSet != null && !resultSet.isEmpty()) {
            for (final Record record : resultSet) {
                convertReleaseNote(releaseNote, record, cache, replacements);
            }
        }
    }

    private void convertReleaseNote(final ReleaseNoteResult releaseNote, final Record record,
                                    final Map<String, Serializable> cache,
                                    final List<Replacement> replacements) {
        //@formatter:off
        processIfNotNull(Neo4jUtils.extractNode(SCM, record), value           -> addScm(releaseNote, value, cache, replacements));
        processIfNotNull(Neo4jUtils.extractNode(MERGE_REQUEST, record), value -> addMergeRequest(releaseNote, value, cache));
        processIfNotNull(Neo4jUtils.extractNode(ISSUE, record), value         -> addIssues(releaseNote, value, (NodeValue) record.get(ISSUE_LABEL)));
        processIfNotNull(Neo4jUtils.extractNode(ISSUE_LINK, record), value    -> addIssues(releaseNote, value, (NodeValue) record.get(ISSUE_LINK_LABEL)));
        processIfNotNull(Neo4jUtils.extractNode(AUTHOR, record), value        -> addAuthors(releaseNote, value, cache, replacements));
        //@formatter:on
    }


    private void addScm(final ReleaseNoteResult releaseNote, final NodeValue scm,
                        final Map<String, Serializable> cache,
                        final List<Replacement> replacements) {

        final String cacheKey = skipNode(scm, cache, SCM);
        if (cacheKey == null) {
            return;
        }

        cache.put(cacheKey, Boolean.TRUE);
        final Map<String, Object> data   = scm.asMap();
        final String              commit = data.containsKey(COMMIT) ? String.valueOf(data.get(COMMIT)) : null;


        if (commit != null) {
            for (final String commitLine : commit.split("\n")) {
                if (commitLine != null) {
                    releaseNote.addCommit(replace(commitLine, replacements));
                }
            }

        }
    }


    private void addMergeRequest(final ReleaseNoteResult releaseNote, final NodeValue node,
                                 final Map<String, Serializable> cache) {
        final String cacheKey = skipNode(node, cache, MERGE_REQUEST);
        if (cacheKey == null) {
            return;
        }
        cache.put(cacheKey, Boolean.TRUE);
        final Map<String, Object> data = node.asMap();

        releaseNote.addMergeRequest(MergeRequests.builder()
                                                 .uid(retrieveString(SHORT_NAME, data))
                                                 .title(retrieveString(TITLE, data))
                                                 .date(retrieveString(MERGED_AT, data))
                                                 .url(retrieveString(URL, data))
                                                 .build());
    }

    private void addIssues(final ReleaseNoteResult releaseNote, final NodeValue nodeValue,
                           final NodeValue labelNode) {
        Issue savedIssue = null;
        if (isNotNull(nodeValue)) {
            final Map<String, Object> data = nodeValue.asMap();
            final Issue issue = Issue.builder()
                                     .name(retrieveString(SHORT_NAME, data))
                                     .title(retrieveString(TITLE, data))
                                     .date(retrieveString(CREATED_AT, data))
                                     .url(retrieveString(URL, data))
                                     .build();


            if (releaseNote.getIssues().contains(issue)) {
                savedIssue = releaseNote.getIssues().get(releaseNote.getIssues().indexOf(issue));
            }
            else {
                releaseNote.addIssue(issue);
                savedIssue = issue;
            }
        }

        if (savedIssue != null && isNotNull(labelNode)) {
            final Map<String, Object> data = labelNode.asMap();

            final Object shortName = data.get(SHORT_NAME);
            if (shortName != null) {
                savedIssue.addLabel(String.valueOf(shortName).replaceAll(EMPTY, UNDERSCORE));
            }

        }
    }

    private void addAuthors(final ReleaseNoteResult releaseNote, final NodeValue node,
                            final Map<String, Serializable> cache,
                            final List<Replacement> replacements) {
        final String cacheKey = skipNode(node, cache, AUTHOR);
        if (cacheKey == null) {
            return;
        }
        cache.put(cacheKey, Boolean.TRUE);
        final Map<String, Object> data = node.asMap();
        releaseNote.addAuthor(Author.builder()
                                    .name(retrieveString(SHORT_NAME, data, replacements))
                                    .email(retrieveString(Constants.EMAIL, data, replacements))
                                    .build());
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private String skipNode(final NodeValue scm, final Map<String, Serializable> cache, final String cachePrefix) {
        if (!isNotNull(scm)) {
            return null;
        }
        final String cacheKey = String.join("_", cachePrefix, String.valueOf(scm.asNode().id()));

        if (cache.containsKey(cacheKey)) {
            return null;
        }
        return cacheKey;
    }

}
