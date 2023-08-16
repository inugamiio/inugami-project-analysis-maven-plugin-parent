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
import io.inugami.api.spi.SpiLoader;
import io.inugami.api.tools.ConsoleColors;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteWriter;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.*;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import io.inugami.maven.plugin.analysis.api.tools.rendering.Neo4jRenderingUtils;
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.DefaultNeo4jDao;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_RELEASE_NOTE_FULL_CQL;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_RELEASE_NOTE_SIMPLE_CQL;

@Slf4j
public class ReleaseNote implements ProjectInformation, QueryConfigurator {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.display.release.note";
    public static final String TOGGLE       = FEATURE_NAME + ".enabled";
    public static final String MODE_FULL    = FEATURE_NAME + ".full";
    public static final String REPLACEMENTS = FEATURE_NAME + ".emails.replacements";


    private static final List<String> QUERIES = List.of(
            QUERIES_SEARCH_RELEASE_NOTE_SIMPLE_CQL,
            QUERIES_SEARCH_RELEASE_NOTE_FULL_CQL
    );
    public static final  String       TAB     = "\t";


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
        final ConfigHandler<String, String> configuration = context.getConfiguration();

        if (shouldSkip(configuration)) {
            return;
        }

        final String previousVersion = configuration.grabOrDefault(PREVIOUS_VERSION, null);

        final DefaultNeo4jDao dao         = new DefaultNeo4jDao(configuration);
        final Gav             gav         = convertMavenProjectToGav(context.getProject());
        final Gav             previousGav = buildPreviousGav(gav, previousVersion);


        final ReleaseNoteResult releaseNoteResult = new ReleaseNoteResult();
        final List<Replacement> replacements      = buildReplacements(configuration);


        final List<ReleaseNoteExtractor> extractors = SpiLoader.getInstance()
                                                               .loadSpiServicesByPriority(ReleaseNoteExtractor.class);
        for (final ReleaseNoteExtractor extractor : extractors) {
            try {
                log.info("invoke extractor : {}", extractor.getClass().getName());
                extractor.extractInformation(releaseNoteResult,
                                             gav,
                                             previousGav,
                                             dao,
                                             replacements,
                                             context
                );
            } catch (final Exception error) {
                log.error(error.getMessage(), error);
            }
        }

        if (log.isDebugEnabled()) {
            log.info("release note:\n{}", convertToJson(releaseNoteResult));
        }
        writeReleaseNote(releaseNoteResult, context);

        final JsonBuilder writer = new JsonBuilder();
        rendering(releaseNoteResult, writer, configuration);

        log.info("\n{}", writer.toString());

        dao.shutdown();
    }

    private boolean shouldSkip(final ConfigHandler<String, String> configuration) {
        final String toggle = configuration.get(TOGGLE);
        return !(toggle == null || "".equals(toggle) || Boolean.parseBoolean(toggle));
    }

    private Gav buildPreviousGav(final Gav gav, final String previousVersion) {
        Gav result = null;
        if (previousVersion != null) {
            result = gav.toBuilder().version(previousVersion).build();
        }
        return result;
    }

    // =========================================================================
    // WRITE JSON
    // =========================================================================
    private void writeReleaseNote(final ReleaseNoteResult releaseNoteResult,
                                  final InfoContext context) {
        final List<ReleaseNoteWriter> writers = SpiLoader.getInstance().loadSpiServicesByPriority(ReleaseNoteWriter.class);

        for (final ReleaseNoteWriter writer : writers) {
            if (writer.accept(context.getConfiguration())) {
                try {
                    log.info(inColor("invoke writer : {}", ConsoleColors.BLUE_BOLD), writer.getClass().getName());
                    writer.process(releaseNoteResult, context);
                } catch (final Exception error) {
                    log.error(error.getMessage(), error);
                }

            } else {
                log.info(inColor("writer disabled : {}", ConsoleColors.YELLOW_BOLD), writer.getClass().getName());
            }
        }
    }

    private String inColor(final String message, final String color) {
        return color + message + ConsoleColors.RESET;
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


    private void renderingCommit(final Set<Map<String, Object>> commit, final JsonBuilder writer) {
        if (commit != null && !commit.isEmpty()) {


            final List<String> lines = buildCommit(commit);
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

    private List<String> buildCommit(final Set<Map<String, Object>> commit) {
        final List<String> result = new ArrayList<>();
        if (commit != null) {

            for (final Map<String, Object> item : commit) {
                final StringBuilder buffer = new StringBuilder();
                buffer.append(orEmpty(item, "date")).append(TAB);
                buffer.append(orEmpty(item, "commitUid")).append(TAB);
                buffer.append(orEmpty(item, "author")).append(TAB);
                buffer.append(orEmpty(item, "message"));
                result.add(buffer.toString());
            }
        }
        return result;
    }

    private String orEmpty(final Map<String, Object> item, final String key) {
        String result = null;
        if (item != null) {
            final Object value = item.get(key);
            result = value == null ? "" : String.valueOf(value);
        }
        return result;
    }

    private String chooseCommitLineColor(final String line) {
        String result = "";
        if (line != null) {
            final String lowerCase = line.toLowerCase();
            if (lowerCase.contains("merge branch") || line.contains("merge pull request")) {
                result = ConsoleColors.CYAN;
            } else if (lowerCase.contains("fix")) {
                result = ConsoleColors.RED;
            } else if (lowerCase.contains("prepare for next development") || lowerCase.contains("prepare release")) {
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
        } else if (value.contains("feature") || value.contains("story")) {
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
    private String convertToJson(final ReleaseNoteResult releaseNoteResult) {
        try {
            return ObjectMapperBuilder.build().writerWithDefaultPrettyPrinter().writeValueAsString(releaseNoteResult);
        } catch (final JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return null;
        }
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
            } catch (final JsonProcessingException e) {
                log.error(e.getMessage(), e);
            }
        }
        return result;
    }


}
