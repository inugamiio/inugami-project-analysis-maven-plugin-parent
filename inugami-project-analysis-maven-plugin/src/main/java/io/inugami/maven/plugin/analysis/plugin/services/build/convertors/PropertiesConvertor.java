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

import io.inugami.api.exceptions.FatalException;
import io.inugami.maven.plugin.analysis.api.convertors.PropertiesConvertorSpi;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;


public class PropertiesConvertor implements PropertiesConvertorSpi {

    public static final List<String> TYPES = Arrays.asList("properties", "text/x-java-properties");


    @Override
    public boolean accept(final String type) {
        return matchType(type, TYPES);
    }

    @Override
    public Map<String, String> convert(final String content) {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(content));
        }
        catch (IOException e) {
            throw new FatalException(e.getMessage(), e);
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
        return result;
    }
}
