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
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.writers.asciidoc.AsciidocInfoWriter;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors.DependenciesExtractor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class DependenciesProjectAsciidocWriter implements AsciidocInfoWriter {

    public static final String PROJECT_DEPENDENCIES = "project_dependencies";
    public static final String TITLE = "Project dependencies";
    public static final String PROPERTY_ENABLED = "io.inugami.maven.plugin.analysis.asciidoc.dependenciesProjects.enabled";

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @Override
    public String getParagraphName() {
        return DependenciesExtractor.TYPE_PROJECT;
    }

    protected String getParagraphBaseName() {
        return PROJECT_DEPENDENCIES;
    }

    protected String getParagraphBaseTitle() {
        return TITLE;
    }

    @Override
    public String getfeatureName() {
        return PROPERTY_ENABLED;
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public LinkedHashMap<String, String> rendering(final ReleaseNoteResult releaseNote, final boolean notSplitFile,
                                                   final InfoContext context) {
        final LinkedHashMap<String, String> result = new LinkedHashMap<>();

        result.put(getParagraphBaseName(), renderBase());
        if (releaseNote != null && releaseNote.getDifferentials().containsKey(getParagraphName())) {
            final Differential differential = releaseNote.getDifferentials().get(getParagraphName());
            //@formatter:off
            result.put(getParagraphBaseName()+"_new_dependencies", renderingDependencies(notNull(differential.getNewValues()),"New dependencies", notSplitFile));
            result.put(getParagraphBaseName()+"_deleted_dependencies", renderingDependencies(notNull(differential.getDeletedValues()),"Deleted dependencies", notSplitFile));
            result.put(getParagraphBaseName()+"_dependencies", renderingDependencies(notNull(differential.getSameValues()),"Dependencies", notSplitFile));
            //@formatter:on
        }
        return result;
    }


    // =========================================================================
    // RENDERING
    // =========================================================================
    private String renderBase() {
        final JsonBuilder writer = new JsonBuilder();
        writer.write("== ").write(getParagraphBaseTitle()).line();
        return writer.toString();
    }

    public static String renderingDependencies(final List<JsonObject> values, final String title,
                                               final boolean notSplitFile) {
        final JsonBuilder writer = new JsonBuilder();

        final List<Gav> data = new ArrayList<>();
        for (final JsonObject value : values) {
            if (value instanceof Gav) {
                data.add((Gav) value);
            }
        }
        Collections.sort(data);

        if (notSplitFile) {
            writer.write("=== ").write(title).line();
        }

        writer.write("[cols=\"3,3,1\", options=\"header\"]").line();
        writer.write("|===").line();
        writer.write("|GroupId | ArtifactId | Version").line();
        writer.line();

        for (final Gav dependency : data) {
            writer.write("|").write(dependency.getGroupId()).line();
            writer.write("|").write(dependency.getArtifactId()).line();
            writer.write("|").write(dependency.getVersion()).line().line();
        }
        writer.write("|===").line();
        writer.line();
        return writer.toString();
    }

}
