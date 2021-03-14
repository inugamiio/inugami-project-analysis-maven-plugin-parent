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
package io.inugami.maven.plugin.analysis.plugin.services.info.release.note.models;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

import java.util.LinkedHashSet;
import java.util.Set;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Getter
public class FlywayDTO implements Comparable<FlywayDTO>, JsonObject {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    @EqualsAndHashCode.Include
    private String      id;
    private String      name;
    private String      type;
    private String      content;
    private Set<String> projectsUsing;

    // =========================================================================
    // API
    // =========================================================================
    public FlywayDTO addProjectUsing(final String projectName) {
        if (projectsUsing == null) {
            projectsUsing = new LinkedHashSet<>();
        }
        if (projectName != null) {
            projectsUsing.add(projectName);
        }
        return this;
    }

    // =========================================================================
    // OVERRIDES
    // =========================================================================
    @Override
    public int compareTo(final FlywayDTO other) {
        return String.valueOf(id).compareTo(String.valueOf(other == null ? null : other.getId()));
    }
}
