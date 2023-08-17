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
import io.inugami.api.tools.StringComparator;
import lombok.*;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.utils.NodeUtils.sortProperties;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public final class Relationship implements JsonObject, Comparable<Relationship> {

    private static final long serialVersionUID = 973646684487506001L;

    @EqualsAndHashCode.Include
    private String                    from;
    @EqualsAndHashCode.Include
    private String                    to;
    @EqualsAndHashCode.Include
    private String                    type;
    private Map<String, Serializable> properties;

    public void sort() {
        properties = sortProperties(properties);
    }

    @Override
    public int compareTo(final Relationship other) {
        return StringComparator.compareTo(buildHash(), other == null ? null : other.buildHash());
    }

    public String buildHash() {
        return new StringBuilder().append(from)
                                  .append("-[").append(type).append("]->")
                                  .append(to)
                                  .toString();
    }

    public static class RelationshipBuilder {
        public Relationship.RelationshipBuilder property(final String key, final Serializable value) {
            if (this.properties == null) {
                this.properties = new LinkedHashMap<>();
            }
            if (key != null && value != null) {
                this.properties.put(key, value);
            }
            this.properties = sortProperties(this.properties);
            return this;
        }

        public Relationship.RelationshipBuilder properties(final Map<String, Serializable> properties) {
            if (this.properties == null) {
                this.properties = new LinkedHashMap<>();
            }
            if (properties != null) {
                this.properties.putAll(properties);
            }
            this.properties = sortProperties(this.properties);
            return this;
        }
    }
}
