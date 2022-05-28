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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.impl.ArtifactResolver;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

@Slf4j
@RequiredArgsConstructor
public class MavenArtifactResolver {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final RepositorySystemSession repoSession;
    private final ArtifactResolver        artifactResolver;


    // =========================================================================
    // API
    // =========================================================================
    public Artifact resolve(final String gav) {
        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(new DefaultArtifact(gav));
        ArtifactResult result = null;
        try {
            result = artifactResolver.resolveArtifact(repoSession, request);
        }
        catch (ArtifactResolutionException e) {
            log.error(e.getMessage(), e);
        }
        return result == null ? null : result.getArtifact();
    }

}
