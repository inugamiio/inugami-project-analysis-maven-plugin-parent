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


@Slf4j
public class ReleaseNoteJsonWriter implements ReleaseNoteWriter {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FEATURE_NAME = "inugami.maven.plugin.analysis.display.release.note.json";
    public static final String ATTACH       = FEATURE_NAME + "attach";

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
        final String json = convertToJson(releaseNote);
        final File file = FilesUtils.buildFile(context.getBasedir(),
                                               "release-note-" + context.getProject().getVersion() + ".json");

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (json != null) {
            log.info("begin write file : {}", file.getAbsolutePath());
            FilesUtils.write(json, file);
        }

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
