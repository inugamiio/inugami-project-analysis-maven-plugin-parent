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
package io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ResultWriter;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;
import io.inugami.maven.plugin.analysis.api.models.ScanNeo4jResult;
import io.inugami.maven.plugin.analysis.plugin.services.neo4j.Neo4jDao;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class Neo4jWriter implements ResultWriter {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final ScanNeo4jResult data = ScanNeo4jResult.builder().build();
    private       Neo4jDao        dao  = null;

    // =========================================================================
    // LIFECYCLE
    // =========================================================================
    @Override
    public void init(final ScanConext context) {
        dao = new Neo4jDao(context.getProject().getProperties());
    }

    protected void initializeNeo4jDriver(final String url, final String userName, final String password) {
        dao = new Neo4jDao(url, userName, password);
    }

    @Override
    public void shutdown(final ScanConext context) {
        dao.shutdown();
    }

    // =========================================================================
    // API
    // =========================================================================

    @Override
    public boolean accept(final JsonObject value, final ScanConext context) {
        final boolean result = value instanceof ScanNeo4jResult;
        if (result) {
            final ScanNeo4jResult neo4jResult = (ScanNeo4jResult) value;
            data.addNode(neo4jResult.getNodes());
            data.addRelationship(neo4jResult.getRelationships());
            data.addNodeToDelete(neo4jResult.getNodesToDeletes());
        }

        return result;
    }

    @Override
    public void write() {
        dao.deleteNodes(data.getNodesToDeletes());
        dao.saveNodes(data.getNodes());
        dao.saveRelationships(data.getRelationships());
    }

}
