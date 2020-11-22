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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Relationship implements JsonObject, Comparable<Relationship> {

    private static final long serialVersionUID = 973646684487506001L;

    @EqualsAndHashCode.Include
    private String                              from;
    @EqualsAndHashCode.Include
    private String                              to;
    @EqualsAndHashCode.Include
    private String                              type;
    private LinkedHashMap<String, Serializable> properties;

    @Override
    public int compareTo(final Relationship other) {
        return buildHash().compareTo(other.buildHash());
    }

    protected String buildHash() {
        return new StringBuilder().append(from)
                                  .append("-[").append(type).append("]->")
                                  .append(to)
                                  .toString();
    }

    public static class RelationshipBuilder {
        private LinkedHashMap<String, Serializable> properties;

        public Relationship.RelationshipBuilder properties(final LinkedHashMap<String, Serializable> properties) {
            if (properties != null) {
                this.properties = new LinkedHashMap<>();
                final List<String> keys = new ArrayList<>(properties.keySet());
                Collections.sort(keys);
                for (final String key : keys) {
                    this.properties.put(key, properties.get(key));
                }
            }
            return this;
        }
    }
}
