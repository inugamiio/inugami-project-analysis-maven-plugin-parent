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
package io.inugami.maven.plugin.analysis.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class MavenGav implements Artifact {
    private    String                       groupId;
    private String                       artifactId;
    private String                       version;
    private String                       packaging;
    private String                       type;
    private String                       scope;
    private String                       classifier;
    private File                         file;
    private String                       baseVersion;
    private String                       id;
    private Collection<ArtifactMetadata> metadataList;
    private ArtifactRepository           repository;
    private String                       dependencyConflictId;
    private String                       downloadUrl;
    private ArtifactFilter               dependencyFilter;
    private ArtifactHandler              artifactHandler;
    private List<String>                 dependencyTrail;
    private VersionRange                 versionRange;
    private boolean                      resolved;
    private boolean                      optional;
    private List<ArtifactVersion>        availableVersions;
    private ArtifactVersion              selectedVersion;

    @Override
    public boolean hasClassifier() {
        return classifier != null;
    }


    @Override
    public void addMetadata(final ArtifactMetadata artifactMetadata) {
        if (metadataList == null) {
            metadataList = new ArrayList<>();
        }
        if (artifactMetadata != null) {
            metadataList.add(artifactMetadata);
        }
    }


    @Override
    public void updateVersion(final String s, final ArtifactRepository artifactRepository) {

    }


    @Override
    public void selectVersion(final String s) {

    }


    @Override
    public boolean isSnapshot() {
        return version != null && version.endsWith("SNAPSHOT");
    }


    @Override
    public void setResolvedVersion(final String s) {

    }


    @Override
    public boolean isRelease() {
        return !isSnapshot();
    }

    @Override
    public void setRelease(final boolean b) {

    }


    @Override
    public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
        return false;
    }

    @Override
    public int compareTo(final Artifact other) {
        if (other == null) {
            return -1;
        }

        final String currentGav = String.join(":", groupId, artifactId, packaging, version);
        final String otherGav = String.join(":", other.getGroupId(), other.getArtifactId(), other.getArtifactHandler().getPackaging(), other.getVersion());
        return currentGav.compareTo(otherGav);
    }
}
