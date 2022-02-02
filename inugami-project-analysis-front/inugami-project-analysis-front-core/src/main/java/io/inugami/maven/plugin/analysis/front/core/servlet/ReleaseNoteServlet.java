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
package io.inugami.maven.plugin.analysis.front.core.servlet;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.inugami.api.models.JsonBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ReleaseNoteServlet extends HttpServlet {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================

    private static final int          SUCCCESS               = 200;
    private static final String       UTF_8                  = "UTF-8";
    private static final long         serialVersionUID       = -4097614303888347284L;
    private static final ObjectMapper OBJECT_MAPPER          = buildObjectMapper();
    public static final  String       APPLICATION_TYPESCRIPT = "application/x-typescript";


    private final String artifactName;

    // =========================================================================
    // API
    // =========================================================================

    @Override
    protected void doGet(final HttpServletRequest req,
                         final HttpServletResponse resp) throws ServletException, IOException {

        resp.getWriter().print(renderJson());
        resp.setStatus(SUCCCESS);
        resp.setContentType(MediaType.APPLICATION_JSON);
        resp.setCharacterEncoding(UTF_8);
    }

    // =========================================================================
    // RENDER
    // =========================================================================

    private String renderJson() {
        final List<String> files   = resolveFiles();
        final List<String> content = filesContent(files);

        final JsonBuilder json = new JsonBuilder();
        json.openList();
        if (content != null) {
            final Iterator<String> iterator = content.iterator();
            while (iterator.hasNext()) {
                json.write(iterator.next());
                if (iterator.hasNext()) {
                    json.addSeparator().line();
                }
            }
        }
        json.closeList();
        return json.toString();
    }


    private List<String> resolveFiles() {
        List<String> result = new ArrayList<>();
        InputStream  input  = null;
        if (artifactName != null) {
            input = getClass().getClassLoader()
                              .getResourceAsStream(String.format("META-INF/releases/%s.json", artifactName));
        }

        if (input != null) {
            try {
                result = OBJECT_MAPPER.readValue(input, new TypeReference<List<String>>() {
                });
            }
            catch (final IOException e) {
                log.error(e.getMessage(), e);
            }
            finally {
                close(input);
            }
        }
        return result;
    }


    private List<String> filesContent(final List<String> files) {
        final List<String> result = new ArrayList<>();
        if (files != null) {
            for (final String path : files) {
                final String content = readContent(path);
                if (content != null) {
                    result.add(content);
                }
            }
        }
        return result;
    }

    private String readContent(final String path) {
        String result = null;
        final InputStream input = getClass().getClassLoader()
                                            .getResourceAsStream(
                                                    String.format("META-INF/releases/%s", path));

        try {
            final JsonNode json = OBJECT_MAPPER.readTree(input);
            result = OBJECT_MAPPER.writeValueAsString(json);
        }
        catch (final IOException e) {
            log.error(e.getMessage(), e);
            close(input);
        }
        return result;
    }

    // =========================================================================
    // TOOLS
    // =========================================================================
    public static ObjectMapper buildObjectMapper() {
        return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                                 .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                                 .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                 .registerModule(new JavaTimeModule())
                                 .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private void close(final InputStream input) {
        try {
            if (input != null) {
                input.close();
            }
        }
        catch (final IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
