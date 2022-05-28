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
package io.inugami.maven.plugin.analysis.plugin.mojo;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.maven.project.MavenProject;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MojoUtils {

    public static Map<String, String> convertToMap(final MavenProject mavenProject) {
        final Map<String, String> result = new LinkedHashMap<>();

        if (mavenProject.getProperties() != null) {
            for (Map.Entry<Object, Object> entry : mavenProject.getProperties().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }

        result.put("project.groupId", mavenProject.getGroupId());
        result.put("project.artifactId", mavenProject.getArtifactId());
        result.put("project.packaging", mavenProject.getPackaging());
        result.put("project.version", mavenProject.getVersion());
        result.put("project.description", orElseEmpty(mavenProject.getDescription()));
        result.put("project.url", orElseEmpty(mavenProject.getUrl()));

        if (mavenProject.getParent() != null) {
            final MavenProject parent = mavenProject.getParent();
            result.put("parent.groupId", parent.getGroupId());
            result.put("parent.artifactId", parent.getArtifactId());
            result.put("parent.packaging", parent.getPackaging());
            result.put("parent.version", parent.getVersion());
            result.put("parent.description", orElseEmpty(parent.getDescription()));
            result.put("parent.url", orElseEmpty(parent.getUrl()));
        }

        return result;
    }

    private static String orElseEmpty(final String value) {
        return value == null ? "" : value;
    }

}
