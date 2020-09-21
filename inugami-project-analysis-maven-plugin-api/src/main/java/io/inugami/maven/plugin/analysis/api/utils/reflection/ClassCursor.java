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

import java.util.ArrayList;
import java.util.List;


public class ClassCursor {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final List<String> classes;

    // =========================================================================
    // API
    // =========================================================================
    public ClassCursor() {
        this.classes = new ArrayList<>();
    }

    public ClassCursor(final List<String> classes) {
        this.classes = classes == null ? new ArrayList<>() : classes;
    }

    // =========================================================================
    // API
    // =========================================================================
    public ClassCursor createNewContext(final Class<?> clazz) {
        classes.add(clazz.getName());
        return new ClassCursor(new ArrayList<>(classes));
    }

    public boolean isPresentInParents(final Class<?> clazz) {
        boolean result = false;
        if (clazz != null) {
            result = classes.contains(clazz.getName());
        }
        return result;
    }
}
