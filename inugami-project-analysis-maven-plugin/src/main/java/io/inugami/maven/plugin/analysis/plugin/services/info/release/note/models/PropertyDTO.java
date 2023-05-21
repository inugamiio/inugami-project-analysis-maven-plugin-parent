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
import io.inugami.maven.plugin.analysis.api.constant.Constants;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@ToString
@Getter
public class PropertyDTO implements Comparable<PropertyDTO>, JsonObject {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final long    serialVersionUID = 6933793488352573741L;
    @EqualsAndHashCode.Include
    private final        String  name;
    private final        String  propertyType;
    private final        String  defaultValue;
    private final        boolean mandatory;
    private final        boolean useForConditionalBean;
    private final        String  constraintDetail;
    private final        String  constraintType;
    @EqualsAndHashCode.Include
    private final        String  artifact;

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public int compareTo(final PropertyDTO other) {
        final String current = String.join(Constants.UNDERSCORE, String.valueOf(artifact), String.valueOf(name));
        final String otherProperty = String.join(Constants.UNDERSCORE,
                                                 String.valueOf(other == null ? null : other.getArtifact()),
                                                 String.valueOf(other == null ? null : other.getName()));
        return current.compareTo(otherProperty);
    }

}
