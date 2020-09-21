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
package io.inugami.maven.plugin.analysis.api.models.neo4j;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.neo4j.driver.types.Node;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VersionConvertor {

    public static VersionNode build(final Node node) {
        return node == null ? null : VersionNode.builder()
                                                .id(node.id())
                                                .name(node.get("name").asString())
                                                .groupId(node.get("groupId").asString())
                                                .artifactId(node.get("artifactId").asString())
                                                .version(node.get("version").asString())
                                                .major(node.get("major").asInt())
                                                .minor(node.get("minor").asInt())
                                                .patch(node.get("patch").asInt())
                                                .tag(node.get("tag").asString())
                                                .packaging(node.get("packaging").asString())
                                                .build();
    }
}
