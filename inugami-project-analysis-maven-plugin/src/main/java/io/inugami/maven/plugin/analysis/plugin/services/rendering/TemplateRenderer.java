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
package io.inugami.maven.plugin.analysis.plugin.services.rendering;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import io.inugami.api.tools.StringTools;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class TemplateRenderer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final MustacheFactory MUSTACHE_FACTORY  = new DefaultMustacheFactory();
    private static final Pattern         MAVEN_PROPS_REGEX = Pattern.compile("(?:[$][{])([^\\{]+)(?:\\})");

    // =========================================================================
    // API
    // =========================================================================
    public String render(final String templateId,
                         final String content,
                         final Map<String, String> properties,
                         final Map<String, String> additionalProperties,
                         final boolean applyMavenProperties) {

        if (content == null) {
            return null;
        }

        final String currentTemplateId = templateId == null ? "render-template" + UUID.randomUUID()
                                                                                      .toString() : templateId;


        final String realContent = applyMavenProperties ? replaceMavenProperties(content) : content;
        final Mustache mustache = MUSTACHE_FACTORY.compile(new StringReader(realContent),
                                                           currentTemplateId);
        final Map<String, Object> data   = buildData(properties, additionalProperties);
        final StringWriter        writer = new StringWriter();

        mustache.execute(writer, data);

        return writer.toString().replaceAll("&#10;", "\n");
    }

    protected String replaceMavenProperties(final String content) {

        return StringTools.replaceAll(MAVEN_PROPS_REGEX, content, "{{$1}}");
    }

    private Map<String, Object> buildData(final Map<String, String> properties,
                                          final Map<String, String> additionalProperties) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (properties != null) {
            result.putAll(properties);
        }
        if (additionalProperties != null) {
            result.putAll(additionalProperties);
        }

        return result;
    }

}
