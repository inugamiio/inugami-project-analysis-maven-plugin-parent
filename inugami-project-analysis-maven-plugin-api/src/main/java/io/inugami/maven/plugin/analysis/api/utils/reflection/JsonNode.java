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

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.models.data.basic.JsonObject;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Iterator;
import java.util.List;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString
@Getter
@Builder
public class JsonNode implements JsonObject {
    private static final long serialVersionUID = -8448971914950919949L;
    @EqualsAndHashCode.Include
    private final String  path;
    private final boolean list;
    private final boolean structure;

    private final boolean map;
    private final String  fieldName;
    private final String  type;


    private final String   mapKey;
    private final JsonNode mapValue;

    private final List<JsonNode> children;

    private final boolean basicType;

    @Override
    public String convertToJson() {
        final JsonBuilder json        = new JsonBuilder();
        final int         level       = countLevel(path);
        final String      indentation = buildIndentation(level);
        json.write(indentation);
        if (list) {
            if (fieldName != null) {
                json.addField(fieldName);
            }

            json.openList();
            if (type != null) {
                json.write(type);
            }
        }
        else if(basicType){
            if(level>0){
                final String currentFieldIndentation = buildIndentation(level);
                json.write(currentFieldIndentation);
                if(fieldName!=null){
                    json.addField(fieldName);
                }
                json.valueQuot(type);
            }else{
                json.write(type);
            }
        }
        else if (structure) {
            if (fieldName != null) {
                json.addField(fieldName);
            }
            json.openObject();

        }
        else if (map) {
            final String currentFieldIndentation = buildIndentation(level + 1);
            json.addField(fieldName);
            json.openObject().line();
            json.write(currentFieldIndentation);
            json.addField("<" + mapKey + ">");

            if (type == null) {
                json.line();
                json.write(mapValue == null ? null : mapValue.convertToJson());
            }
            else {
                json.write(type);
            }

            json.line();
            json.write(currentFieldIndentation);
            json.closeObject();
        }
        else {
            json.addField(fieldName);
            json.valueQuot(type);
        }

        if (children != null && !children.isEmpty()) {
            final Iterator<JsonNode> iterator = children.iterator();
            while (iterator.hasNext()) {
                final JsonNode node = iterator.next();
                json.line();
                json.write(node == null ? null : node.convertToJson());
                if (iterator.hasNext()) {
                    json.addSeparator();
                }
            }
        }

        if (list) {
            if (children != null && !children.isEmpty()) {
                json.line();
                json.write(indentation);
            }
            json.closeList();
        }
        if (structure) {
            json.line();
            json.write(indentation);
            json.closeObject();
        }

        return json.toString();
    }

    private int countLevel(final String path) {
        int result = 0;
        if (path != null) {
            for (final char charElement : path.toCharArray()) {
                if ('.' == charElement) {
                    result++;
                }
            }
        }
        return result;
    }

    private String buildIndentation(final int length) {
        final StringBuilder result = new StringBuilder();
        for (int i = length; i > 0; i--) {
            result.append("  ");
        }
        return result.toString();
    }

}
