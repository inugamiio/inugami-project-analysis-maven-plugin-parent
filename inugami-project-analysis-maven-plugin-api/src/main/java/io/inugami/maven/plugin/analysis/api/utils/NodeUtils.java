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
package io.inugami.maven.plugin.analysis.api.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Consumer;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NodeUtils {

    // =========================================================================
    // API
    // =========================================================================

    public static <T> void processIfNotNull(final T value, final Consumer<T> consumer) {
        if (value != null && consumer != null) {
            consumer.accept(value);
        }
    }

    public static void processIfNotEmpty(final String value, final Consumer<String> consumer) {
        if (value != null && !value.isEmpty() && consumer != null) {
            consumer.accept(value);
        }
    }

    public static void processIfNotEmptyForce(final String value, final Consumer<String> consumer) {
        if (value != null && !value.isEmpty() && !"null".equals(value) && consumer != null) {
            consumer.accept(value);
        }
    }

    public static String cleanLines(final String value) {
        return value == null ? null : value.replaceAll("\n", "\\\\n").replaceAll("\"", "\\\\\"");
    }

    public static boolean hasText(final String value) {
        return value != null && !value.trim().isEmpty();
    }
}
