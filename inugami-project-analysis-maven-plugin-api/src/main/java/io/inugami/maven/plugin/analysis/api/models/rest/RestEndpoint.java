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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.api.tools.StringComparator;
import lombok.*;

import java.lang.reflect.Method;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@Setter
@Builder(toBuilder = true)
public class RestEndpoint implements JsonObject, Comparable<RestEndpoint> {
    private static final long           serialVersionUID = -6556991122270585469L;
    private              String         nickname;
    private              String         uri;
    private              String         verb;
    private              String         headers;
    private              String         body;
    private              String         bodyRequireOnly;
    private              String         consume;
    private              String         produce;
    private              String         description;
    private              String         responseType;
    private              String         responseTypeRequireOnly;
    private              String         uid;
    private              String         method;
    private              DescriptionDTO descriptionDetail;
    private              boolean        deprecated;

    @JsonIgnore
    private transient Method javaMethod;

    @Override
    public int compareTo(final RestEndpoint value) {
        final String current = "" + this.getUri() + "" + this.getVerb();
        final String other   = value == null ? null : "" + value.getUri() + "" + value.getVerb();
        return StringComparator.compareTo(current, other);
    }
}
