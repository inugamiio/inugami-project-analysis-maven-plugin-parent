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

import io.inugami.maven.plugin.analysis.api.models.Gav;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import org.apache.maven.project.MavenProject;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuilderTools {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final String  PARENT        = "PARENT";
    public static final String  MAJOR         = "major";
    public static final String  MINOR         = "minor";
    public static final String  PATCH         = "patch";
    public static final String  TAG           = "tag";
    public static final Pattern VERSION_REGEX = Pattern.compile(
            "(?<major>[0-9]+)((?:[.])(?<minor>[0-9]+)){0,1}((?:[.])(?<patch>[0-9]+)){0,1}((?:[-._])(?<tag>.*)){0,1}");
    public static final String  RELEASE       = "RELEASE";
    public static final String  SNAPSHOT      = "SNAPSHOT";

    // =========================================================================
    // API
    // =========================================================================
    public static boolean isSnapshotNode(final Node node) {
        return node == null || node.getProperties() == null ? false : SNAPSHOT.equals(node.getProperties().get(TAG));

    }

    public static Node buildNodeVersion(final Gav gav) {
        final Map<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put("groupId", gav.getGroupId());
        additionalInfo.put("artifactId", gav.getArtifactId());
        additionalInfo.put("version", gav.getVersion());
        additionalInfo.put("packaging", gav.getType());
        additionalInfo.put("major", extractMajorVersion(gav.getVersion()));
        additionalInfo.put("minor", extractMinorVersion(gav.getVersion()));
        additionalInfo.put("patch", extractPatchVersion(gav.getVersion()));
        additionalInfo.put("tag", extractTag(gav.getVersion()));

        return Node.builder()
                   .type("Version")
                   .uid(String.join(":", gav.getGroupId(), gav.getArtifactId(), gav.getVersion(),
                                    gav.getType()))
                   .name(gav.getVersion())
                   .properties(additionalInfo)
                   .build();
    }

    public static Node buildNodeVersion(final MavenProject project) {
        final Map<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put("groupId", project.getGroupId());
        additionalInfo.put("artifactId", project.getArtifactId());
        additionalInfo.put("version", project.getVersion());
        additionalInfo.put("packaging", project.getPackaging());
        additionalInfo.put("major", extractMajorVersion(project.getVersion()));
        additionalInfo.put("minor", extractMinorVersion(project.getVersion()));
        additionalInfo.put("patch", extractPatchVersion(project.getVersion()));
        additionalInfo.put("tag", extractTag(project.getVersion()));

        return Node.builder()
                   .type("Version")
                   .uid(String.join(":", project.getGroupId(), project.getArtifactId(), project.getVersion(),
                                    project.getPackaging()))
                   .name(project.getVersion())
                   .properties(additionalInfo)
                   .build();
    }

    public static Node buildGavNodeArtifact(final Gav gav) {
        final Map<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put("groupId", gav.getGroupId());
        additionalInfo.put("artifactId", gav.getArtifactId());
        additionalInfo.put("version", gav.getVersion());
        additionalInfo.put("packaging", gav.getType());

        return Node.builder()
                   .type("Artifact")
                   .uid(String.join(":", gav.getGroupId(), gav.getArtifactId(), gav.getType()))
                   .name(gav.getArtifactId())
                   .properties(additionalInfo)
                   .build();
    }

    public static Node buildArtifactNode(final MavenProject project) {
        final Map<String, Serializable> additionalInfo = new LinkedHashMap<>();
        additionalInfo.put("groupId", project.getGroupId());
        additionalInfo.put("artifactId", project.getArtifactId());
        additionalInfo.put("version", project.getVersion());
        additionalInfo.put("packaging", project.getPackaging());
        return Node.builder()
                   .type("Artifact")
                   .uid(String.join(":", project.getGroupId(), project.getArtifactId(), project.getPackaging()))
                   .name(project.getArtifactId())
                   .properties(additionalInfo)
                   .build();
    }


    public static Relationship buildRelationshipArtifact(final Node node, final Node artifactNode) {
        return Relationship.builder()
                           .from(artifactNode.getUid())
                           .to(node.getUid())
                           .type(RELEASE)
                           .build();
    }

    public static Integer extractMajorVersion(final String version) {
        return extractIntGroup(version, MAJOR);
    }


    public static Integer extractMinorVersion(final String version) {
        return extractIntGroup(version, MINOR);
    }

    public static Integer extractPatchVersion(final String version) {
        return extractIntGroup(version, PATCH);
    }

    public static String extractTag(final String version) {
        String        result  = null;
        final Matcher matcher = VERSION_REGEX.matcher(version);
        if (matcher.matches()) {
            result = matcher.group(TAG);
        }
        return result == null ? "" : result;
    }

    public static Integer extractIntGroup(final String version, final String groupName) {
        Integer       result  = 0;
        final Matcher matcher = VERSION_REGEX.matcher(version);
        if (matcher.matches()) {
            final String value = matcher.group(groupName);
            if (value != null) {
                result = Integer.parseInt(value);
            }
        }
        return result;
    }
}
