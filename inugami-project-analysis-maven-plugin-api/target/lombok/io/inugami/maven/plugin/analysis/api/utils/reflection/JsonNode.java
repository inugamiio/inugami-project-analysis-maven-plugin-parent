// Generated by delombok at Mon Mar 08 22:38:41 CET 2021
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
import java.util.Iterator;
import java.util.List;

public class JsonNode implements JsonObject {
    private static final long serialVersionUID = -8448971914950919949L;
    private final String path;
    private final boolean list;
    private final boolean structure;
    private final boolean map;
    private final String fieldName;
    private final String type;
    private final String mapKey;
    private final JsonNode mapValue;
    private final List<JsonNode> children;
    private final boolean basicType;

    @Override
    public String convertToJson() {
        final JsonBuilder json = new JsonBuilder();
        final int level = countLevel(path);
        final String indentation = buildIndentation(level);
        json.write(indentation);
        if (list) {
            if (fieldName != null) {
                json.addField(fieldName);
            }
            json.openList();
            if (type != null) {
                json.write(type);
            }
        } else if (basicType) {
            if (level > 0) {
                final String currentFieldIndentation = buildIndentation(level);
                json.write(currentFieldIndentation);
                if (fieldName != null) {
                    json.addField(fieldName);
                }
                json.valueQuot(type);
            } else {
                json.write(type);
            }
        } else if (structure) {
            if (fieldName != null) {
                json.addField(fieldName);
            }
            json.openObject();
        } else if (map) {
            final String currentFieldIndentation = buildIndentation(level + 1);
            json.addField(fieldName);
            json.openObject().line();
            json.write(currentFieldIndentation);
            json.addField("<" + mapKey + ">");
            if (type == null) {
                json.line();
                json.write(mapValue == null ? null : mapValue.convertToJson());
            } else {
                json.write(type);
            }
            json.line();
            json.write(currentFieldIndentation);
            json.closeObject();
        } else {
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

    @java.lang.SuppressWarnings("all")
    JsonNode(final String path, final boolean list, final boolean structure, final boolean map, final String fieldName, final String type, final String mapKey, final JsonNode mapValue, final List<JsonNode> children, final boolean basicType) {
        this.path = path;
        this.list = list;
        this.structure = structure;
        this.map = map;
        this.fieldName = fieldName;
        this.type = type;
        this.mapKey = mapKey;
        this.mapValue = mapValue;
        this.children = children;
        this.basicType = basicType;
    }


    @java.lang.SuppressWarnings("all")
    public static class JsonNodeBuilder {
        @java.lang.SuppressWarnings("all")
        private String path;
        @java.lang.SuppressWarnings("all")
        private boolean list;
        @java.lang.SuppressWarnings("all")
        private boolean structure;
        @java.lang.SuppressWarnings("all")
        private boolean map;
        @java.lang.SuppressWarnings("all")
        private String fieldName;
        @java.lang.SuppressWarnings("all")
        private String type;
        @java.lang.SuppressWarnings("all")
        private String mapKey;
        @java.lang.SuppressWarnings("all")
        private JsonNode mapValue;
        @java.lang.SuppressWarnings("all")
        private List<JsonNode> children;
        @java.lang.SuppressWarnings("all")
        private boolean basicType;

        @java.lang.SuppressWarnings("all")
        JsonNodeBuilder() {
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder path(final String path) {
            this.path = path;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder list(final boolean list) {
            this.list = list;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder structure(final boolean structure) {
            this.structure = structure;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder map(final boolean map) {
            this.map = map;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder fieldName(final String fieldName) {
            this.fieldName = fieldName;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder type(final String type) {
            this.type = type;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder mapKey(final String mapKey) {
            this.mapKey = mapKey;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder mapValue(final JsonNode mapValue) {
            this.mapValue = mapValue;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder children(final List<JsonNode> children) {
            this.children = children;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode.JsonNodeBuilder basicType(final boolean basicType) {
            this.basicType = basicType;
            return this;
        }

        @java.lang.SuppressWarnings("all")
        public JsonNode build() {
            return new JsonNode(this.path, this.list, this.structure, this.map, this.fieldName, this.type, this.mapKey, this.mapValue, this.children, this.basicType);
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("all")
        public java.lang.String toString() {
            return "JsonNode.JsonNodeBuilder(path=" + this.path + ", list=" + this.list + ", structure=" + this.structure + ", map=" + this.map + ", fieldName=" + this.fieldName + ", type=" + this.type + ", mapKey=" + this.mapKey + ", mapValue=" + this.mapValue + ", children=" + this.children + ", basicType=" + this.basicType + ")";
        }
    }

    @java.lang.SuppressWarnings("all")
    public static JsonNode.JsonNodeBuilder builder() {
        return new JsonNode.JsonNodeBuilder();
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public boolean equals(final java.lang.Object o) {
        if (o == this) return true;
        if (!(o instanceof JsonNode)) return false;
        final JsonNode other = (JsonNode) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$path = this.getPath();
        final java.lang.Object other$path = other.getPath();
        if (this$path == null ? other$path != null : !this$path.equals(other$path)) return false;
        return true;
    }

    @java.lang.SuppressWarnings("all")
    protected boolean canEqual(final java.lang.Object other) {
        return other instanceof JsonNode;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $path = this.getPath();
        result = result * PRIME + ($path == null ? 43 : $path.hashCode());
        return result;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    public java.lang.String toString() {
        return "JsonNode(path=" + this.getPath() + ", list=" + this.isList() + ", structure=" + this.isStructure() + ", map=" + this.isMap() + ", fieldName=" + this.getFieldName() + ", type=" + this.getType() + ", mapKey=" + this.getMapKey() + ", mapValue=" + this.getMapValue() + ", children=" + this.getChildren() + ", basicType=" + this.isBasicType() + ")";
    }

    @java.lang.SuppressWarnings("all")
    public String getPath() {
        return this.path;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isList() {
        return this.list;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isStructure() {
        return this.structure;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isMap() {
        return this.map;
    }

    @java.lang.SuppressWarnings("all")
    public String getFieldName() {
        return this.fieldName;
    }

    @java.lang.SuppressWarnings("all")
    public String getType() {
        return this.type;
    }

    @java.lang.SuppressWarnings("all")
    public String getMapKey() {
        return this.mapKey;
    }

    @java.lang.SuppressWarnings("all")
    public JsonNode getMapValue() {
        return this.mapValue;
    }

    @java.lang.SuppressWarnings("all")
    public List<JsonNode> getChildren() {
        return this.children;
    }

    @java.lang.SuppressWarnings("all")
    public boolean isBasicType() {
        return this.basicType;
    }
}