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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.files.FilesUtils;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteWriter;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.utils.ObjectMapperBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


@Slf4j
public class ReleaseNoteJsonWriter implements ReleaseNoteWriter {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final  String FEATURE_NAME   = "inugami.maven.plugin.analysis.display.release.note.json";
    public static final  String CONF_FOLDER    = FEATURE_NAME + ".folder";
    private static final String DEFAULT_FOLDER = String.join(File.separator,
                                                             "src", "main", "resources",
                                                             "META-INF", "releases");

    public static final String ATTACH = FEATURE_NAME + "attach";

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final ConfigHandler<String, String> configuration) {
        return Boolean.parseBoolean(configuration.grabOrDefault(FEATURE_NAME, "true"));
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final ReleaseNoteResult releaseNote,
                        final InfoContext context) {

        final String destinationFolder = context.getConfiguration().grabOrDefault(CONF_FOLDER, DEFAULT_FOLDER);
        final String json              = convertToJson(releaseNote);
        final String fileName = String.join("-",
                                            context.getProject().getArtifactId(),
                                            context.getProject().getVersion()) + ".json";

        final File file = FilesUtils.buildFile(context.getBasedir(), destinationFolder, fileName);

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (json != null) {
            log.info("begin write file : {}", file.getAbsolutePath());
            FilesUtils.write(json, file);
        }

        writeIndexFile(file.getAbsoluteFile().getParentFile(), context.getProject().getArtifactId());

        if (Boolean.parseBoolean(context.getConfiguration().grabOrDefault(ATTACH, "true"))) {
            final MavenProject project = context.getProject();
            log.info("attach json release note to artifact : {}:{}:{}", project.getGroupId(),
                     project.getArtifactId(),
                     project.getVersion());

            final DefaultArtifact artifact = new DefaultArtifact(project.getGroupId(),
                                                                 project.getArtifactId(),
                                                                 project.getVersion(),
                                                                 "compile",
                                                                 "json",
                                                                 "release-note",
                                                                 context.getArtifactHandler());
            artifact.setFile(file);
            context.getProject().addAttachedArtifact(artifact);
        }
    }

    private void writeIndexFile(final File parentFile, final String artifactId) {
        final List<String> files        = extractReleaseNoteFiles(parentFile, artifactId);
        final List<String> filesOrdered = sortReleasesNotes(files);
        final String       json         = new JsonBuilder().writeListString(filesOrdered).toString();

        final File indexFile = FilesUtils.buildFile(parentFile, artifactId + ".releases.json");
        FilesUtils.write(json, indexFile);
    }


    protected List<String> extractReleaseNoteFiles(final File parentFile, final String artifactId) {
        final List<String> files = new ArrayList<>();
        if (parentFile != null && parentFile.exists()) {
            for (final String fileName : parentFile.list()) {
                if (fileName.startsWith(artifactId, 0) && fileName.endsWith(".json")) {
                    files.add(fileName);
                }
            }
        }
        return files;
    }

    protected List<String> sortReleasesNotes(final List<String> files) {
        final List<String> result = new ArrayList<>(files);
        Collections.sort(result, (r, v) -> v.compareTo(r));
        return result;
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
}
