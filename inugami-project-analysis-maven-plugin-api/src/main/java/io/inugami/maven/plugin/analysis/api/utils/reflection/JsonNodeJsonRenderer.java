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
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Iterator;

import static io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNodeRendererUtils.buildIndentation;
import static io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNodeRendererUtils.countLevel;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonNodeJsonRenderer {

    // =========================================================================
    // API
    // =========================================================================
    public static String toJson(final JsonNode jsonNode) {
        if (jsonNode == null) {
            return "null";
        }

        final JsonBuilder json        = new JsonBuilder();
        final int         level       = countLevel(jsonNode.getPath());
        final String      indentation = buildIndentation(level);
        json.write(indentation);

        if (jsonNode.isList()) {
            if (jsonNode.getFieldName() != null) {
                json.addField(jsonNode.getFieldName());
            }

            json.openList();
            if (jsonNode.getType() != null) {
                json.write(jsonNode.getType());
            }
        }

        else if (jsonNode.isBasicType()) {
            if (level > 0) {
                final String currentFieldIndentation = buildIndentation(level);
                json.write(currentFieldIndentation);
                if (jsonNode.getFieldName() != null) {
                    json.addField(jsonNode.getFieldName());
                }
                json.valueQuot(jsonNode.getType());
            }
            else {
                json.write(jsonNode.getType());
            }
        }

        else if (jsonNode.isStructure()) {
            if (jsonNode.getFieldName() != null) {
                json.addField(jsonNode.getFieldName());
            }
            json.openObject();

        }

        else if (jsonNode.isMap()) {
            final String currentFieldIndentation = buildIndentation(level + 1);
            json.addField(jsonNode.getFieldName());
            json.openObject().line();
            json.write(currentFieldIndentation);
            json.addField("<" + jsonNode.getMapKey() + ">");

            if (jsonNode.getType() == null) {
                json.line();
                json.write(jsonNode.getMapValue() == null ? null : jsonNode.getMapValue().convertToJson());
            }
            else {
                json.write(jsonNode.getType());
            }

            json.line();
            json.write(currentFieldIndentation);
            json.closeObject();
        }

        else {
            json.addField(jsonNode.getFieldName());
            json.valueQuot(jsonNode.getType());
        }

        if (jsonNode.getChildren() != null && !jsonNode.getChildren().isEmpty()) {
            final Iterator<JsonNode> iterator = jsonNode.getChildren().iterator();
            while (iterator.hasNext()) {
                final JsonNode node = iterator.next();
                json.line();
                json.write(node == null ? null : node.convertToJson());
                if (iterator.hasNext()) {
                    json.addSeparator();
                }
            }
        }

        if (jsonNode.isList()) {
            if (jsonNode.getChildren() != null && !jsonNode.getChildren().isEmpty()) {
                json.line();
                json.write(indentation);
            }
            json.closeList();
        }
        if (jsonNode.isStructure()) {
            json.line();
            json.write(indentation);
            json.closeObject();
        }

        return json.toString();
    }

}
