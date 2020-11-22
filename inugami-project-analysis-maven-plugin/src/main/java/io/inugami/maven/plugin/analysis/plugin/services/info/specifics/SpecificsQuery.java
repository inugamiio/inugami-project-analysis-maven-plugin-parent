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
package io.inugami.maven.plugin.analysis.plugin.services.info.specifics;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.tools.ConsoleTools;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import io.inugami.maven.plugin.analysis.api.tools.rendering.Neo4jRenderingUtils;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;
import org.neo4j.driver.Record;
import org.neo4j.driver.internal.InternalRelationship;
import org.neo4j.driver.types.Node;

import java.io.File;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
public class SpecificsQuery implements ProjectInformation {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String QUERY_PATH      = "inugami.query.path";
    public static final String SKIP_PROPERTIES = "inugami.skip.properties";

    // =========================================================================
    // PROCESS
    // =========================================================================
    @Override
    public void process(final MavenProject project, final ConfigHandler<String, String> configuration) {
        final Neo4jDao dao = new Neo4jDao(configuration);

        final boolean interactive  = configuration.grabBoolean("interactive");
        final Gav     gav          = loadCurrentGav(convertMavenProjectToGav(project), interactive);
        final File    templatePath = loadQuery(configuration, interactive);
        final String  skipPattern  = configuration.get(SKIP_PROPERTIES);
        final Pattern skipRegex    = skipPattern == null ? null : Pattern.compile(skipPattern);

        if (templatePath == null) {
            log.info("no query define");
        }
        else {
            final Map<String, String> properties = new HashMap<>(configuration);
            properties.put("artifactId", gav.getArtifactId());
            properties.put("groupId", gav.getGroupId());
            properties.put("version", gav.getVersion());

            final String query = TemplateRendering.render(templatePath, properties);
            log.info("query:\n{}", query);
            final List<Record> resultSet = dao.search(query);

            String result = null;
            if (resultSet == null || resultSet.isEmpty()) {
                result = "no result";
            }
            else {
                result = renderResultSet(resultSet, skipRegex,configuration);
            }
            log.info("\n{}", result);
        }
        dao.shutdown();
    }


    // =========================================================================
    // LOAD INFORMATIONS
    // =========================================================================
    private Gav loadCurrentGav(final Gav currentArtifact, final boolean interactive) {
        final Gav gav = currentArtifact;

        if (interactive) {
            final Gav.GavBuilder gavBuilder = Gav.builder();
            gavBuilder.groupId(ConsoleTools.askQuestion("GroupId ?", currentArtifact.getGroupId()));
            gavBuilder.artifactId(ConsoleTools.askQuestion("ArtifactId ?", currentArtifact.getArtifactId()));
            gavBuilder.artifactId(ConsoleTools.askQuestion("Version ?", currentArtifact.getVersion()));
        }
        return gav;
    }

    private File loadQuery(final ConfigHandler<String, String> configuration, final boolean interactive) {
        File   result = null;
        String path   = configuration.get(QUERY_PATH);
        if (interactive || path == null) {
            path = ConsoleTools.askQuestion("Where is the query path ?");
        }

        if (path != null) {
            result = new File(path);
        }
        return result;
    }

    // =========================================================================
    // RENDERING
    // =========================================================================
    private String renderResultSet(final List<Record> resultSet, final Pattern skipRegex,final ConfigHandler<String, String> configuration) {
        final Map<String, Collection<DataRow>> data          = new LinkedHashMap<>();
        final Collection<DataRow>              nodes         = new HashSet<>();
        final Collection<DataRow>              relationships = new HashSet<>();
        if (resultSet != null || !resultSet.isEmpty()) {
            for (final Record record : resultSet) {
                final Map<String, Object> dataRecord = Optional.ofNullable(record.asMap()).orElse(new HashMap<>());
                for (final Map.Entry<String, Object> entry : dataRecord.entrySet()) {
                    final Object element = entry.getValue();

                    if (element instanceof Node) {
                        nodes.add(renderNodes((Node) element, skipRegex));
                    }
                    else if (element instanceof InternalRelationship) {
                        relationships.add(renderRelationships((InternalRelationship) element, skipRegex));
                    }
                }
            }


        }

        data.put("nodes", nodes);
        data.put("relationships", relationships);
        return Neo4jRenderingUtils.rendering(data,configuration,"specificQuery");
    }


    private DataRow renderNodes(final Node data, final Pattern skipRegex) {
        final DataRow result = new DataRow();

        final String label = String.join(":", Optional.ofNullable(data.labels()).orElse(List.of()));
        final String name  = Neo4jRenderingUtils.getNodeName(data);

        final StringBuilder uid = new StringBuilder();
        uid.append("(");
        if (label != null && !label.trim().isEmpty()) {
            uid.append("<").append(label).append(">");
        }
        uid.append(name);
        uid.append(")");
        result.setUid(uid.toString());

        final Map<String, Serializable> properties = new LinkedHashMap<>();
        properties.put("id", data.id());
        properties.put("name", result.getUid());
        properties.putAll(buildProperties(data.asMap(), skipRegex));
        result.setProperties(properties);
        return result;
    }


    private DataRow renderRelationships(final InternalRelationship data, final Pattern skipRegex) {
        final DataRow       result = new DataRow();
        final StringBuilder uid    = new StringBuilder();
        uid.append("(").append(data.startNodeId()).append(")-[");
        uid.append(data.type());
        uid.append("]->(").append(data.endNodeId()).append(")");
        result.setUid(uid.toString());

        final Map<String, Serializable> properties = new LinkedHashMap<>();
        properties.put("id", data.id());
        properties.put("name", result.getUid());
        properties.putAll(buildProperties(data.asMap(), skipRegex));
        result.setProperties(properties);
        return result;
    }

    private Map<String, Serializable> buildProperties(final Map<String, Object> data, final Pattern skipRegex) {
        final Map<String, Serializable> result        = new LinkedHashMap<>();
        final Map<String, Object>       rowProperties = Optional.ofNullable(data).orElse(new LinkedHashMap<>());
        for (final Map.Entry<String, Object> entry : rowProperties.entrySet()) {
            if (entry.getValue() != null && accept(entry.getKey(), skipRegex)) {

                if (entry.getValue() instanceof Serializable) {
                    result.put(entry.getKey(), (Serializable) entry.getValue());
                }
                else {
                    result.put(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }
        }
        return result;
    }

    private boolean accept(final String key, final Pattern skipRegex) {
        boolean result = true;
        if (key == null) {
            result = false;
        }
        else if (skipRegex != null) {
            result = !skipRegex.matcher(key).matches();
        }
        return result;
    }
}
