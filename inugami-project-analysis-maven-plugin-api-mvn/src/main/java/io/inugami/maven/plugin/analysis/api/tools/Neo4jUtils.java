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
package io.inugami.maven.plugin.analysis.api.tools;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.internal.value.ListValue;
import org.neo4j.driver.internal.value.NodeValue;
import org.neo4j.driver.types.Relationship;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.buildNodeVersion;

@SuppressWarnings({"java:S6213"})
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Neo4jUtils {

    // =========================================================================
    // API
    // =========================================================================
    public static Gav convertMavenProjectToGav(final MavenProject project) {
        return project == null ? null : Gav.builder()
                                           .groupId(project.getGroupId())
                                           .artifactId(project.getArtifactId())
                                           .version(project.getVersion())
                                           .type(project.getPackaging())
                                           .build();
    }

    public static String ifNull(final String value, final Supplier<String> handler) {
        String result = value;
        if (result == null) {
            result = handler.get();
        }
        return result;
    }

    public static Node buildArtifactVersion(final MavenProject project,
                                            final ConfigHandler<String, String> configuration) {
        final boolean useMavenProject = Boolean.parseBoolean(configuration.grabOrDefault("useMavenProject", "false"));
        return useMavenProject
                ? buildNodeVersion(project)
                : buildNodeVersion(buildGav(project, configuration));
    }

    public static Gav buildGav(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final boolean useMavenProject = Boolean.parseBoolean(configuration.grabOrDefault("useMavenProject", "false"));

        if (useMavenProject) {
            return Gav.builder()
                      .groupId(project.getGroupId())
                      .artifactId(project.getArtifactId())
                      .version(project.getVersion())
                      .type(project.getPackaging())
                      .build();
        } else {
            final String groupId = ifNull(configuration.get(GROUP_ID),
                                          () -> ConsoleTools.askQuestion("groupId ?", project.getGroupId()));

            final String artifactId = ifNull(configuration.get(ARTIFACT_ID),
                                             () -> ConsoleTools.askQuestion("artifactId ?", project.getArtifactId()));

            final String type = ifNull(configuration.get(TYPE),
                                       () -> ConsoleTools.askQuestion("type ?", project.getPackaging()));

            final String version = ifNull(configuration.get(VERSION),
                                          () -> ConsoleTools.askQuestion("version ?", project.getVersion()));
            return Gav.builder()
                      .groupId(groupId)
                      .artifactId(artifactId)
                      .version(version)
                      .type(type)
                      .build();
        }
    }

    public static Map<String, Collection<DataRow>> extractDataFromResultSet(final List<Record> resultSet,
                                                                            final BiConsumer<Map<String, Collection<DataRow>>, Map<String, Object>> consumer) {

        final Map<String, Collection<DataRow>> data = new LinkedHashMap<>();
        if (resultSet != null && !resultSet.isEmpty()) {
            for (final Record record : resultSet) {
                if (record != null) {
                    final Map<String, Object> recordData = record.asMap();
                    consumer.accept(data, recordData);
                }
            }
        }
        return data;
    }


    public static String getNodeName(final Object node) {
        String result = null;
        if (node == null) {
            //nothing
        } else if (node instanceof org.neo4j.driver.types.Node) {
            result = retrieve("name", (org.neo4j.driver.types.Node) node);
        } else if (node instanceof NodeValue) {
            result = String.valueOf(((NodeValue) node).get("name"));
        }
        return result == null ? null : result.replace("\"", "");
    }

    public static NodeValue extractNode(final String nodeName, final Record record) {
        NodeValue result = null;
        Value     node   = null;
        if (record != null) {
            node = record.get(nodeName);
        }
        if (node != null && !node.isNull() && node instanceof NodeValue) {
            result = (NodeValue) node;
        }
        return result;
    }

    public static List<Relationship> extractRelationships(final String nodeName, final Record record) {
        final List<Relationship> result = new ArrayList<>();
        Value                    node   = null;
        if (record != null) {
            node = record.get(nodeName);
        }
        if (node != null && !node.isNull()) {
            if (node instanceof Relationship) {
                result.add((Relationship) node);
            } else if (node instanceof ListValue) {
                final List<Object> nodes = ((ListValue) node).asList();
                Optional.ofNullable(nodes)
                        .orElse(new ArrayList<>())
                        .stream()
                        .filter(Objects::nonNull)
                        .filter(Relationship.class::isInstance)
                        .map(o -> (Relationship) o)
                        .forEach(result::add);
            }
        }
        return result;
    }

    public static org.neo4j.driver.types.Node getNode(final Object node) {
        return node instanceof org.neo4j.driver.types.Node ? (org.neo4j.driver.types.Node) node : null;
    }

    public static InternalRelationship getRelationship(final Object node) {
        return node instanceof InternalRelationship ? (InternalRelationship) node : null;
    }


    public static String retrieve(final String key, final org.neo4j.driver.types.Node node) {
        String result = null;
        if (node != null) {
            final Value value = node.get(key);
            if (value != null && !value.isNull()) {
                result = value.asString();
            }
        }
        return result;
    }

    public static void ifPropertyNotNull(final String key, final org.neo4j.driver.types.Node node,
                                             final Consumer<Object> consumer) {
        if (node != null) {
            final Map<String, Object> values = node.asMap();
            if (values != null && values.containsKey(key) && values.get(key) != null) {
                consumer.accept(values.get(key));
            }
        }
    }

    public static String retrieveString(final String key, final Map<String, Object> data) {
        String result = null;
        if (data != null && data.containsKey(key)) {
            result = String.valueOf(data.get(key));
        }
        return result;
    }


    public static boolean isNotNull(final Value value) {
        return value != null && !value.isNull();
    }
}
