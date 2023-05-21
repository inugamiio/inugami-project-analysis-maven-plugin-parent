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
package io.inugami.maven.plugin.analysis.plugin.services.scan.parents;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ProjectScanner;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import org.apache.maven.project.MavenProject;

import java.util.List;

import static io.inugami.maven.plugin.analysis.api.tools.BuilderTools.*;

public class ScanParent implements ProjectScanner {


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<JsonObject> scan(final ScanConext context) {
        final ScanNeo4jResult result = ScanNeo4jResult.builder().build();

        extractAllParent(null, context.getProject(), result);
        return List.of(result);
    }

    private void extractAllParent(final MavenProject childProject, final MavenProject currentProject,
                                  final ScanNeo4jResult result) {
        final Node node = buildNodeVersion(currentProject);

        if (isSnapshotNode(node)) {
            result.addNodeToDelete(node.getUid());
        }

        final Node artifactNode = buildArtifactNode(currentProject);
        result.addNode(node);
        result.addNode(artifactNode);
        result.addRelationship(buildRelationshipArtifact(node, artifactNode));


        if (childProject != null) {
            final Node childNode = buildNodeVersion(childProject);
            result.addRelationship(Relationship.builder()
                                               .from(childNode.getUid())
                                               .to(node.getUid())
                                               .type(PARENT)
                                               .build());
        }

        if (currentProject.getParent() != null) {
            extractAllParent(currentProject, currentProject.getParent(), result);
        }
    }


}
