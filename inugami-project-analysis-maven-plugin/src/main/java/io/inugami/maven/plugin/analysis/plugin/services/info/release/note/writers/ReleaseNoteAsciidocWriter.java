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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers;

import edu.emory.mathcs.backport.java.util.Collections;
import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.files.FilesUtils;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteWriter;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Author;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Issue;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.MergeRequests;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ReleaseNoteAsciidocWriter implements ReleaseNoteWriter {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME    = "inugami.maven.plugin.analysis.display.release.note.asciidoc";
    public static final String BASE_DOC_FOLDER = FEATURE_NAME + ".baseDir";
    public static final String SPLIT_FILE      = FEATURE_NAME + ".splitFile";

    private static final Pattern COMMIT_REGEX = Pattern
            .compile("(?:\\[([^]]+)\\])(?:\\[([^]]+)\\])(?:\\[([^]]+)\\])(.*)");
    public static final  String  RELEASE_NOTE = "release-note";
    public static final  String  ADOC         = ".adoc";
    public static final  String  DELIMITER    = "-";

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final ConfigHandler<String, String> configuration) {
        return Boolean.parseBoolean(configuration.grabOrDefault(FEATURE_NAME, "false"));
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final ReleaseNoteResult releaseNote, final InfoContext context) {

        final File baseDocFolder = buildBaseDocFolder(context.getBasedir(), context.getConfiguration());

        try {
            render(releaseNote, baseDocFolder, context);
        }
        catch (final IOException e) {
            log.error(e.getMessage(), e);
        }
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private File buildBaseDocFolder(final File basedir,
                                    final ConfigHandler<String, String> configuration) {

        final String path = configuration.getOrDefault(BASE_DOC_FOLDER,
                                                       FilesUtils.buildPath(basedir, "src", "doc", "releases"));

        final File folder = new File(path);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    private void render(final ReleaseNoteResult releaseNote, final File baseDocFolder,
                        final InfoContext context) throws IOException {
        final ConfigHandler<String, String> config  = context.getConfiguration();
        final String                        version = context.getProject().getVersion();

        final boolean notSplitFile = !Boolean.parseBoolean(config.grabOrDefault(SPLIT_FILE, "false"));
        final Writer  writer       = buildWriter(baseDocFolder, version, notSplitFile);

        final String project = renderProject(context.getProject());
        final String authors = renderAuthors(releaseNote.getAuthors(), notSplitFile);
        final String commit  = renderCommit(releaseNote.getCommit(), notSplitFile);
        final String pr      = renderMergeRequest(releaseNote.getMergeRequests(), notSplitFile);
        final String issues  = renderIssues(releaseNote.getIssues(), notSplitFile);

        write(project, writer, "project", version, baseDocFolder);
        write(authors, writer, "author", version, baseDocFolder);
        write(issues, writer, "issues", version, baseDocFolder);
        write(pr, writer, "merge-requests", version, baseDocFolder);
        write(commit, writer, "commit", version, baseDocFolder);

        if(writer!=null){
            writer.close();
        }
    }



    private void write(final String content,
                       final Writer writer,
                       final String context,
                       final String version,
                       final File baseDocFolder) throws IOException {
        if (writer != null) {
            writer.write(content);
            writer.flush();
        }
        else {
            final File file = FilesUtils.buildFile(baseDocFolder,
                                                   version,
                                                   String.join(DELIMITER, RELEASE_NOTE, context+ADOC));
            FilesUtils.write(content, file);
        }
    }

    private Writer buildWriter(final File baseDocFolder, final String version,
                               final boolean notSplitFile) throws IOException {
        Writer writer = null;
        if (notSplitFile) {
            final File file = FilesUtils
                    .buildFile(baseDocFolder, String.join(DELIMITER, RELEASE_NOTE, ""+version+ADOC));
            if (file.exists()) {
                file.delete();
            }
            log.info("writing in file : {}", file.getAbsolutePath());
            writer = new FileWriter(file);
        }
        return writer;
    }


    private String renderProject(final MavenProject project) {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("= ")
              .write(project.getGroupId())
              .write(":")
              .write(project.getArtifactId())
              .write(":")
              .write(project.getVersion())
              .line();

        writer.line();
        writer.write("_(").write(LocalDateTime.now()).write(")_").line();
        if(project.getDescription()!=null){
            writer.line();
            writer.write(":description: ").write(project.getDescription());
            writer.line();
        }
        if(project.getUrl()!=null){
            writer.line();
            writer.write(":url-project: ").write(project.getUrl());
            writer.line();
        }
        writer.line();

        writer.write(":keywords: release-note").line();
        writer.write(":toc:").line();
        writer.line();
        return writer.toString();
    }

    protected String renderAuthors(final Set<Author> authors, final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();
        if (authors != null) {
            final List<Author> currentAuthors = new ArrayList<>(authors);
            currentAuthors.sort((ref, value) -> {
                return String.valueOf(ref == null ? null : ref.getName())
                             .compareTo(value == null ? null : value.getName());
            });

            if (notSplitFile) {
                writer.write("== Authors").line();
            }
            for (final Author author : currentAuthors) {
                writer.write("- ").write(author.getName());
                if (author.getEmail() != null) {
                    writer.write(" (").write(author.getEmail()).write(")").line();
                }
            }
            writer.line();
        }
        return writer.toString();
    }

    protected String renderCommit(final Set<String> commit, final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();
        if (commit != null) {
            if (notSplitFile) {
                writer.write("== Commit").line();
            }
            writer.write("[cols=\"2,1,1,4\", options=\"header\"]").line();
            writer.write("|===").line();
            writer.write("|Date | SHA | Author | Message").line();
            writer.line();

            for (final String item : commit) {
                final Matcher matcher = COMMIT_REGEX.matcher(item);
                if (matcher.matches()) {
                    writer.write("|").write(trim(matcher.group(1))).line();
                    writer.write("|").write(trim(matcher.group(2))).line();
                    writer.write("|").write(trim(matcher.group(3))).line();
                    writer.write("|").write(trim(matcher.group(4))).line();
                    writer.line();
                }
            }
            writer.write("|===").line();
            writer.line();
        }
        return writer.toString();
    }


    protected String renderMergeRequest(final List<MergeRequests> mergeRequests, final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();
        if (mergeRequests != null) {
            if (notSplitFile) {
                writer.write("== Merge requests").line();
            }
            final List<MergeRequests> data = new ArrayList<>(mergeRequests);
            data.sort((ref, value) -> {
                return String.valueOf(ref == null ? null : ref.getDate())
                             .compareTo(String.valueOf(value == null ? null : value.getDate()));
            });

            writer.write("[cols=\"2,1,3,4\", options=\"header\"]").line();
            writer.write("|===").line();
            writer.write("|Date | Id | Title | Url").line();
            writer.line();
            for (final MergeRequests value : data) {
                writer.write("|").write(trim(value.getDate())).line();
                writer.write("|").write(trim(value.getUid())).line();
                writer.write("|").write(trim(value.getTitle())).line();
                writer.write("|").write(trim(value.getUrl())).line();
                writer.line();
            }
            writer.write("|===").line();
            writer.line();
        }
        return writer.toString();
    }


    protected String renderIssues(final List<Issue> values, final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();
        if (values != null) {
            if (notSplitFile) {
                writer.write("== Issues").line();
            }

            final List<Issue> data = new ArrayList<>(values);
            data.sort((ref, value) -> {
                return String.valueOf(ref == null ? null : ref.getDate())
                             .compareTo(String.valueOf(value == null ? null : value.getDate()));
            });

            writer.write("[cols=\"2,1,3,4\", options=\"header\"]").line();
            writer.write("|===").line();
            writer.write("|Date | Issue | Title | Url").line();
            writer.line();
            for (final Issue value : data) {
                writer.write("|").write(trim(value.getDate())).line();
                writer.write("|").write(trim(value.getName())).line();
                writer.write("|").write(renderLabels(value.getLabels())).line();
                writer.write("|").write(trim(value.getUrl())).line();
                writer.line();
            }
            writer.write("|===").line();
            writer.line();
        }

        return writer.toString();
    }

    private String renderLabels(final Set<String> labels) {
        String result = "";
        if (result != null) {
            final List<String> data = new ArrayList<>(labels);
            Collections.sort(data);
            result = String.join(" ", data);
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private String trim(final String value) {
        return value == "" ? null : value.trim();
    }
}
