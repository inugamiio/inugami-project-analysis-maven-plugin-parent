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
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.ReleaseNoteResult;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.Replacement;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;

import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_PROJECT_DEPENDENCIES_GRAPH_CQL;

@Slf4j
public class ProjectDependenciesExtractor implements ReleaseNoteExtractor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String TYPE         = "dependencies";
    public static final String TYPE_PROJECT = "dependencies_project";
    public static final String PACKAGING    = "packaging";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void extractInformation(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                   final Gav previousVersion, final Neo4jDao dao, final List<Replacement> replacements,
                                   final InfoContext context) {

        extractProjectDependencies(releaseNoteResult, currentVersion, previousVersion, dao, context);
    }

    private void extractProjectDependencies(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                            final Gav previousVersion, final Neo4jDao dao, final InfoContext context) {
        final List<JsonObject> currentValues = search(QUERIES_SEARCH_PROJECT_DEPENDENCIES_GRAPH_CQL, currentVersion,
                                                      context.getConfiguration(), dao, this::convert);


    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private List<JsonObject> convert(final List<Record> resultSet) {
        final List<JsonObject>                  result        = new ArrayList<>();
        final Map<Long, Gav>                    nodeRefs      = new LinkedHashMap<>();
        final Map<Long, Map<String, Set<Long>>> relationships = new LinkedHashMap<>();

        final Set<Gav> artifacts = new LinkedHashSet<>();

        for (final Record record : resultSet) {

            final Gav dependency    = extractGav("dependency", record);
            Long      dependencyId  = null;
            final Gav depProducer   = extractGav("depProducer", record);
            Long      depProducerId = null;
            final Gav depConsumer   = extractGav("depConsumer", record);
            Long      depConsumerId = null;

            final String serviceConsumedType = extractServiceType("serviceConsumedType", record);
            final String serviceProducedType = extractServiceType("serviceProducedType", record);


            if (dependency != null) {
                artifacts.add(dependency);
                dependencyId = extractNodeId("dependency", record);
                nodeRefs.put(dependencyId, dependency);
            }
            if (depProducer != null) {
                artifacts.add(depProducer);
                depProducerId = extractNodeId("depProducer", record);
                nodeRefs.put(depProducerId, dependency);
            }
            if (depConsumer != null) {
                artifacts.add(depConsumer);
                depConsumerId = extractNodeId("depConsumer", record);
                nodeRefs.put(depConsumerId, dependency);
            }


            final List<Relationship> libLinks = extractRelationships("dependencyLink", record);
            for (final Relationship relationship : libLinks) {
                final Set<Long> currentRelationship = retrieveRelationships(relationship.endNodeId(), "lib",
                                                                            relationships);
                currentRelationship.add(relationship.startNodeId());

            }

            if (depProducerId != null) {
                final Set<Long> currentRelationship = retrieveRelationships(dependencyId, serviceProducedType,
                                                                            relationships);
                currentRelationship.add(depProducerId);
            }

            if (depConsumerId != null) {
                final Set<Long> currentRelationship = retrieveRelationships(depConsumerId, serviceConsumedType,
                                                                            relationships);
                currentRelationship.add(dependencyId);
            }
            log.info("record : {}", record);
        }
        return result;
    }

    private Set<Long> retrieveRelationships(final long nodeId, final String relationshipType,
                                            final Map<Long, Map<String, Set<Long>>> relationships) {

        Map<String, Set<Long>> relationshipResult = relationships.get(nodeId);
        if (relationshipResult == null) {
            relationshipResult = new LinkedHashMap<>();
            relationships.put(nodeId, relationshipResult);
        }

        Set<Long> result = relationshipResult.get(relationshipType);
        if (result == null) {
            result = new LinkedHashSet<>();
            relationshipResult.put(relationshipType, result);
        }
        return result;
    }

    private Long extractNodeId(final String nodeName, final Record record) {
        Long            result   = null;
        final NodeValue property = extractNode(nodeName, record);
        if (isNotNull(property)) {
            result = property.asNode().id();
        }
        return result;
    }


    private String extractServiceType(final String nodeName, final Record record) {
        String          result   = null;
        final NodeValue property = extractNode(nodeName, record);
        if (isNotNull(property)) {
            final Node propertyNode = property.asNode();

            result = retrieve("name", propertyNode);
        }
        return result;
    }

    private Gav extractGav(final String nodeName, final Record record) {
        Gav             result   = null;
        final NodeValue property = extractNode(nodeName, record);
        if (isNotNull(property)) {
            final Node propertyNode = property.asNode();
            result = Gav.builder()
                        .groupId(retrieve(GROUP_ID, propertyNode))
                        .artifactId(retrieve(ARTIFACT_ID, propertyNode))
                        .version(retrieve(VERSION, propertyNode))
                        .type(retrieve(PACKAGING, propertyNode))
                        .build();
        }
        return result;
    }

}
