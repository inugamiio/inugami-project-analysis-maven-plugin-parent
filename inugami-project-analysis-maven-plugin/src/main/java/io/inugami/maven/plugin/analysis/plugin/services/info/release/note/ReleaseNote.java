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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import io.inugami.api.models.JsonBuilder;
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
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.*;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.isNotNull;

@Slf4j
public class ReleaseNote implements ProjectInformation, QueryConfigurator {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.display.release.note";
    public static final String MODE_FULL    = FEATURE_NAME + ".full";
    public static final String REPLACEMENTS = FEATURE_NAME + ".emails.replacements";

    public static final  String       SCM              = "scm";
    private static final List<String> QUERIES          = List.of(
            "META-INF/queries/search_release_note_simple.cql",
            "META-INF/queries/search_release_note_full.cql"
                                                                );
    public static final  String       COMMIT           = "commit";
    public static final  String       MERGE_REQUEST    = "mergeRequest";
    public static final  String       TITLE            = "title";
    public static final  String       MERGED_AT        = "merged_at";
    public static final  String       URL              = "url";
    public static final  String       SHORT_NAME       = "shortName";
    public static final  String       LINE_DECO        = "-";
    public static final  String       CREATED_AT       = "created_at";
    public static final  String       ISSUE            = "issue";
    public static final  String       ISSUE_LABEL      = "issueLabel";
    public static final  String       ISSUE_LINK       = "issueLink";
    public static final  String       ISSUE_LINK_LABEL = "issueLinkLabel";
    public static final  String       ISSUES           = "Issues";
    public static final  String       MERGE_REQUESTS   = "Merge request";
    public static final  String       AUTHOR           = "author";
    public static final  String       NAME             = "name";
    public static final  String       EMAIL            = "email";
    public static final  String       AUTHORS          = "Authors";


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
                Map.entry("version", gav.getVersion())
                                   ));
        return config;
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final Neo4jDao dao       = new Neo4jDao(configuration);
        final String   queryPath = selectQuery(configuration);
        final Gav      gav       = convertMavenProjectToGav(project);

        final String query = TemplateRendering.render(QueriesLoader.getQuery(queryPath),
                                                      configure(queryPath,
                                                                gav,
                                                                configuration));

        log.info("query:\n{}", query);
        final List<Record>      resultSet         = dao.search(query);
        final ReleaseNoteResult releaseNoteResult = new ReleaseNoteResult();
        final List<Replacement> replacements      = buildReplacements(configuration);
        convertToReleaseNote(releaseNoteResult, resultSet, replacements);
        final String json = convertToJson(releaseNoteResult);
        if (log.isDebugEnabled()) {
            log.info("release note:\n{}", json);
        }
        writeJson(json, project.getBasedir());

        final JsonBuilder writer = new JsonBuilder();
        rendering(releaseNoteResult, writer, configuration);

        log.info("\n{}", writer.toString());

        dao.shutdown();
    }


    // =========================================================================
    // CONVERT
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
        addScm(releaseNote, (NodeValue) record.get(SCM), cache, replacements);
        addMergeRequest(releaseNote, (NodeValue) record.get(MERGE_REQUEST), cache);
        addIssues(releaseNote, (NodeValue) record.get(ISSUE), (NodeValue) record.get(ISSUE_LABEL));
        addIssues(releaseNote, (NodeValue) record.get(ISSUE_LINK), (NodeValue) record.get(ISSUE_LINK_LABEL));
        addAuthors(releaseNote, (NodeValue) record.get(AUTHOR), cache, replacements);
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
                savedIssue.addLabel(String.valueOf(shortName).replaceAll(" ", "_"));
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
                                    .email(retrieveString(EMAIL, data, replacements))
                                    .build());
    }

    // =========================================================================
    // WRITE JSON
    // =========================================================================
    private void writeJson(final String json, final File basedir) {

    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    private void rendering(final ReleaseNoteResult releaseNoteResult, final JsonBuilder writer,
                           final ConfigHandler<String, String> configuration) {

        renderingCommit(releaseNoteResult.getCommit(), writer);
        renderingMergeRequest(releaseNoteResult.getMergeRequests(), writer, configuration);
        renderingIssues(releaseNoteResult.getIssues(), writer, configuration);
        renderingAuthors(releaseNoteResult.getAuthors(), writer, configuration);
    }


    private void renderingCommit(final Set<String> commit, final JsonBuilder writer) {
        if (commit != null && !commit.isEmpty()) {
            final List<String> lines = new ArrayList<>(commit);
            Collections.sort(lines);

            writer.write(ConsoleColors.CYAN);
            writer.write(ConsoleColors.createLine(LINE_DECO, 80)).line();
            writer.write("SCM").line();
            writer.write(ConsoleColors.createLine(LINE_DECO, 80)).line();
            writer.write(ConsoleColors.RESET);

            for (final String line : lines) {
                writer.write(chooseCommitLineColor(line));
                writer.write(line);
                writer.write(ConsoleColors.RESET);
                writer.line();
            }
        }
    }

    private String chooseCommitLineColor(final String line) {
        String result = "";
        if (line != null) {
            final String lowerCase = line.toLowerCase();
            if (lowerCase.contains("merge branch") || line.contains("merge pull request")) {
                result = ConsoleColors.CYAN;
            }
            else if (lowerCase.contains("fix")) {
                result = ConsoleColors.RED;
            }
            else if (lowerCase.contains("prepare for next development") || lowerCase.contains("prepare release")) {
                result = ConsoleColors.YELLOW;
            }
        }

        return result;
    }

    private void renderingMergeRequest(final List<MergeRequests> mergeRequests, final JsonBuilder writer,
                                       final ConfigHandler<String, String> configuration) {
        if (mergeRequests != null && !mergeRequests.isEmpty()) {

            final Map<String, Collection<DataRow>> data = new LinkedHashMap<>();
            final List<DataRow>                    rows = new ArrayList<>();
            for (final MergeRequests merge : mergeRequests) {
                final Map<String, Serializable> properties = new LinkedHashMap<>();
                final DataRow                   row        = new DataRow();

                row.setUid(merge.getDate());

                properties.put("date", merge.getDate());
                properties.put("title", merge.getTitle());
                properties.put("url", merge.getUrl());
                row.setProperties(properties);
                rows.add(row);
            }
            data.put(MERGE_REQUESTS, rows);
            writer.line();
            writer.write(Neo4jRenderingUtils.rendering(data, configuration, MERGE_REQUESTS));
        }
    }

    private void renderingIssues(final List<Issue> issues, final JsonBuilder writer,
                                 final ConfigHandler<String, String> configuration) {
        if (issues != null && !issues.isEmpty()) {

            final Map<String, Collection<DataRow>> data = new LinkedHashMap<>();
            final List<DataRow>                    rows = new ArrayList<>();
            for (final Issue issue : issues) {
                final Map<String, Serializable> properties = new LinkedHashMap<>();
                final DataRow                   row        = new DataRow();
                final String labels = issue.getLabels() == null || issue.getLabels()
                                                                        .isEmpty() ? "" : String
                                              .join(" ", issue.getLabels());
                row.setUid(issue.getName());
                row.setRowColor(chooseIssueColor(labels));
                properties.put("date", issue.getDate());
                properties.put("title", issue.getTitle());
                properties.put("labels", labels);
                properties.put("url", issue.getUrl());
                row.setProperties(properties);
                rows.add(row);
            }
            data.put(ISSUES, rows);
            writer.line();
            writer.write(Neo4jRenderingUtils.rendering(data, configuration, ISSUES));
        }

    }

    private String chooseIssueColor(final String labels) {
        String       result = "";
        final String value  = labels.toLowerCase();
        if (value.contains("epic")) {
            result = ConsoleColors.RED;
        }
        else if (value.contains("feature") || value.contains("story")) {
            result = ConsoleColors.GREEN;
        }
        return result;
    }


    private void renderingAuthors(final Set<Author> authors, final JsonBuilder writer,
                                  final ConfigHandler<String, String> configuration) {
        if (authors != null && !authors.isEmpty()) {

            final Map<String, Collection<DataRow>> data = new LinkedHashMap<>();
            final List<DataRow>                    rows = new ArrayList<>();
            for (final Author author : authors) {
                final Map<String, Serializable> properties = new LinkedHashMap<>();
                final DataRow                   row        = new DataRow();

                row.setUid(author.getEmail());
                properties.put(NAME, author.getName());
                properties.put(EMAIL, author.getEmail());
                row.setProperties(properties);
                rows.add(row);
            }
            data.put(AUTHORS, rows);
            writer.line();
            writer.write(Neo4jRenderingUtils.rendering(data, configuration, AUTHORS));
        }
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private String selectQuery(final ConfigHandler<String, String> configuration) {
        final boolean isFullMode = Boolean.parseBoolean(configuration.grabOrDefault(MODE_FULL, "false"));
        return isFullMode ? QUERIES.get(1) : QUERIES.get(0);
    }

    private String convertToJson(final ReleaseNoteResult releaseNoteResult) {
        try {
            return ObjectMapperBuilder.build().writerWithDefaultPrettyPrinter().writeValueAsString(releaseNoteResult);
        }
        catch (final JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

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

    private String retrieveString(final String key, final Map<String, Object> data) {
        String result = null;
        if (data != null && data.containsKey(key)) {
            result = String.valueOf(data.get(key));
        }
        return result;
    }

    private String retrieveString(final String key, final Map<String, Object> data,
                                  final List<Replacement> replacements) {
        String result = null;
        if (data != null && data.containsKey(key)) {
            result = String.valueOf(data.get(key));
        }

        result = replace(result, replacements);
        return result;
    }


    private List<Replacement> buildReplacements(final ConfigHandler<String, String> configuration) {
        final List<Replacement> result = new ArrayList<>();
        final String            config = configuration.getOrDefault(REPLACEMENTS, null);
        if (config != null) {
            try {
                final List<ReplacementConfig> data = ObjectMapperBuilder.build().readValue(config,
                                                                                           new TypeReference<List<ReplacementConfig>>() {
                                                                                           });
                for (final ReplacementConfig replacementConfig : data) {
                    result.add(Replacement.builder()
                                          .pattern(Pattern.compile(replacementConfig.getFrom()))
                                          .replacement(replacementConfig.getTo())
                                          .build()
                              );
                }
            }
            catch (final JsonProcessingException e) {
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }

    private String replace(final String input, final List<Replacement> replacements) {
        String result = input;
        if (result != null && replacements != null) {
            for (final Replacement replacement : replacements) {
                final Matcher matcher = replacement.getPattern().matcher(result);
                if (matcher.matches()) {
                    result = matcher.replaceAll(replacement.getReplacement());
                }
            }
        }
        return result;
    }

}
