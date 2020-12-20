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
package io.inugami.maven.plugin.analysis.plugin.services.info.env;

import io.inugami.maven.plugin.analysis.api.actions.ProjectInformation;
import io.inugami.maven.plugin.analysis.api.actions.QueryConfigurator;
import io.inugami.maven.plugin.analysis.api.models.InfoContext;
import io.inugami.maven.plugin.analysis.api.tools.QueriesLoader;
import io.inugami.maven.plugin.analysis.api.tools.TemplateRendering;
import io.inugami.maven.plugin.analysis.api.tools.rendering.DataRow;
import io.inugami.maven.plugin.analysis.api.tools.rendering.Neo4jRenderingUtils;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.DefaultNeo4jDao;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class EnvInfo implements ProjectInformation, QueryConfigurator {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private static final VersionEnv   VERSION_ENV = new VersionEnv();
    // =========================================================================
    // QUERIES
    // =========================================================================
    private static final List<String> QUERIES     = List.of("META-INF/queries/search_env_info.cql");


    @Override
    public boolean accept(final String queryPath) {
        return QUERIES.contains(queryPath);
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void process(final InfoContext context) {
        final DefaultNeo4jDao dao = new DefaultNeo4jDao(context.getConfiguration());


        final String query = TemplateRendering.render(QueriesLoader.getQuery(QUERIES.get(0)),
                                                      configure(QUERIES.get(0),
                                                                null,
                                                                context.getConfiguration()));
        log.info("query:\n{}", query);
        final Map<String, Long> envs = new LinkedHashMap<>();
        final Map<String, Collection<DataRow>> firstPass = extractDataFromResultSet(dao.search(query),
                                                                                    (data, record) -> this
                                                                                            .buildModels(data, record,
                                                                                                         envs));

        final Map<String, Collection<DataRow>> data = VERSION_ENV.sortData(firstPass, envs, null);

        log.info("\n{}", Neo4jRenderingUtils.rendering(data, context.getConfiguration(), "env"));
        dao.shutdown();
    }


    // =========================================================================
    // BUILDER
    // =========================================================================
    public void buildModels(final Map<String, Collection<DataRow>> data,
                            final Map<String, Object> record,
                            final Map<String, Long> envs) {

        log.debug("record : {}", record);
        Collection<DataRow> lines = data.get(VERSION_ENV.ENVIRONMENTS);
        if (lines == null) {
            lines = new ArrayList<>();
            data.put(VERSION_ENV.ENVIRONMENTS, lines);
        }

        VERSION_ENV.buildLine("version", "env", "deploy", data, record, envs);
    }
}
