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
package io.inugami.maven.plugin.analysis.functional;

import java.util.Collection;
import java.util.Map;

public final class CheckUtils {

    private CheckUtils() {
    }

    // =========================================================================
    // EMPTY
    // =========================================================================
    public static boolean notEmpty(final String value) {
        return value != null && !"".equals(value);
    }

    public static boolean isEmpty(final String value) {
        return !notEmpty(value);
    }

    public static boolean notEmpty(final Collection<?> value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isEmpty(final Collection<?> value) {
        return !notEmpty(value);
    }

    public static boolean notEmpty(final Map<?, ?> value) {
        return value != null && !value.isEmpty();
    }

    public static boolean isEmpty(final Map<?, ?> value) {
        return !notEmpty(value);
    }

    public static <T> boolean isEmpty(final T[] values) {
        return values == null || values.length == 0;
    }

    public static <T> boolean notEmpty(final T[] values) {
        return !isEmpty(values);
    }
}
