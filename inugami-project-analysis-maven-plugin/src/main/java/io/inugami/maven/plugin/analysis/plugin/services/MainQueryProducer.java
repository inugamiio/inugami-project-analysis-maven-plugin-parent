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
package io.inugami.maven.plugin.analysis.plugin.services;

import io.inugami.maven.plugin.analysis.api.actions.QueryProducer;
import io.inugami.maven.plugin.analysis.api.models.QueryDefinition;

import java.util.List;

public class MainQueryProducer implements QueryProducer {

    // =========================================================================
    // API
    // =========================================================================
    @Override
    public List<QueryDefinition> extractQueries() {
        return List.of(
                QueryDefinition.builder()
                               .path("META-INF/queries/search_services_rest.cql")
                               .name("search_services_rest")
                               .type("cql")
                               .description(
                                       "Allow to search all rest services consume or expose by current project and these dependencies who expose/consume them")
                               .build(),

                QueryDefinition.builder()
                               .path("META-INF/queries/search_consumers.cql")
                               .name("search_consumers")
                               .type("cql")
                               .description("Allow to search all consumed rest services")
                               .build(),

                QueryDefinition.builder()
                               .path("META-INF/queries/search_produce.cql")
                               .name("search_produce")
                               .type("cql")
                               .description("Allow to search all exposed rest services")
                               .build(),

                QueryDefinition.builder()
                               .path("META-INF/queries/search_properties.cql")
                               .name("search_properties")
                               .type("cql")
                               .description("Allow to search all consumed properties")
                               .build()

                      );
    }

}
