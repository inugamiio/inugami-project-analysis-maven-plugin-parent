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
package io.inugami.maven.plugin.analysis.front.api;

import java.util.List;

public interface FrontPluginSpi {
    String getPluginName();

    default String getRootClass() {
        return null;
    }

    default String getTitle(){
        return null;
    }

    default String getLoadingImage(){
        return null;
    }

    default String getFavIcon(){
        return null;
    }

    default List<String> getJavaScriptLink(){
        return null;
    }

    default List<String> getCssFiles(){
        return null;
    }
}
