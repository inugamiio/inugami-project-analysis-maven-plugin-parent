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

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.writers.asciidoc.AsciidocInfoWriter;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.FlywayExtractor;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.FlywayDTO;

import java.util.*;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.processIfNotNull;

public class FlywayAsciidocWriter implements AsciidocInfoWriter {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Override
    public String getParagraphName() {
        return "error_codes";
    }

    @Override
    public String getfeatureName() {
        return "io.inugami.maven.plugin.analysis.asciidoc.errorCode.enabled";
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public LinkedHashMap<String, String> rendering(final ReleaseNoteResult releaseNote, final boolean notSplitFile,
                                                   final InfoContext context) {
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();

        result.put("flyway_base", renderBase("Flyway"));
        if (releaseNote != null && releaseNote.getDifferentials().containsKey(FlywayExtractor.FLYWAY)) {
            final Differential differential = releaseNote.getDifferentials().get(FlywayExtractor.FLYWAY);
            //@formatter:off
            result.put("flyway_new", renderingFlyway(notNull(differential.getNewValues()),"New flyway script", notSplitFile));
            result.put("flyway_deleted", renderingFlyway(notNull(differential.getDeletedValues()),"Deleted flyway script", notSplitFile));
            result.put("flyway", renderingFlyway(notNull(differential.getSameValues()),"Flyway script", notSplitFile));
            //@formatter:on
        }

        return result;

    }


    // =========================================================================
    // PRIVATE
    // =========================================================================
    private String renderingFlyway(final List<JsonObject> values, final String title,
                                   final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();
        if (notSplitFile) {
            writer.write("=== ").write(title).line();
        }

        final Map<String, List<FlywayDTO>> data = convertAndSortData(values);
        for (Map.Entry<String, List<FlywayDTO>> entry : data.entrySet()) {
            writer.write("==== ").write(entry.getKey()).line();
            for (FlywayDTO flywayScript : entry.getValue()) {
                writer.write(renderScript(flywayScript));
                writer.line();
            }
        }


        return writer.toString();
    }


    private String renderScript(final FlywayDTO flywayScript) {
        final JsonBuilder writer = new JsonBuilder();

        writer.write("===== ").write(flywayScript.getName()).line();

        writer.line();
        writer.write("[source,sql]").line();
        writer.write("----").line();
        writer.write(flywayScript.getContent()).line();
        writer.write("----");

        writer.line().write("*id :* ").write(flywayScript.getId()).line();
        writer.line().write("*Type :* ").write(flywayScript.getType()).line();

        processIfNotNull(flywayScript.getProjectsUsing(), values -> {
            List<String> projects = new ArrayList<>(flywayScript.getProjectsUsing());
            Collections.sort(projects);
            writer.line().write("*Projects using :* ").line();
            projects.forEach(value -> writer.line().write("* ").write(value).line());
        });
        return writer.toString();
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    private Map<String, List<FlywayDTO>> convertAndSortData(final List<JsonObject> values) {
        Map<String, List<FlywayDTO>> buffer = new HashMap<>();

        for (JsonObject value : Optional.ofNullable(values).orElse(new ArrayList<>())) {
            if (value instanceof FlywayDTO) {
                final FlywayDTO flyway = (FlywayDTO) value;

                List<FlywayDTO> dbTypeScripts = buffer.get(flyway.getType());
                if (dbTypeScripts == null) {
                    dbTypeScripts = new ArrayList<>();
                    buffer.put(flyway.getType(), dbTypeScripts);
                }
                dbTypeScripts.add(flyway);
            }
        }

        final LinkedHashMap<String, List<FlywayDTO>> result = new LinkedHashMap<>();
        final List<String>                           keys   = new ArrayList<>(buffer.keySet());
        Collections.sort(keys);

        for (String key : keys) {
            final List<FlywayDTO> scripts = new ArrayList<>(buffer.get(key));
            Collections.sort(scripts,
                             (ref, value) -> String.valueOf(ref.getName()).compareTo(String.valueOf(value.getName())));
            result.put(key, scripts);
        }
        return result;
    }

}
