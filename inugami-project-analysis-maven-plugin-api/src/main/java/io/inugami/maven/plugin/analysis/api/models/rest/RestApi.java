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


@EqualsAndHashCode
@ToString
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
public class RestApi implements JsonObject {
    private static final long               serialVersionUID = 2101356331716509852L;
    private final        String             name;
    private final        String             description;
    private final        String             baseContext;
    private final        List<RestEndpoint> endpoints;

    public RestApi orderEndPoint() {
        if (endpoints != null) {
            Collections.sort(endpoints, (ref, value) -> {
                int result = 0;
                if (value.getUri() != null && value.getUri().equals(ref.getUri())) {
                    result = String.valueOf(value.getVerb()).compareTo(String.valueOf(ref.getVerb()));
                } else {
                    result = String.valueOf(value.getUri()).compareTo(String.valueOf(ref.getUri()));
                }
                return result;
            });
        }
        return this;
    }
}
