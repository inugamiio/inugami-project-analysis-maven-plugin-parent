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

@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Builder(toBuilder = true)
public class RestEndpoint implements JsonObject, Comparable<RestEndpoint> {
    private static final long serialVersionUID = -6556991122270585469L;
    private final String nickname;
    private final String uri;
    private final String verb;
    private final String headers;
    private final String body;
    private final String consume;
    private final String produce;
    private final String description;
    private final String responseType;
    private final String uid;

    @Override
    public int compareTo(final RestEndpoint value) {
        int result = 0;
        if (value.getUri().equals(this.getUri())) {
            result = value.getVerb().compareTo(this.getVerb());
        }
        else {
            result = value.getUri().compareTo(this.getUri());
        }
        return result;
    }
}
