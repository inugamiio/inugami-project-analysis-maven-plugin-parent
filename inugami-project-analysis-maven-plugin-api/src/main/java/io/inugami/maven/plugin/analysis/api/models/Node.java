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

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Builder(toBuilder = true)
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Node implements JsonObject, Comparable<Node> {

    private static final long                                serialVersionUID = 7519867544798392684L;
    private              String                              type;
    private              String                              name;
    @EqualsAndHashCode.Include
    private              String                              uid;
    private              LinkedHashMap<String, Serializable> properties;

    @Override
    public int compareTo(final Node other) {
        return buildHash().compareTo(other.buildHash());
    }

    private String buildHash() {
        return new StringBuilder().append(uid)
                                  .append("<")
                                  .append(type)
                                  .append(">")
                                  .append("{")
                                  .append(properties)
                                  .append("}")
                                  .toString();
    }


    public static class NodeBuilder {
        private LinkedHashMap<String, Serializable> properties;

        public NodeBuilder properties(final LinkedHashMap<String, Serializable> properties) {
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
