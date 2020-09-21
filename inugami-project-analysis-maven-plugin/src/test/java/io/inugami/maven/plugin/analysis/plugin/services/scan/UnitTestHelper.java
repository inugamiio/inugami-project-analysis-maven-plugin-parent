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
package io.inugami.maven.plugin.analysis.plugin.services.scan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import flexjson.JSONSerializer;
import io.inugami.api.exceptions.Asserts;
import io.inugami.api.loggers.Loggers;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UnitTestHelper {

    // =========================================================================
    // API
    // =========================================================================
    public static String loadJsonReference(final String relativePath) {
        if (relativePath == null) {
            throw new RuntimeException("can't read file from null relative path!");
        }

        final File path = buildTestFilePath(relativePath.split("/"));
        return path.exists() ? readFile(path) : null;
    }

    public static String readFile(final File file) {
        String result = null;
        try {
            result = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        }
        catch (final IOException e) {
            throwException(e);
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public static File buildTestFilePath(final String... filePathParts) {
        final List<String> parts = new ArrayList<>(Arrays.asList("src", "test", "resources"));
        Arrays.asList(filePathParts)
              .forEach(parts::add);
        return buildPath(parts.toArray(new String[]{}));
    }

    public static File buildPath(final String... parts) {
        final File basePath = new File(".");

        final String[] allPathParts = new String[parts.length + 1];
        allPathParts[0] = basePath.getAbsolutePath();
        System.arraycopy(parts, 0, allPathParts, 1, parts.length);

        return new File(String.join(File.separator, allPathParts));
    }

    // =========================================================================
    // OBJECT CONVERT
    // =========================================================================
    public static String convertToJson(final Object value) {
        String result = null;
        try {
            result = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                                       .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                                       .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                       .writeValueAsString(value);
        }
        catch (final Exception e) {
            throwException(e);
        }
        return result;
    }

    public static String forceConvertToJson(final Object value) {
        return new JSONSerializer().prettyPrint(true)
                                   .exclude("*.class")
                                   .deepSerialize(value);
    }

    public static String convertToJsonWithoutIndent(final Object value) {
        String result = null;
        try {
            result = new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                                       .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                       .writeValueAsString(value);
        }
        catch (final Exception e) {
            throwException(e);
        }
        return result;
    }


    public static <T> T loadJson(final String path, final TypeReference<T> refObjectType) {
        Asserts.notNull("can't load Json Object from null path", path);
        final String json = loadJsonReference(path);
        return convertFromJson(json, refObjectType);
    }

    public static <T> T convertFromJson(final String json, final TypeReference<T> refObjectType) {
        try {
            return json == null ? null : new ObjectMapper().readValue(json, refObjectType);
        }
        catch (final IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // =========================================================================
    // EXCEPTIONS
    // =========================================================================
    private static void throwException(final Exception error) {
        throw new RuntimeException(error.getMessage(), error);
    }

    public static void assertText(final Object value, final String jsonRef) {
        if (jsonRef == null) {
            Asserts.isNull("json must be null", value);
        }
        else {
            Asserts.notNull("json mustn't be null", value);
            final String json = convertToJson(value);
            assertText(jsonRef, json);
        }


    }

    public static void assertText(final String jsonRef, final String json) {
        Asserts.notNull("json ref mustn't be null", jsonRef);
        Asserts.notNull("json mustn't be null", json);
        final String[] jsonValue = json.split("\n");
        final String[] refLines  = jsonRef.split("\n");

        try {
            Asserts.isTrue(
                    String.format("reference and json have not same size : %s,%s", jsonValue.length, refLines.length),
                    jsonValue.length == refLines.length);
        }
        catch (final Throwable e) {
            Loggers.DEBUG.error("\nactual :\n{}\nreference:\n{}\n----------", json, jsonRef);
            throw e;
        }


        for (int i = 0; i < refLines.length; i++) {
            if (!jsonValue[i].trim().equals(refLines[i].trim())) {
                Loggers.DEBUG.error("\nactual :\n{}\nreference:\n{}----------", json, jsonRef);
                throw new RuntimeException(
                        String.format("[%s] %s != %s", i + 1, jsonValue[i].trim(), refLines[i].trim()));
            }

        }
    }
}
