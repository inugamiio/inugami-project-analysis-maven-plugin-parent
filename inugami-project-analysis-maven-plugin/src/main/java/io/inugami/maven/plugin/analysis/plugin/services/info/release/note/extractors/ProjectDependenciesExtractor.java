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
import io.inugami.api.models.data.basic.JsonObjects;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.ReleaseNoteExtractor;
import io.inugami.maven.plugin.analysis.api.services.info.release.note.models.*;
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import java.util.*;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.api.tools.Neo4jUtils.*;
import static io.inugami.maven.plugin.analysis.plugin.services.MainQueryProducer.QUERIES_SEARCH_PROJECT_DEPENDENCIES_GRAPH_CQL;

@SuppressWarnings({"java:S6213", "java:S3824"})
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

        extractProjectDependencies(releaseNoteResult, currentVersion, dao, context);
    }

    private void extractProjectDependencies(final ReleaseNoteResult releaseNoteResult, final Gav currentVersion,
                                            final Neo4jDao dao, final InfoContext context) {
        final List<JsonObject> currentValues = search(QUERIES_SEARCH_PROJECT_DEPENDENCIES_GRAPH_CQL, currentVersion,
                                                      context.getConfiguration(), dao,
                                                      resultSet -> this.convert(resultSet, currentVersion));

        if (currentValues != null && !currentValues.isEmpty()) {

            final JsonObjects<GavGraph> artifacts = (JsonObjects<GavGraph>) currentValues.get(0);

            final ProjectDependenciesGraph.ProjectDependenciesGraphBuilder builder = ProjectDependenciesGraph.builder();
            builder.artifacts(artifacts.getData());

            if (currentValues.size() > 1) {
                final JsonObjects<Graph> graph = (JsonObjects<Graph>) currentValues.get(1);
                builder.graph(graph.getData());
            }

            releaseNoteResult.setProjectDependenciesGraph(builder.build());

        }
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    private List<JsonObject> convert(final List<Record> resultSet, final Gav currentVersion) {
        final List<JsonObject>                  result        = new ArrayList<>();
        final Map<Long, GavGraph>               nodeRefs      = new LinkedHashMap<>();
        final Map<Long, Map<String, Set<Long>>> relationships = new LinkedHashMap<>();

        final Set<GavGraph> artifacts = new LinkedHashSet<>();

        for (final Record record : resultSet) {

            convertOnRecord(currentVersion, nodeRefs, relationships, artifacts, record);
        }

        final Set<GavGraph> artifactBuffer = new LinkedHashSet<>();
        nodeRefs.entrySet().stream().map(Map.Entry::getValue).forEach(artifactBuffer::add);

        final JsonObjects<GavGraph> projectGavs = new JsonObjects<>(new ArrayList<>(artifactBuffer));

        final JsonObjects<Graph> dependencies = convertToJsonObject(nodeRefs, relationships);

        result.add(projectGavs);
        result.add(dependencies);

        return result;
    }

    protected void convertOnRecord(final Gav currentVersion, final Map<Long, GavGraph> nodeRefs, final Map<Long, Map<String, Set<Long>>> relationships, final Set<GavGraph> artifacts, final Record record) {
        final GavGraph dependency    = extractGav("dependency", record, currentVersion);
        Long           dependencyId  = null;
        final GavGraph depProducer   = extractGav("depProducer", record, currentVersion);
        Long           depProducerId = null;
        final GavGraph depConsumer   = extractGav("depConsumer", record, currentVersion);
        Long           depConsumerId = null;

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
            nodeRefs.put(depConsumerId, depConsumer);
        }


        final List<Relationship> libLinks = extractRelationships("dependencyLink", record);
        for (final Relationship relationship : libLinks) {
            final Set<Long> currentRelationship = retrieveRelationships(relationship.endNodeId(), "lib",
                                                                        relationships);
            currentRelationship.add(relationship.startNodeId());

        }

        if (depProducerId != null) {
            final Set<Long> currentRelationship = retrieveRelationships(dependencyId, serviceConsumedType,
                                                                        relationships);
            currentRelationship.add(depProducerId);
        }

        if (depConsumerId != null) {
            final Set<Long> currentRelationship = retrieveRelationships(depConsumerId, serviceProducedType,
                                                                        relationships);
            currentRelationship.add(dependencyId);
        }
        log.info("record : {}", record);
    }

    protected JsonObjects<Graph> convertToJsonObject(final Map<Long, GavGraph> nodeRefs,
                                                     final Map<Long, Map<String, Set<Long>>> relationships) {
        final List<Graph>                            result = new ArrayList<>();
        final Map<String, Map<String, List<String>>> buffer = resolveDependenciesGraph(nodeRefs, relationships);

        for (final Map.Entry<String, Map<String, List<String>>> artifactEntry : buffer.entrySet()) {
            final Graph.GraphBuilder    builder      = Graph.builder().hash(artifactEntry.getKey());
            final List<GraphDependency> dependencies = new ArrayList<>();

            for (final Map.Entry<String, List<String>> dependency : artifactEntry.getValue().entrySet()) {
                dependencies.add(GraphDependency.builder()
                                                .hash(dependency.getKey())
                                                .consume(dependency.getValue())
                                                .build());
            }
            builder.dependencies(dependencies);
            result.add(builder.build());
        }
        return new JsonObjects<>(result);
    }

    protected Map<String, Map<String, List<String>>> resolveDependenciesGraph(final Map<Long, GavGraph> nodeRefs,
                                                                              final Map<Long, Map<String, Set<Long>>> relationships) {

        final Map<String, Map<String, List<String>>> result = new LinkedHashMap<>();

        for (final Map.Entry<Long, Map<String, Set<Long>>> artifactEntry : relationships.entrySet()) {

            final GavGraph dependencyHash = nodeRefs.get(artifactEntry.getKey());

            for (final Map.Entry<String, Set<Long>> entry : artifactEntry.getValue().entrySet()) {
                resolveDependenciesGraphOnRelationship(nodeRefs, result, dependencyHash, entry);
            }
        }
        return result;
    }

    protected static void resolveDependenciesGraphOnRelationship(final Map<Long, GavGraph> nodeRefs, final Map<String, Map<String, List<String>>> result, final GavGraph dependencyHash, final Map.Entry<String, Set<Long>> entry) {
        final String relationship = entry.getKey();
        for (final Long id : entry.getValue()) {
            final GavGraph artifactHash = nodeRefs.get(id);
            log.debug("{}-[{}]->{}", dependencyHash.getHash(), relationship, artifactHash.getHash());


            Map<String, List<String>> artifactBuffer = result.get(artifactHash.getHash());
            if (artifactBuffer == null) {
                artifactBuffer = new LinkedHashMap<>();
                result.put(artifactHash.getHash(), artifactBuffer);
            }

            List<String> dependencyBuffer = artifactBuffer.get(dependencyHash.getHash());
            if (dependencyBuffer == null) {
                dependencyBuffer = new ArrayList<>();
                artifactBuffer.put(dependencyHash.getHash(), dependencyBuffer);
            }

            if (!dependencyBuffer.contains(relationship)) {
                dependencyBuffer.add(relationship);
            }

        }
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

    private GavGraph extractGav(final String nodeName, final Record record, final Gav currentVersion) {
        GavGraph        result   = null;
        final NodeValue property = extractNode(nodeName, record);
        if (isNotNull(property)) {
            final Node propertyNode = property.asNode();
            result = GavGraph.builder()
                             .groupId(retrieve(GROUP_ID, propertyNode))
                             .artifactId(retrieve(ARTIFACT_ID, propertyNode))
                             .version(retrieve(VERSION, propertyNode))
                             .type(retrieve(PACKAGING, propertyNode))
                             .build();

            if (currentVersion.getHash().equals(result.getHash())) {
                result.setCurrentProject(true);
            }
        }
        return result;
    }


}
