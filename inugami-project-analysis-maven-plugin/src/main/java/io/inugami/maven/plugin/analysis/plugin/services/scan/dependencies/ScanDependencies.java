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
package io.inugami.maven.plugin.analysis.plugin.services.scan.dependencies;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ProjectScanner;
import io.inugami.maven.plugin.analysis.api.models.*;
import io.inugami.maven.plugin.analysis.api.tools.BuilderTools;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ScanDependencies implements ProjectScanner {

    public static final String DEPENDENCY         = "DEPENDENCY";
    public static final String PROJECT_DEPENDENCY = "PROJECT_DEPENDENCY";

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> scan(final ScanConext context) {
        final ScanNeo4jResult result       = ScanNeo4jResult.builder().build();
        final Set<Gav>        dependencies = context.getDependencies();

        final List<String> projectBaseNames = new ArrayList<>();
        final String projectBaseName = context.getConfiguration()
                                              .get("inugami.maven.plugin.analysis.project.base.name");



        if (projectBaseName != null) {
            for (final String element : projectBaseName.split(",")) {
                projectBaseNames.add(element.trim());
            }
        }

        for (final Gav gav : dependencies) {
            addDependencies(null, gav, result, projectBaseNames);
        }

        final Node currentVersion = BuilderTools.buildNodeVersion(context.getProject());
        if (BuilderTools.isSnapshotNode(currentVersion)) {
            result.addNodeToDelete(currentVersion.getUid());
        }

        addDirectDependencies(currentVersion, context.getDirectDependencies(), result,
                              context.getProject().getGroupId(), projectBaseNames);
        return List.of(result);
    }

    private void addDirectDependencies(final Node currentVersion,
                                       final Set<Gav> directDependencies,
                                       final ScanNeo4jResult result,
                                       final String groupId,
                                       final List<String> projectBaseNames) {
        if (directDependencies != null) {
            for (final Gav dependency : directDependencies) {
                final Node dependencyNode = BuilderTools.buildNodeVersion(dependency);
                result.addNode(dependencyNode);
                result.addRelationship(Relationship.builder()
                                                   .from(currentVersion.getUid())
                                                   .to(dependencyNode.getUid())
                                                   .type(chooseRelationshiptType(groupId, dependency.getGroupId(),
                                                                                 projectBaseNames))
                                                   .build());
            }

        }
    }

    private String chooseRelationshiptType(final String currentGroupId, final String dependencyGroupId,
                                           final List<String> projectBaseNames) {
        boolean projectDependency = false;
        for (final String baseName : projectBaseNames) {
            if (currentGroupId.startsWith(baseName) && dependencyGroupId.startsWith(baseName)) {
                projectDependency = true;
                break;
            }
        }
        return projectDependency ? PROJECT_DEPENDENCY : DEPENDENCY;
    }

    private Gav buildGav(final MavenProject project) {
        return Gav.builder()
                  .groupId(project.getGroupId())
                  .artifactId(project.getArtifactId())
                  .version(project.getVersion())
                  .type(project.getPackaging())
                  .build();
    }

    private void addDependencies(final Gav parent, final Gav gav, final ScanNeo4jResult result,
                                 final List<String> projectBaseNames) {
        if (gav != null) {
            final Node node         = BuilderTools.buildNodeVersion(gav);
            final Node artifactNode = BuilderTools.buildGavNodeArtifact(gav);

            result.addNode(node);
            result.addNode(artifactNode);

            result.addRelationship(BuilderTools.buildRelationshipArtifact(node, artifactNode));
            if (parent != null) {
                result.addRelationship(Relationship.builder()
                                                   .from(BuilderTools.buildNodeVersion(parent).getUid())
                                                   .to(node.getUid())
                                                   .type(chooseRelationshiptType(parent.getGroupId(), gav.getGroupId(),
                                                                                 projectBaseNames))
                                                   .build());
            }

            if (gav.getDependencies() != null) {
                for (final Gav children : gav.getDependencies()) {
                    addDependencies(gav, children, result, projectBaseNames);
                }
            }
        }
    }


}
