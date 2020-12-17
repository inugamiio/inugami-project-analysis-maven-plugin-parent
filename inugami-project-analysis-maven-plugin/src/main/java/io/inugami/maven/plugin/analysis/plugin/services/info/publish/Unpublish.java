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
package io.inugami.maven.plugin.analysis.plugin.services.info.publish;

import io.inugami.api.exceptions.UncheckedException;
import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.tools.ConsoleTools;
import io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j.Neo4jWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.project.MavenProject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
public class Unpublish implements ProjectInformation {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String GROUP_ID    = "groupId";
    public static final String ARTIFACT_ID = "artifactId";
    public static final String TYPE        = "type";
    public static final String VERSION     = "version";
    public static final String ENV         = "env";
    public static final String LEVEL       = "envLevel";
    public static final String ENV_TYPE    = "envType";
    public static final String FIELD_TYPE  = "type";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final InfoContext context) {

        final ScanNeo4jResult data = buildData(context.getConfiguration(), context.getProject());

        final Neo4jWriter neo4jWriter = new Neo4jWriter().init(context.getConfiguration());
        neo4jWriter.appendData(data);
        neo4jWriter.write();
        neo4jWriter.shutdown(null);
    }


    // =========================================================================
    // BUILDERS
    // =========================================================================
    private ScanNeo4jResult buildData(final ConfigHandler<String, String> configuration, final MavenProject project) {
        final ScanNeo4jResult result          = new ScanNeo4jResult();
        final boolean         justThisVersion = Boolean
                .parseBoolean(configuration.grabOrDefault("justThisVersion", "false"));

        final Node            artifactNode    = buildArtifactVersion(project, configuration);
        final Node            env             = buildEnvNode(configuration);

        result.addDeleteScript(buildDeletePublishRelation(artifactNode, env, justThisVersion));
        return result;
    }

    public static List<String> buildDeletePublishRelation(final Node artifactNode, final Node env,
                                                          final boolean justThisVersion) {
        final List<String> result = new ArrayList<>();

        result.add(buildDeleteDeploy(artifactNode, env, justThisVersion));
        result.add(buildDeleteHaveArtifactVersion(artifactNode, env, justThisVersion));
        return result;
    }


    private static String buildDeleteDeploy(final Node artifactNode, final Node env, final boolean justThisVersion) {
        final JsonBuilder query = new JsonBuilder();
        query.write("MATCH (v:Version)-[r:DEPLOY]->(env:Env)");
        query.write(" where");
        if (justThisVersion) {
            query.write(" v.name=").valueQuot(artifactNode.getUid());
        }
        else {
            query.write(" v.groupId=").valueQuot(artifactNode.getProperties().get("groupId"));
            query.write(" and v.artifactId=").valueQuot(artifactNode.getProperties().get("artifactId"));
        }

        query.write(" and ");
        query.write(" env.name=").valueQuot(env.getUid());
        query.write(" delete r");
        final String result = query.toString();
        log.info(result);
        return result;
    }

    private static String buildDeleteHaveArtifactVersion(final Node artifactNode, final Node env,
                                                         final boolean justThisVersion) {
        final JsonBuilder query = new JsonBuilder();
        query.write("MATCH (env:Env)-[r:HAVE_ARTIFACT_VERSION]->(v:Version)");
        query.write(" where");
        if (justThisVersion) {
            query.write(" v.name=").valueQuot(artifactNode.getUid());
        }
        else {
            query.write(" v.groupId=").valueQuot(artifactNode.getProperties().get("groupId"));
            query.write(" and v.artifactId=").valueQuot(artifactNode.getProperties().get("artifactId"));
        }

        query.write(" and ");
        query.write(" env.name=").valueQuot(env.getUid());
        query.write(" delete r");
        final String result = query.toString();
        log.info(result);
        return result;
    }

    private Gav buildGav(final ConfigHandler<String, String> configuration, final MavenProject project) {
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

    private Node buildEnvNode(final ConfigHandler<String, String> configuration) {
        final Node.NodeBuilder builder = Node.builder();
        builder.type("Env");

        final String uid = ifNull(configuration.get(ENV), () -> ConsoleTools.askQuestion("environment ?", "DEV-1"));
        builder.name(uid);
        builder.uid(uid);

        final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();


        final String levelStr = ifNull(configuration.get(LEVEL),
                                       () -> ConsoleTools.askQuestion("environment level ?", "1"));
        final int level = convertLevel(levelStr);
        properties.put("level", level);


        final String envType = ifNull(configuration.get(ENV_TYPE),
                                      () -> ConsoleTools.askQuestion("environment type ?"));

        if (envType != null) {
            properties.put(FIELD_TYPE, envType);
        }

        builder.properties(properties);

        return builder.build();
    }


    // =========================================================================
    // TOOLS
    // =========================================================================
    private int convertLevel(final String value) {
        int result = 0;
        try {
            result = Integer.parseInt(value);
        }
        catch (final Exception e) {
            throw new UncheckedException("invalid environment level :" + value);
        }
        if (result < 0) {
            result = 0;
        }
        return result;
    }
}
