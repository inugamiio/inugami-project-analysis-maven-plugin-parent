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
package io.inugami.maven.plugin.analysis.api.utils.reflection.fieldTransformers;


import io.inugami.maven.plugin.analysis.api.utils.reflection.ClassCursor;
import io.inugami.maven.plugin.analysis.api.utils.reflection.FieldTransformer;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;
import io.inugami.maven.plugin.analysis.functional.CheckUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DefaultFieldTransformer implements FieldTransformer {

    // =========================================================================
    // ACCEPT
    // =========================================================================
    @Override
    public boolean accept(final Field field, final Class<?> type, final Type genericType, final String currentPath) {
        return true;
    }

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void transform(final Field field, Class<?> type,
                          final Type genericType,
                          final JsonNode.JsonNodeBuilder builder,
                          final String currentPath,
                          final ClassCursor cursor) {

        if (cursor.isPresentInParents(type)) {
            builder.type(ReflectionService.renderFieldTypeRecursiveNoQuot(type));
        }
        else {
            final List<JsonNode> children      = new ArrayList<>();
            final List<Field>    childrenField = ReflectionService.getAllFields(type);
            builder.type(ReflectionService.renderFieldType(type));
            builder.fieldName(field.getName());
            builder.list(ReflectionService.isList(field.getType()));

            for (Field childField : childrenField) {
                if (Modifier.isStatic(childField.getModifiers())) {
                    continue;
                }
                final Class<?>    childType   = ReflectionService.extractGenericType(childField.getGenericType());
                final ClassCursor childCursor = cursor.createNewContext(childField.getType());

                final JsonNode childNode = ReflectionService.renderType(childType,
                                                                        ReflectionService.getGenericType(childType),
                                                                        childCursor);

                final JsonNode.JsonNodeBuilder child = childNode.toBuilder()
                                                                .path(currentPath + "." + childField.getName())
                                                                .fieldName(childField.getName());

                if (ReflectionService.isList(childField.getType()) && CheckUtils.notEmpty(childNode.getChildren())) {
                    JsonNode wrapper = JsonNode.builder()
                                               .list(true)
                                               .fieldName(childField.getName())
                                               .children(List.of(child.build()))
                                               .build();
                    children.add(wrapper);
                }
                else {

                    child.structure(!ReflectionService.isBasicType(childField.getType()));
                    children.add(child.build());
                }
            }

            builder.structure(true);
            builder.children(children);

        }
    }
}
