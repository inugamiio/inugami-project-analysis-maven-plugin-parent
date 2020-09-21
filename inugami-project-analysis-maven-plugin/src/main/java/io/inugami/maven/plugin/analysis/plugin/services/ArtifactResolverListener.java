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
package io.inugami.maven.plugin.analysis.plugin.services;

import io.inugami.api.loggers.Loggers;
import io.inugami.maven.plugin.analysis.api.models.Gav;
import lombok.Getter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.versioning.VersionRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class ArtifactResolverListener implements ResolutionListener {
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final Set<Gav>  artifacts       = new HashSet<>();
    private       Gav       parentArtifact  = null;
    private       Gav       currentArtifact = null;
    private final List<Gav> buffer          = new ArrayList<>();

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void testArtifact(final Artifact node) {

    }


    @Override
    public void startProcessChildren(final Artifact artifact) {
        Loggers.DEBUG.info("artifact : {}", artifact);
        final Gav gav = buffer.get(buffer.indexOf(buildGav(artifact)));
        gav.setParent(parentArtifact);
        if (parentArtifact == null) {
            artifacts.add(gav);
        }
        parentArtifact = gav;
    }

    @Override
    public void endProcessChildren(final Artifact artifact) {

        final Gav gav = buffer.get(buffer.indexOf(buildGav(artifact)));
        parentArtifact = gav.getParent();
        buffer.remove(gav);
    }

    @Override
    public void includeArtifact(final Artifact artifact) {
        if (Loggers.DEBUG.isDebugEnabled()) {
            Loggers.DEBUG.info("artifact : {}", artifact);
        }
        final Gav gav = buildGav(artifact);

        currentArtifact = gav;
        buffer.add(gav);
        if (parentArtifact != null) {
            parentArtifact.addDependency(gav);
        }
    }

    private Gav buildGav(final Artifact artifact) {
        return Gav.builder()
                  .groupId(artifact.getGroupId())
                  .artifactId(artifact.getArtifactId())
                  .version(artifact.getVersion())
                  .type(artifact.getType())
                  .build();
    }


    // =========================================================================
    // OVERRIDES
    // =========================================================================
    @Override
    public void omitForNearer(final Artifact omitted, final Artifact kept) {

    }

    @Override
    public void updateScope(final Artifact artifact, final String scope) {

    }

    @Override
    public void manageArtifact(final Artifact artifact, final Artifact replacement) {

    }

    @Override
    public void omitForCycle(final Artifact artifact) {

    }

    @Override
    public void updateScopeCurrentPom(final Artifact artifact, final String ignoredScope) {

    }

    @Override
    public void selectVersionFromRange(final Artifact artifact) {

    }

    @Override
    public void restrictRange(final Artifact artifact, final Artifact replacement, final VersionRange newRange) {

    }


}
