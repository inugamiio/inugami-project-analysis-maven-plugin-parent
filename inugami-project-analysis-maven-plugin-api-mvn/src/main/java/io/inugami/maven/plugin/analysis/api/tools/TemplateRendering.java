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
package io.inugami.maven.plugin.analysis.api.tools;

import io.inugami.api.exceptions.UncheckedException;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.commons.files.FilesUtils;
import io.inugami.configuration.services.ConfigHandlerHashMap;
import io.inugami.maven.plugin.analysis.api.models.QueryDefinition;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TemplateRendering {

    // =========================================================================
    // API
    // =========================================================================
    public static String render(final QueryDefinition queryDefinition, final Map<String, String> properties) {
        return render(queryDefinition, properties, null);
    }

    public static String render(final QueryDefinition queryDefinition, final Map<String, String> properties,
                                final Map<String, String> additionalProperties) {
        if (queryDefinition == null || queryDefinition.getPath() == null) {
            return null;
        }
        else {
            return render(queryDefinition.getPath(), properties, additionalProperties);
        }
    }

    public static String render(final String templatePath, final Map<String, String> properties) {
        return render(templatePath, properties, null);
    }

    public static String render(final String templatePath, final Map<String, String> properties,
                                final Map<String, String> additionalProperties) {


        String content = null;
        if (templatePath != null) {
            content = FilesUtils.readFileFromClassLoader(templatePath);
        }

        return render(properties, additionalProperties, content);
    }

    public static String render(final File templatePath, final Map<String, String> properties) {
        String content = null;
        try {
            content = FilesUtils.readContent(templatePath);
        }
        catch (final IOException e) {
            throw new UncheckedException(e.getMessage(),e);
        }
        return render(properties, new LinkedHashMap<>(), content);
    }

    private static String render(final Map<String, String> properties, final Map<String, String> additionalProperties,
                                 final String content) {
        String result = null;
        if (content != null) {
            final ConfigHandler<String, String> configuration = new ConfigHandlerHashMap();

            if (properties != null) {
                configuration.putAll(properties);
            }
            if (additionalProperties != null) {
                configuration.putAll(additionalProperties);
            }
            result = configuration.applyProperties(content);
        }

        return result;
    }
}
