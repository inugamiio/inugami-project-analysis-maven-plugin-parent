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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.writers.asciidoc;

import edu.emory.mathcs.backport.java.util.Collections;
import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.writers.asciidoc.AsciidocInfoWriter;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.ErrorCodeExtractor;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.ErrorCodeDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class ErrorCodeAsciidocWriter implements AsciidocInfoWriter {

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public String getParagraphName() {
        return "error_codes";
    }

    @Override
    public LinkedHashMap<String, String> rendering(final ReleaseNoteResult releaseNote, final boolean notSplitFile,
                                                   final InfoContext context) {

        final LinkedHashMap<String, String> result = new LinkedHashMap<>();
        result.put("error_base", renderBase());
        if (releaseNote != null && releaseNote.getDifferentials().containsKey(ErrorCodeExtractor.ERROR_CODES)) {
            final Differential differential = releaseNote.getDifferentials().get(ErrorCodeExtractor.ERROR_CODES);
            //@formatter:off
            result.put("new_errors", renderingErrors(notNull(differential.getNewValues()),"New error codes", notSplitFile));
            result.put("deleted_errors", renderingErrors(notNull(differential.getDeletedValues()),"Deleted error codes", notSplitFile));
            result.put("errors", renderingErrors(notNull(differential.getSameValues()),"Error codes", notSplitFile));
            //@formatter:on
        }
        return result;
    }

    private String renderBase() {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("== Error codes").line();
        return writer.toString();
    }

    private String renderingErrors(final List<JsonObject> values, final String title, final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();

        if (notSplitFile) {
            writer.write("=== ").write(title).line();
        }

        writer.write("[cols=\"2,1,1,4,1\", options=\"header\"]").line();
        writer.write("|===").line();
        writer.write("|Error | Type | Status | Message | artifact").line();
        writer.line();

        final List<ErrorCodeDTO> data = new ArrayList<>();
        for (final JsonObject value : values) {
            if (value instanceof ErrorCodeDTO) {
                data.add((ErrorCodeDTO) value);
            }
        }
        Collections.sort(data);

        for (final ErrorCodeDTO error : data) {
            writer.write("|").write(error.getErrorCode()).line();
            writer.write("|").write(error.getType()).line();
            writer.write("|").write(error.getStatusCode()).line();
            writer.write("|").write(error.getMessage()).line();
            writer.write("|").write(error.getArtifact()).line();
            writer.line();
        }

        writer.write("|===").line();
        writer.line();
        return writer.toString();
    }


}
