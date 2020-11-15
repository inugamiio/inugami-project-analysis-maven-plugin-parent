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
package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;

import java.util.List;

public interface ClassAnalyzer {
    default boolean accept(final Class<?> clazz, final ScanConext context) {
        return true;
    }

    List<JsonObject> analyze(Class<?> clazz, ScanConext context);

    default void initialize(final ScanConext context) {
    }

    default boolean isEnable(final String feature, final ScanConext context, final boolean defaultValue) {
        final String activation = context == null ? String.valueOf(defaultValue) : context.getConfiguration()
                                                                                          .getOrDefault(feature,
                                                                                                        String.valueOf(
                                                                                                                defaultValue));
        return Boolean.parseBoolean(activation);
    }
}
