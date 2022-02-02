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

import io.inugami.api.models.JsonBuilder;
import io.inugami.maven.plugin.analysis.front.api.FrontPluginSpi;
import io.inugami.maven.plugin.analysis.front.api.IndexHtmlLoadingContentSpi;
import io.inugami.maven.plugin.analysis.front.api.models.HtmlAttribute;

import java.util.List;

import static io.inugami.maven.plugin.analysis.front.api.RenderingConstants.*;
import static io.inugami.maven.plugin.analysis.front.api.utils.HtmlRenderingUtils.*;

public class CoreIndexHtmlLoadingContent implements IndexHtmlLoadingContentSpi {


    @Override
    public String getLoadingContent(final String contextPath, final List<FrontPluginSpi> plugins) {
        final JsonBuilder result = new JsonBuilder();
        result.write(openTag(DIV, HtmlAttribute.build("class", "loading")));
        result.write(openTag(DIV, HtmlAttribute.build("class", "info")));

        result.write(tag(H1, () -> buildPluginTitle(plugins)));
        result.write(tag(H2, () -> "loading ..."));
        result.write(openTag(DIV, HtmlAttribute.build("class", "icon-loading")));
        result.write(loadSvg("META-INF/resources/release-note-app/images/inugami-logo.svg", 256, 256));
        result.line();
        result.write(closeTag(DIV));

        result.write(closeTag(DIV));
        result.write(closeTag(DIV));
        return result.toString();
    }
}
