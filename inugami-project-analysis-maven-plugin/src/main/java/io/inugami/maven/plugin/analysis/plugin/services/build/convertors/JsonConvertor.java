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
package io.inugami.maven.plugin.analysis.plugin.services.build.convertors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.inugami.api.exceptions.FatalException;
import io.inugami.commons.marshaling.JsonMarshaller;
import io.inugami.maven.plugin.analysis.api.convertors.PropertiesConvertorSpi;

import java.util.*;

public class JsonConvertor implements PropertiesConvertorSpi {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    public static final List<String> TYPES = Arrays.asList("json",
                                                           "application/json",
                                                           "application/x-javascript",
                                                           "text/javascript",
                                                           "text/x-javascript",
                                                           "text/x-json",
                                                           "text/json");


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public boolean accept(final String type) {
        return matchType(type, TYPES);
    }


    @Override
    public Map<String, String> convert(final String content) {
        final JsonNode      tree   = read(content);
        Map<String, String> result = new LinkedHashMap<>();

        addProperties("", tree, result);

        return result;
    }

    private void addProperties(final String path, final JsonNode tree, final Map<String, String> result) {
        if (tree.isObject()) {
            final Iterator<Map.Entry<String, JsonNode>> fields = tree.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field       = fields.next();
                final String                      currentPath = path + resolveRootLevel(path) + field.getKey();
                addProperties(currentPath, field.getValue(), result);
            }
        }
        else if (tree.isArray()) {
            int cursor = 0;
            for (JsonNode child : tree) {
                final String currentPath = path + resolveRootLevel(path) + cursor;
                addProperties(currentPath, child, result);
                cursor++;
            }
        }
        else {
            result.put(path, tree.asText());
        }
    }

    private String resolveRootLevel(final String path) {
        return path.isEmpty() ? "" : ".";
    }


    // =========================================================================
    // TOOLS
    // =========================================================================
    private JsonNode read(final String content) {
        try {
            return JsonMarshaller.getInstance().getDefaultObjectMapper()
                                 .readTree(content);
        }
        catch (JsonProcessingException e) {
            throw new FatalException(e.getMessage(), e);
        }
    }
}
