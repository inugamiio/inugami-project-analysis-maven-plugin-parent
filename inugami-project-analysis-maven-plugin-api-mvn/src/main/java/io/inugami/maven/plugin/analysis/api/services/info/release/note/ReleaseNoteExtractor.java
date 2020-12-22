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
package io.inugami.maven.plugin.analysis.api.services.info.release.note;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import org.neo4j.driver.Record;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;

import static io.inugami.maven.plugin.analysis.api.utils.Constants.*;

public interface ReleaseNoteExtractor {

    void extractInformation(final ReleaseNoteResult releaseNoteResult,
                            final Gav currentVersion,
                            final Gav previousVersion,
                            final Neo4jDao dao,
                            final List<Replacement> replacements,
                            final InfoContext context);


    // =========================================================================
    // TOOLS
    // =========================================================================
    default String retrieveString(final String key, final Map<String, Object> data) {
        String result = null;
        if (data != null && data.containsKey(key)) {
            result = String.valueOf(data.get(key));
        }
        return result;
    }

    default String retrieveString(final String key, final Map<String, Object> data,
                                  final List<Replacement> replacements) {
        String result = null;
        if (data != null && data.containsKey(key)) {
            result = String.valueOf(data.get(key));
        }
        return result == null ? null : replace(result, replacements);
    }

    default String replace(final String input, final List<Replacement> replacements) {
        String result = input;
        if (result != null && replacements != null) {
            for (final Replacement replacement : replacements) {
                final Matcher matcher = replacement.getPattern().matcher(result);
                if (matcher.matches()) {
                    result = matcher.replaceAll(replacement.getReplacement());
                }
            }
        }
        return result;
    }


    default List<JsonObject> search(final String queryName,
                                    final Gav gav,
                                    final ConfigHandler<String, String> configuration,
                                    final Neo4jDao dao,
                                    final Function<List<Record>, List<JsonObject>> transformer) {
        List<JsonObject>                    result = null;
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(Map.ofEntries(
                Map.entry(GROUP_ID, gav.getGroupId()),
                Map.entry(ARTIFACT_ID, gav.getArtifactId()),
                Map.entry(VERSION, gav.getVersion())
                                   ));
        final String query = TemplateRendering.render(QueriesLoader.getQuery(queryName),
                                                      config);
        final List<Record> resultSet = dao.search(query);
        if (resultSet != null && transformer != null) {
            result = transformer.apply(resultSet);
        }
        return result;
    }

}
