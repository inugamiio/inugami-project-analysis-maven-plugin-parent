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
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.PropertiesExtractor;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.PropertyDTO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.EMPTY;
import static io.inugami.maven.plugin.analysis.api.constant.Constants.SPACE;

;

public class PropertiesAsciidocWriter implements AsciidocInfoWriter {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public String getParagraphName() {
        return PropertiesExtractor.TYPE;
    }

    @Override
    public String getfeatureName() {
        return "io.inugami.maven.plugin.analysis.asciidoc.properties.enabled";
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    @Override
    public LinkedHashMap<String, String> rendering(final ReleaseNoteResult releaseNote, final boolean notSplitFile,
                                                   final InfoContext context) {

        final LinkedHashMap<String, String> result = new LinkedHashMap<>();

        result.put("properties_base", renderBase());
        if (releaseNote != null && releaseNote.getDifferentials().containsKey(PropertiesExtractor.TYPE)) {
            final Differential differential = releaseNote.getDifferentials().get(PropertiesExtractor.TYPE);
            //@formatter:off
            result.put("new_properties", renderingProperties(notNull(differential.getNewValues()), "New properties", notSplitFile));
            result.put("deleted_properties", renderingProperties(notNull(differential.getDeletedValues()), "Deleted properties", notSplitFile));
            result.put("properties", renderingProperties(notNull(differential.getSameValues()), "Properties", notSplitFile));
            //@formatter:on
        }

        return result;
    }


    // =========================================================================
    // RENDERING
    // =========================================================================
    private String renderBase() {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("== Properties").line();
        return writer.toString();
    }

    private String renderingProperties(final List<JsonObject> values,
                                       final String title,
                                       final boolean notSplitFile) {

        final JsonBuilder writer = new JsonBuilder();

        if (notSplitFile) {
            writer.write("=== ").write(title).line();
        }
        final List<PropertyDTO> data = new ArrayList<>();
        for (final JsonObject value : values) {
            if (value instanceof PropertyDTO) {
                data.add((PropertyDTO) value);
            }
        }
        Collections.sort(data);

        writer.write("[cols=\"3,1,1,1,1,1,2\", options=\"header\"]").line();
        writer.write("|===").line();
        writer.write("|Name | Type | defaultValue | constraint | detail | use for bean | artifact").line();
        writer.line();


        for (final PropertyDTO property : data) {
            writer.write("|").write(renderPropertyName(property.getName(), property.isMandatory())).line();
            writer.write("|").write(notNull(property.getPropertyType())).line();
            writer.write("|").write(notNull(property.getDefaultValue())).line();
            writer.write("|").write(notNull(property.getConstraintType())).line();
            writer.write("|").write(notNull(property.getConstraintDetail())).line();
            writer.write("|").write(property.isUseForConditionalBean() ? true : SPACE).line();
            writer.write("|").write(notNull(property.getArtifact())).line();
            writer.line();
        }

        writer.write("|===").line();
        writer.line();

        return writer.toString();
    }


    private String renderPropertyName(final String name, final boolean mandatory) {
        final StringBuilder result = new StringBuilder();
        if (mandatory) {
            result.append("*");
        }

        result.append(name);
        if (mandatory) {
            result.append("*");
        }
        return result.toString();
    }

    private String notNull(final String value) {
        return value == null ? EMPTY : value;
    }

}
