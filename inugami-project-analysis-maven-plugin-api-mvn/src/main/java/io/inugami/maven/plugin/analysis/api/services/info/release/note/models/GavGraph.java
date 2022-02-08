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
package io.inugami.maven.plugin.analysis.api.services.info.release.note.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Setter
@Getter
public class GavGraph implements JsonObject {
    @EqualsAndHashCode.Include
    private final String  artifactId;
    @EqualsAndHashCode.Include
    private final String  groupId;
    @EqualsAndHashCode.Include
    private final String  type;
    @EqualsAndHashCode.Include
    private final String  version;
    private final String  hash;
    private       boolean currentProject;

    @Builder
    public GavGraph(final String artifactId, final String groupId, final String type, final String version) {
        this.artifactId     = artifactId;
        this.groupId        = groupId;
        this.type           = type;
        this.version        = version;
        this.currentProject = currentProject;
        this.hash           = String.join(":", List.of(String.valueOf(groupId),
                                                       String.valueOf(artifactId),
                                                       String.valueOf(version),
                                                       String.valueOf(type)));
    }
}
