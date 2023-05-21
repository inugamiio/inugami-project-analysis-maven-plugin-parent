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

import com.fasterxml.jackson.core.JsonProcessingException;
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.front.api.models.DependenciesCheck;
import io.inugami.maven.plugin.analysis.front.api.services.DependenciesCheckService;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static io.inugami.maven.plugin.analysis.front.core.tools.JsonMarshallerUtils.OBJECT_MAPPER;

@SuppressWarnings({"java:S1989"})
@Slf4j
public class DependenciesCheckServlet extends HttpServlet {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final int                      SUCCCESS               = 200;
    private static final int                      ERROR                  = 500;
    private static final long                     serialVersionUID       = -4097614303888347284L;
    public static final  String                   APPLICATION_TYPESCRIPT = "application/x-typescript";
    public static final  String                   DEFUALT_RESPONSE       = "{}";
    private final        DependenciesCheckService dependenciesCheckService;


    // =========================================================================
    // CONSTRUCTOR
    // =========================================================================

    public DependenciesCheckServlet(
            final DependenciesCheckService dependenciesCheckService) {

        if (dependenciesCheckService == null) {
            this.dependenciesCheckService = SpiLoader.getInstance().loadSpiSingleServicesByPriority(
                    DependenciesCheckService.class);
        } else {
            this.dependenciesCheckService = dependenciesCheckService;
        }
    }


    // =========================================================================
    // API
    // =========================================================================

    @Override
    protected void doGet(final HttpServletRequest req,
                         final HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType(MediaType.APPLICATION_JSON);
        ServletCommons.setUtf8(resp);
        int    status = SUCCCESS;
        String json   = null;

        try {
            json = retrieveData();
        } catch (final Exception error) {
            status = ERROR;
            json = DEFUALT_RESPONSE;
        }

        try {
            resp.getWriter().print(json);
            resp.setStatus(status);
        } catch (final Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private String retrieveData() throws JsonProcessingException {
        String            result = DEFUALT_RESPONSE;
        DependenciesCheck data   = null;
        if (dependenciesCheckService != null) {
            data = dependenciesCheckService.getDependenciesCheckData();

            try {
                result = OBJECT_MAPPER.writeValueAsString(data);
            } catch (final JsonProcessingException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }
        return result;
    }
}
