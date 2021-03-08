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

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface FieldTransformer {
    boolean accept(Field field, Class<?> type, Type genericType, String currentPath);

    void transform(Field field, Class<?> type, Type genericType, JsonNode.JsonNodeBuilder builder, String currentPath,
                   ClassCursor cursor);


    default boolean stop(final Field field, final Class<?> type, final Type genericType, final String currentPath) {
        return true;
    }

}
