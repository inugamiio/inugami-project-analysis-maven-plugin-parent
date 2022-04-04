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

import io.inugami.maven.plugin.analysis.front.core.renderers.PluginsModuleRenderer;
import lombok.RequiredArgsConstructor;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
public class PluginsModuleServlet extends HttpServlet {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final int    SUCCCESS               = 200;
    private static final String UTF_8                  = "UTF-8";
    private static final long   serialVersionUID       = -4097614303888347284L;
    public static final  String APPLICATION_TYPESCRIPT = "application/x-typescript";
    
    // =========================================================================
    // API
    // =========================================================================

    @Override
    protected void doGet(final HttpServletRequest req,
                         final HttpServletResponse resp) throws ServletException, IOException {

        resp.getWriter().print(new PluginsModuleRenderer().render());
        resp.setStatus(SUCCCESS);
        resp.setContentType(APPLICATION_TYPESCRIPT);
        resp.setCharacterEncoding(UTF_8);
    }
}