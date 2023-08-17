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
package io.inugami.maven.plugin.analysis.api.models.rest;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.*;

import java.util.Collections;
import java.util.List;


@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder(toBuilder = true)
public final class RestApi implements JsonObject {
    private static final long serialVersionUID = 2101356331716509852L;

    @ToString.Include
    private String             name;
    private String             description;
    @ToString.Include
    private String             baseContext;
    @EqualsAndHashCode.Include
    private List<RestEndpoint> endpoints;

    public RestApi orderEndPoint() {
        if (endpoints != null) {
            Collections.sort(endpoints);
        }
        return this;
    }
}
