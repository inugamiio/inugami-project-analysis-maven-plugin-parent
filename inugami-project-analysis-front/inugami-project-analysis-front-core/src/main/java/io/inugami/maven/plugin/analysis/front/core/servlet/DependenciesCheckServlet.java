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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

public class DependenciesCheckServlet extends HttpServlet {

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

        resp.getWriter().print("{\n" +
                                       "  \"deprecated\": [\n" +
                                       "    {\n" +
                                       "      \"groupId\": \"io.inugami.maven.plugin.analysis\",\n" +
                                       "      \"comment\": \"Please update to inugami maven plugin version 1.5.2 or higher\",\n" +
                                       "      \"link\":\"https://search.maven.org/artifact/io.inugami.maven.plugin.analysis/inugami-project-analysis-maven-plugin-parent/1.5.2/pom\",\n" +
                                       "      \"rules\": {\n" +
                                       "        \"major\": {\n" +
                                       "            \"version\": 1,\n" +
                                       "            \"ruleType\": \"=\"\n" +
                                       "        },\n" +
                                       "        \"minor\": {\n" +
                                       "          \"version\": 5,\n" +
                                       "          \"ruleType\": \"<\"\n" +
                                       "        }\n" +
                                       "      }\n" +
                                       "    },\n" +
                                       "    {\n" +
                                       "      \"groupId\": \"com.fasterxml.jackson.core\",\n" +
                                       "      \"rules\": {\n" +
                                       "        \"minor\": {\n" +
                                       "          \"version\": 13,\n" +
                                       "          \"ruleType\": \"<=\"\n" +
                                       "        }\n" +
                                       "      }\n" +
                                       "    },\n" +
                                       "    {\n" +
                                       "      \"groupId\": \"org.apache.logging.log4j\",\n" +
                                       "      \"link\":\"https://search.maven.org/search?q=g:org.apache.logging.log4j\",\n" +
                                       "      \"rules\": {\n" +
                                       "        \"major\": {\n" +
                                       "          \"version\": 2,\n" +
                                       "          \"ruleType\": \"=\"\n" +
                                       "        },\n" +
                                       "        \"minor\": {\n" +
                                       "          \"version\": 17,\n" +
                                       "          \"ruleType\": \"<\"\n" +
                                       "        }\n" +
                                       "      }\n" +
                                       "    }\n" +
                                       "  ],\n" +
                                       "  \"securityIssue\": [\n" +
                                       "    {\n" +
                                       "      \"groupId\": \"org.apache.logging.log4j\",\n" +
                                       "      \"comment\": \"CVE-2021-44832 : Log4j2 contains major security issue\",\n" +
                                       "      \"link\":\"https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44832\",\n" +
                                       "      \"level\" : \"critical\",\n" +
                                       "      \"rules\": {\n" +
                                       "        \"major\": {\n" +
                                       "          \"version\": 2,\n" +
                                       "          \"ruleType\": \"=\"\n" +
                                       "        },\n" +
                                       "        \"minor\": {\n" +
                                       "          \"version\": 17,\n" +
                                       "          \"ruleType\": \"<\"\n" +
                                       "        }\n" +
                                       "      }\n" +
                                       "    }\n" +
                                       "  ],\n" +
                                       "  \"ban\": [\n" +
                                       "    {\n" +
                                       "      \"groupId\": \"org.apache.logging.log4j\",\n" +
                                       "      \"comment\": \"banished because of CVE-2021-44832\",\n" +
                                       "      \"link\":\"https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2021-44832\",\n" +
                                       "      \"level\" : \"critical\",\n" +
                                       "      \"rules\": {\n" +
                                       "        \"major\": {\n" +
                                       "          \"version\": 2,\n" +
                                       "          \"ruleType\": \"=\"\n" +
                                       "        },\n" +
                                       "        \"minor\": {\n" +
                                       "          \"version\": 17,\n" +
                                       "          \"ruleType\": \"<\"\n" +
                                       "        }\n" +
                                       "      }\n" +
                                       "    }\n" +
                                       "  ]\n" +
                                       "}\n");
        resp.setStatus(SUCCCESS);
        resp.setContentType(MediaType.APPLICATION_JSON);
        resp.setCharacterEncoding(UTF_8);
    }
}
