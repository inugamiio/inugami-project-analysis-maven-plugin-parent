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

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

import java.io.Serializable;
import java.util.Map;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Relationship implements JsonObject {

    private static final long serialVersionUID = 973646684487506001L;
    @EqualsAndHashCode.Include
    private final String from;
    @EqualsAndHashCode.Include
    private final String to;
    @EqualsAndHashCode.Include
    private final String type;

    private final Map<String, Serializable> properties;
}
