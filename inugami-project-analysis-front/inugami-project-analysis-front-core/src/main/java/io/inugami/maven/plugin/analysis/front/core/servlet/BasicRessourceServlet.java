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

import io.inugami.commons.files.FilesUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@SuppressWarnings({"java:S1989"})
@Builder
@AllArgsConstructor
public class BasicRessourceServlet extends HttpServlet {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final int SUCCCESS = 200;

    private final String resourcePath;
    private final String mediaType;
    // =========================================================================
    // API
    // =========================================================================

    @Override
    protected void doGet(final HttpServletRequest req,
                         final HttpServletResponse resp) throws ServletException, IOException {
        resp.setStatus(SUCCCESS);
        resp.setContentType(mediaType);
        ServletCommons.setUtf8(resp);
        try {
            final String content = FilesUtils.readFileFromClassLoader(resourcePath);
            resp.getWriter().print(content);
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
