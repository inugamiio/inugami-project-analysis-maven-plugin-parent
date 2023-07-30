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

import java.util.Collections;

public class UnknownJsonType extends JsonNode {
    public static final String OBJECT = "<object>";
    private static final long serialVersionUID = -4473300215794854497L;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================


    public UnknownJsonType() {
        super(null,null, false, false, false, null, null, null, null, Collections.emptyList(), false);
    }

    @Override
    public String convertToJson() {
        return new JsonBuilder().valueQuot(OBJECT).toString();
    }
}
