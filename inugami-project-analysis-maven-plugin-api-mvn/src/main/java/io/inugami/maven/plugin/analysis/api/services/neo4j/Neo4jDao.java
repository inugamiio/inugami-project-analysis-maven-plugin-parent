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
package io.inugami.maven.plugin.analysis.api.services.neo4j;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;

import java.util.List;
import java.util.Map;

public interface Neo4jDao {
    void shutdown();

    Node getNode(final String name, final String type);

    List<Record> search(final String query);

    String searchQuery(final String name, final String type);

    void deleteNodes(final List<String> nodesToDeletes);

    void deleteNode(final String uid);

    void saveNodes(final List<io.inugami.maven.plugin.analysis.api.models.Node> nodes);

    void saveRelationships(final List<Relationship> relationships);

    void deleteRelationship(final List<Relationship> relationshipsToDeletes);

    void processScripts(final List<String> scripts, final ConfigHandler<String, String> configuration);

    void processSave(final String cypherQuery);


    String buildDeleteNodeQuery(final String uid);

    String buildCreateNodeQuery(final io.inugami.maven.plugin.analysis.api.models.Node node,
                                final Map<String, Object> parameters);

    String buildRelationshipQuery(final Relationship relationship);

    String buildRelationshipToDeleteQuery(final Relationship relationship);

    String convertValue(final Object value);
}
