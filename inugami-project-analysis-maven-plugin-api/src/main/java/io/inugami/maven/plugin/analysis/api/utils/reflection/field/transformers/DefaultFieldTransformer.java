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
package io.inugami.maven.plugin.analysis.api.utils.reflection.field.transformers;

import io.inugami.maven.plugin.analysis.api.utils.reflection.ClassCursor;
import io.inugami.maven.plugin.analysis.api.utils.reflection.FieldTransformer;
import io.inugami.maven.plugin.analysis.api.utils.reflection.JsonNode;
import io.inugami.maven.plugin.analysis.api.utils.reflection.ReflectionService;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
@SuppressWarnings({"java:S120"})
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
    public void transform(final Field field, final Class<?> type,
                          final Type genericType,
                          final JsonNode.JsonNodeBuilder builder,
                          final String currentPath,
                          final ClassCursor cursor) {
        if (cursor.isPresentInParents(type)) {
            builder.type(ReflectionService.renderFieldTypeRecursiveNoQuot(type));
        }
        else {
            builder.type(ReflectionService.renderFieldType(type));
        }

    }
}
