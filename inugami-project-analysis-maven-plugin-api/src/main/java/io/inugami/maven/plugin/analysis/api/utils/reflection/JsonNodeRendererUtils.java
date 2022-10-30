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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonNodeRendererUtils {


    // =========================================================================
    // API
    // =========================================================================
    public static int countLevel(final String path) {
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

    public static String buildIndentation(final int length) {
        final StringBuilder result = new StringBuilder();
        for (int i = length; i > 0; i--) {
            result.append("  ");
        }
        return result.toString();
    }
}
