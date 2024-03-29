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
import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.api.tools.ConsoleTools;
import io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j.Neo4jWriter;
import org.apache.maven.project.MavenProject;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

@SuppressWarnings({"java:S3252"})
public class Publish implements ProjectInformation {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String     ENV                   = "env";
    public static final String     LEVEL                 = "envLevel";
    public static final String     ENV_TYPE              = "envType";
    public static final String     FIELD_TYPE            = "type";
    public static final String     DEPLOY                = "DEPLOY";
    public static final String     REL_ENV_TYPE          = "ENV_TYPE";
    public static final String     HAVE_ARTIFACT_VERSION = "HAVE_ARTIFACT_VERSION";
    public static final ZoneOffset UTC_OFFSET            = ZoneOffset.systemDefault().getRules().getOffset(LocalDateTime.now(ZoneOffset.UTC));

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
        final ScanNeo4jResult result = new ScanNeo4jResult();

        final boolean autoUnpublish   = Boolean.parseBoolean(configuration.grabOrDefault("autoUnpublish", "false"));
        final boolean justThisVersion = Boolean.parseBoolean(configuration.grabOrDefault("justThisVersion", "false"));
        final Node    artifactNode    = buildArtifactVersion(project, configuration);
        final Node    env             = buildEnvNode(configuration);

        final Node envType = buildEnvType(env);

        if (autoUnpublish) {
            final String previousEnv = ifNull(configuration.get("previousEnv"),
                                              () -> ConsoleTools.askQuestion("Previous environment ?"));
            if (previousEnv != null) {
                result.addDeleteScript(Unpublish.buildDeletePublishRelation(artifactNode,
                                                                            Node.builder().uid(previousEnv).build(),
                                                                            justThisVersion));
            }
            result.addDeleteScript(Unpublish.buildDeletePublishRelation(artifactNode, env, justThisVersion));
        }

        result.addNode(artifactNode, env);
        result.addRelationship(Relationship.builder()
                                           .from(artifactNode.getUid())
                                           .to(env.getUid())
                                           .type(DEPLOY)
                                           .properties(buildDeployProperties())
                                           .build());

        result.addRelationship(Relationship.builder()
                                           .from(env.getUid())
                                           .to(artifactNode.getUid())
                                           .type(HAVE_ARTIFACT_VERSION)
                                           .properties(buildDeployProperties())
                                           .build());
        if (envType != null) {
            result.addNode(envType);
            result.addRelationship(Relationship.builder()
                                               .from(env.getUid())
                                               .to(envType.getUid())
                                               .type(REL_ENV_TYPE)
                                               .build());
        }

        return result;
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


    private Node buildEnvType(final Node env) {
        final Serializable type = env.getProperties().get(FIELD_TYPE);
        return type == null ? null : Node.builder()
                                         .type("EnvType")
                                         .uid(String.valueOf(type))
                                         .name(String.valueOf(type))
                                         .build();
    }

    private LinkedHashMap<String, Serializable> buildDeployProperties() {
        final LinkedHashMap<String, Serializable> result = new LinkedHashMap<>();
        final LocalDateTime                       now    = LocalDateTime.now();
        result.put("date", DateTimeFormatter.ISO_DATE_TIME.format(now));
        result.put("timestamp", now.toEpochSecond(UTC_OFFSET));
        result.put("dateUtc", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now(ZoneOffset.UTC)));
        result.put("timestampUtc", now.toEpochSecond(ZoneOffset.UTC));
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================


    private int convertLevel(final String value) {
        int result = 0;
        try {
            result = Integer.parseInt(value);
        } catch (final Exception e) {
            throw new UncheckedException("invalid environment level :" + value);
        }
        if (result < 0) {
            result = 0;
        }
        return result;
    }
}
