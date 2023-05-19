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
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.FlywayDTO;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;

import java.util.*;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.extractNode;
import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.getNodeName;
import static io.inugami.maven.plugin.analysis.api.utils.Constants.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_FLYWAY;

public class FlywayExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String FLYWAY              = "flyway";
    public static final String ARTIFACT_NODE       = "artifact";
    public static final String FLYWAY_NODE         = "flyway";
    public static final String FLYWAY_CONTENT_NODE = "flywayContent";
    public static final String DEPENDENCY_NODE     = "dependency";
    public static final String SHORT_NAME          = "shortName";
    public static final String DB_TYPE             = "dbType";
    public static final String CONTENT             = "content";
    public static final String NAME                = "name";


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        final Set<JsonObject> currentErrorCodes = searchFlyway(currentVersion, context.getConfiguration(), dao);
        final Set<JsonObject> previousErrorCodes = previousVersion == null ? null : searchFlyway(previousVersion,
                                                                                                 context.getConfiguration(),
                                                                                                 dao);


        releaseNoteResult.addDifferential(FLYWAY, Differential
                .buildDifferential(currentErrorCodes, previousErrorCodes));
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private Set<JsonObject> searchFlyway(final Gav gav, final ConfigHandler<String, String> configuration,
                                         final Neo4jDao dao) {
        Set<JsonObject>                     result = null;
        final ConfigHandler<String, String> config = new ConfigHandlerHashMap(configuration);
        config.putAll(Map.ofEntries(
                Map.entry(GROUP_ID, gav.getGroupId()),
                Map.entry(ARTIFACT_ID, gav.getArtifactId()),
                Map.entry(VERSION, gav.getVersion())
        ));
        final String query = TemplateRendering.render(QueriesLoader.getQuery(QUERIES_SEARCH_FLYWAY),
                                                      config);
        final List<Record> resultSet = dao.search(query);
        if (resultSet != null) {
            result = convertToModel(resultSet);
        }
        return result;
    }

    private Set<JsonObject> convertToModel(final List<Record> resultSet) {
        final List<FlywayDTO> result = new ArrayList<>();

        for (final Record record : resultSet) {
            final String artifact   = getNodeName(extractNode(ARTIFACT_NODE, record));
            final String dependency = getNodeName(extractNode(DEPENDENCY_NODE, record));

            final NodeValue flyway        = extractNode(FLYWAY_NODE, record);
            final NodeValue flywayContent = extractNode(FLYWAY_CONTENT_NODE, record);


            final FlywayDTO dto = buildDto(flyway, flywayContent);
            if (dto == null) {
                continue;
            }
            
            final int index = result.indexOf(dto);
            if (index == -1) {
                dto.addProjectUsing(artifact).addProjectUsing(dependency);
                result.add(dto);
            } else {
                result.get(index)
                      .addProjectUsing(artifact)
                      .addProjectUsing(dependency);
            }

        }
        return new LinkedHashSet<>(result);
    }

    private FlywayDTO buildDto(final NodeValue flyway, final NodeValue flywayContent) {

        FlywayDTO                 result            = null;
        final Map<String, Object> flywayProperties  = flyway == null ? new HashMap<>() : flyway.asMap();
        final Map<String, Object> contentProperties = flywayContent == null ? new HashMap<>() : flywayContent.asMap();

        if (flywayProperties != null) {
            result = FlywayDTO.builder()
                              .id(retrieveString(NAME, flywayProperties))
                              .name(retrieveString(SHORT_NAME, flywayProperties))
                              .type(retrieveString(DB_TYPE, flywayProperties))
                              .content(retrieveString(CONTENT, contentProperties))
                              .build();
        }
        return result;
    }

}
