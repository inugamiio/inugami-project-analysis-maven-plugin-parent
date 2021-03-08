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
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.EntitiesExtractor;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.EntityDTO;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.ErrorCodeDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

public class EntityAsciidocWriter implements AsciidocInfoWriter {

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public String getParagraphName() {
        return "entity";
    }

    @Override
    public String getfeatureName() {
        return "io.inugami.maven.plugin.analysis.asciidoc.entity.enabled";
    }

    @Override
    public LinkedHashMap<String, String> rendering(final ReleaseNoteResult releaseNote, final boolean notSplitFile,
                                                   final InfoContext context) {

        final LinkedHashMap<String, String> result = new LinkedHashMap<>();

        result.put("entities_root", renderBase());
        if (releaseNote != null && releaseNote.getDifferentials().containsKey(EntitiesExtractor.ENTITY_TYPE)) {
            final Differential differential = releaseNote.getDifferentials().get(EntitiesExtractor.ENTITY_TYPE);
            //@formatter:off
            result.put("new_entities", processRendering(notNull(differential.getNewValues()),"New entities", notSplitFile));
            result.put("deleted_entities", processRendering(notNull(differential.getDeletedValues()),"Deleted entities", notSplitFile));
            result.put("entities", processRendering(notNull(differential.getSameValues()),"Entities", notSplitFile));
            //@formatter:on
        }

        return result;
    }

    private String renderBase() {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("== Entities").line();
        return writer.toString();
    }

    private String processRendering(final List<JsonObject> values, final String title, final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();

        if (notSplitFile) {
            writer.write("=== ").write(title).line();
        }

        final List<EntityDTO> data = new ArrayList<>();
        for (final JsonObject value : values) {
            if (value instanceof EntityDTO) {
                data.add((EntityDTO) value);
            }
        }
        Collections.sort(data);

        for (final EntityDTO entity : data) {
            writer.write("==== ").write(entity.getName()).line();
            writer.write(renderPayload(entity.getPayload()));

            if(entity.getProjectsUsing()!=null){
                writer.line().write("*Projects using :* ").line();
                entity.getProjectsUsing().forEach(value -> writer.line().write("* ").write(value).line());
            }

            writer.line();
        }

        writer.line();
        return writer.toString();
    }


}
