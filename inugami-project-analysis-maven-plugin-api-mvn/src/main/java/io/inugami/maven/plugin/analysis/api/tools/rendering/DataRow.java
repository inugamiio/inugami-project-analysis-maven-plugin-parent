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
package io.inugami.maven.plugin.analysis.api.tools.rendering;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Setter
@Getter
public class DataRow {
    String                    rowColor;
    @EqualsAndHashCode.Include
    String                    uid;
    Map<String, Serializable> properties = new LinkedHashMap<>();

    public void setProperties(final Map<String, Serializable> properties) {
        if (properties != null) {
            this.properties.putAll(properties);
        }
    }

    public DataRow put(final String key, final Serializable value) {
        if (key != null && value != null) {
            properties.put(key, value);
        }
        return this;
    }

    public DataRow putAll(final Map<String, Serializable> properties) {
        if (properties != null) {
            this.properties.putAll(properties);
        }
        return this;
    }
}
