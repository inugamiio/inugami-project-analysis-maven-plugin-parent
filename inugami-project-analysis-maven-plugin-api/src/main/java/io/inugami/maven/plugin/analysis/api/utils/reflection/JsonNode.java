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
package io.inugami.maven.plugin.analysis.api.utils.reflection;

import io.inugami.api.models.data.basic.JsonObject;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Getter
@Builder(toBuilder = true)
public class JsonNode implements JsonObject {
    private static final long    serialVersionUID = -8448971914950919949L;
    public static final  String  ROOT             = ".";
    @EqualsAndHashCode.Include
    private final        String  path;
    private final        boolean list;
    private final        boolean structure;

    private final boolean map;
    private final String  fieldName;
    private final String  type;


    private final String   mapKey;
    private final JsonNode mapValue;

    private final List<JsonNode> children;

    private final boolean basicType;

    private final DescriptionDTO description;

    @Override
    public String convertToJson() {
        return JsonNodeJsonRenderer.toJson(this);
    }


    public String toXML(final int nbTab, final boolean strict) {
        return JsonNodeXmlRenderer.toXml(this, nbTab, strict);
    }

    public boolean isRoot() {
        return path == null || !path.contains(ROOT);
    }
}
