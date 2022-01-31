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
package io.inugami.maven.plugin.analysis.front.plugins;

import io.inugami.maven.plugin.analysis.front.api.FrontPluginSpi;

import java.util.List;

public class CorePlugin implements FrontPluginSpi {
    @Override
    public String getPluginName() {
        return "io.inugami.maven.plugin.analysis.front:inugami-project-analysis-front-plugins";
    }

    @Override
    public String getNgModuleName() {
        return "InugamiProjectAnalysisPluginModule";
    }

    @Override
    public String getNgModuleFileName() {
        return "inugami-project-analysis-plugin.module";
    }

    @Override
    public String getModuleFolder() {
        return "inugami-project-analysis-plugin";
    }


    @Override
    public List<String> getCssFiles() {
        return List.of("css/agate.min.css",
                       "css/inugami-project-analysis-front-core-plugin.css");
    }

    @Override
    public List<String> getJavaScriptLink() {
        return List.of("js/highlight.min.js",
                       "js/inugami-project-analysis-front-core-plugin.js");
    }

    @Override
    public List<String> getMessageProperties() {
        return List.of("META-INF/inugami-project-analysis-front-plugins_en.properties",
                       "META-INF/inugami-project-analysis-front-plugins_fr.properties");
    }

    @Override
    public String getRootClass() {
        return "inugami-project-analysis";
    }
}
