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

import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Neo4jWriterIT {
    @Disabled
    @Test
    void processSave() {
        final Neo4jWriter writer = new Neo4jWriter();
        writer.initializeNeo4jDriver("bolt://localhost:7687", "neo4j", "password");

        final Map<String, Object> parameters = Map.ofEntries(
                Map.entry("name", "Foobar"),
                Map.entry("value", "hello")
        );
        final String query = writer.getDao().buildCreateNodeQuery(Node.builder()
                                                                      .type("Test")
                                                                      .build(), parameters);


        final Map<String, Object> parameters2 = Map.ofEntries(
                Map.entry("name", "Joe"),
                Map.entry("value", "other ...")
        );
        final String query2 = writer.getDao().buildCreateNodeQuery(Node.builder()
                                                                       .type("Test")
                                                                       .build(), parameters2);
        writer.getDao().processSave(query);
        writer.getDao().processSave(query2);


        final LinkedHashMap<String, Serializable> properties = new LinkedHashMap<>();
        properties.put("nb", 5);
        properties.put("value", "basic text");
        final String queryRel = writer.getDao().buildRelationshipQuery(Relationship.builder()
                                                                                   .from("Foobar")
                                                                                   .to("Joe")
                                                                                   .type("DEPS")
                                                                                   .properties(properties)
                                                                                   .build());
        writer.getDao().processSave(queryRel);
        assertThat(queryRel).isNotNull();
    }
}