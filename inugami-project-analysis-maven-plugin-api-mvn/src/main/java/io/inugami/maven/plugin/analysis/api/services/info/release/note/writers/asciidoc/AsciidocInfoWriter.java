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
package io.inugami.maven.plugin.analysis.api.services.info.release.note.writers.asciidoc;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public interface AsciidocInfoWriter {
    String getParagraphName();

    LinkedHashMap<String, String> rendering(final ReleaseNoteResult releaseNote,
                                            final boolean notSplitFile,
                                            final InfoContext context);

    String getfeatureName();

    default List<JsonObject> notNull(final List<JsonObject> values) {
        return Optional.ofNullable(values).orElse(new ArrayList<>());
    }

    default boolean isEnabled(final ConfigHandler<String, String> configuration) {
        final String featureName = getfeatureName();
        return featureName == null ? true : Boolean.parseBoolean(configuration.grabOrDefault(featureName,"true"));
    }
}
