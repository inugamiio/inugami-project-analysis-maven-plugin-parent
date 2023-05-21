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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.extractors;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.GlossaryDTO;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.GlossaryAnalyzer;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;

import java.util.*;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.extractNode;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_GLOSSARY;

@SuppressWarnings({"java:S6213"})
public class GlossaryExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String NODE = "g";


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        final Set<JsonObject> currentErrorCodes = searchGlossaries(currentVersion, context.getConfiguration(), dao);

        releaseNoteResult.addExtractedInformation("glossary", currentErrorCodes);
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private Set<JsonObject> searchGlossaries(final Gav gav, final ConfigHandler<String, String> configuration,
                                             final Neo4jDao dao) {
        Set<JsonObject>                     result = null;
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(Map.ofEntries(
                Map.entry(GROUP_ID, gav.getGroupId()),
                Map.entry(ARTIFACT_ID, gav.getArtifactId()),
                Map.entry(VERSION, gav.getVersion())
        ));
        final String query = TemplateRendering.render(QueriesLoader.getQuery(QUERIES_SEARCH_GLOSSARY),
                                                      config);
        final List<Record> resultSet = dao.search(query);
        if (resultSet != null) {
            result = convertToModel(resultSet);
        }
        return result;
    }

    private Set<JsonObject> convertToModel(final List<Record> resultSet) {
        final List<GlossaryDTO> result = new ArrayList<>();

        for (final Record record : resultSet) {
            final NodeValue           node = extractNode(NODE, record);
            final Map<String, Object> map  = node == null ? new HashMap<>() : node.asMap();

            result.add(GlossaryDTO.builder()
                                  .label(extractInfo(GlossaryAnalyzer.LABEL, map))
                                  .value(extractInfo(GlossaryAnalyzer.VALUE, map))
                                  .description(extractInfo(GlossaryAnalyzer.DESCRIPTION, map))
                                  .type(extractInfo(GlossaryAnalyzer.TYPE, map))
                                  .build());
        }
        return new LinkedHashSet<>(result);
    }

    private String extractInfo(final String key, final Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        final Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }


}

