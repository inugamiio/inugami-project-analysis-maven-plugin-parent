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
import io.inugami.maven.plugin.analysis.api.constant.Constants;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Differential;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models.EntityDTO;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.types.Node;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_ENTITIES;
import static io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.EntitiesAnalyzer.LOCAL_ENTITY;

public class EntitiesExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final  String ENTITY_TYPE         = "entities";
    public static final  String NODE_LOCAL_ENTITY   = "localEntity";
    public static final  String NODE_ENTITY         = "entity";
    public static final  String PAYLOAD             = "payload";
    public static final  String SHORT_NAME          = "shortName";
    private static final String NODE_DEPENDENCY_REF = "dependencyEntityRef";
    private static final String NODE_DEPENDENCY     = "dependency";


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        final List<JsonObject> currentErrorCodes = search(QUERIES_SEARCH_ENTITIES, currentVersion,
                                                          context.getConfiguration(), dao, this::convert);

        final List<JsonObject> previousErrorCodes = previousVersion == null ? null : search(
                QUERIES_SEARCH_ENTITIES,
                previousVersion,
                context.getConfiguration(),
                dao,
                this::convert);

        releaseNoteResult.addDifferential(ENTITY_TYPE, Differential
                .buildDifferential(newSet(currentErrorCodes), newSet(previousErrorCodes)));
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private List<JsonObject> convert(final List<Record> resultSet) {
        final Map<String, EntityDTO> buffer = new LinkedHashMap<>();

        for (final Record record : resultSet) {
            final NodeValue entityNode        = extractNode(NODE_ENTITY, record);
            final NodeValue dependencyNode    = extractNode(NODE_DEPENDENCY, record);
            final NodeValue dependencyRefNode = extractNode(NODE_DEPENDENCY_REF, record);
            if (isNotNull(entityNode)) {
                convertOnRecord(buffer, entityNode, dependencyNode, dependencyRefNode);
            }


        }
        return buffer.entrySet()
                     .stream()
                     .map(Map.Entry::getValue)
                     .collect(Collectors.toList());
    }

    protected void convertOnRecord(final Map<String, EntityDTO> buffer, final NodeValue entityNode, final NodeValue dependencyNode, final NodeValue dependencyRefNode) {
        final Node   propertyNode = entityNode.asNode();
        final String nodeName     = cleanName(retrieve(SHORT_NAME, propertyNode));
        EntityDTO    entity       = null;
        if (buffer.containsKey(nodeName)) {
            entity = buffer.get(nodeName);
        } else {
            entity = EntityDTO.builder()
                              .name(nodeName)
                              .payload(retrieve(PAYLOAD, propertyNode))
                              .build();
            buffer.put(nodeName, entity);
        }
        final String dependency    = dependencyNode == null ? null : buildArtifact(dependencyNode);
        final String dependencyRef = dependencyRefNode == null ? null : buildArtifact(dependencyRefNode);

        if (dependency != null) {
            entity.addProjectUsing(dependency);
        }
        if (dependencyRef != null) {
            entity.addProjectUsing(dependencyRef);
        }
    }

    private String cleanName(final String name) {
        return name == null ? null : name.replaceFirst(LOCAL_ENTITY, "");
    }

    private String buildArtifact(final NodeValue artifact) {
        String result = null;
        if (artifact != null && !artifact.isNull()) {
            final Node node = artifact.asNode();
            result = String.join(Constants.GAV_SEPARATOR,
                                 retrieve(Constants.GROUP_ID, node),
                                 retrieve(Constants.ARTIFACT_ID, node),
                                 retrieve(Constants.VERSION, node));
        }

        return result;
    }
}
