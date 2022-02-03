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

import lombok.*;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Getter
@Builder
@RequiredArgsConstructor
public class GavGraph {
    @EqualsAndHashCode.Include
    private final String  artifactId;
    @EqualsAndHashCode.Include
    private final String  groupId;
    @EqualsAndHashCode.Include
    private final String  type;
    @EqualsAndHashCode.Include
    private final String  version;
    private final String  hash;
    private final boolean currentProject;
}
