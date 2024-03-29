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
package io.inugami.maven.plugin.analysis.api.tools;

import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.api.actions.QueryProducer;
import io.inugami.maven.plugin.analysis.api.models.QueryDefinition;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QueriesLoader {
    private static final Set<QueryDefinition> QUERIES = new LinkedHashSet<>();

    // =========================================================================
    // API
    // =========================================================================
    public static synchronized  Set<QueryDefinition> loadQueries() {
        if (QUERIES.isEmpty()) {
            final List<QueryProducer> producers = SpiLoader.getInstance().loadSpiServicesByPriority(QueryProducer.class);
            if (producers != null) {
                producers.stream().map(QueryProducer::extractQueries).forEach(QUERIES::addAll);
            }
        }
        return QUERIES;
    }

    public static QueryDefinition getQuery(final String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }

        if (QUERIES.isEmpty()) {
            loadQueries();
        }

        return QUERIES.stream()
                      .filter(query -> path.equals(query.getPath()))
                      .findFirst()
                      .orElse(null);
    }

    public static QueryDefinition getQueryByName(final String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        if (QUERIES.isEmpty()) {
            loadQueries();
        }

        return QUERIES.stream()
                      .filter(query -> name.equals(query.getName()))
                      .findFirst()
                      .orElse(null);
    }
}
