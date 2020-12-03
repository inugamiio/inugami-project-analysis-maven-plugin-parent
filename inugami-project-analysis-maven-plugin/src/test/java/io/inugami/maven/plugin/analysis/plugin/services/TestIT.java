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

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;

@Slf4j
public class TestIT {


    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    public static void main(final String... args) {

        final String query = "Match (version:Version) where version.groupId= \"io.inugami.demo\"\n" +
                "                          and version.artifactId=\"spring-boot-training-rest\"\n" +
                "                          and version.version=\"0.0.1-SNAPSHOT\"\n" +
                "\n" +
                "OPTIONAL MATCH (v)-[:PROJECT_DEPENDENCY*0..10]->(depExposer:Version)-[expose:EXPOSE]->(serviceExpose:Service)\n" +
                "OPTIONAL MATCH (v)-[:PROJECT_DEPENDENCY*0..10]->(depConsumer:Version)-[consume:CONSUME]->(serviceConsume:Service)\n" +
                "\n" +
                "OPTIONAL MATCH (depVersionConsumer)-[dependenciesConsume:CONSUME]->(serviceExpose)\n" +
                "OPTIONAL MATCH (depVersionProducer)-[dependenciesExpose:EXPOSE]->(serviceConsume)\n" +
                "OPTIONAL MATCH (depExposerArtifact:Artifact)-[]->(depExposer)\n" +
                "OPTIONAL MATCH (depConsumerArtifact:Artifact)-[]->(depExposer)\n" +
                "\n" +
                "return  serviceExpose,\n" +
                "        serviceConsume,\n" +
                "        depExposer,\n" +
                "        depConsumer,\n" +
                "        depVersionConsumer,\n" +
                "        depVersionProducer,\n" +
                "        dependenciesConsume,\n" +
                "        dependenciesExpose,\n" +
                "        depExposerArtifact,\n" +
                "        depConsumerArtifact";

        final Driver driver = GraphDatabase.driver("bolt://localhost:7687",
                                                   AuthTokens.basic("neo4j", "password"));
        final Session session = driver.session();
        session.writeTransaction((TransactionWork)(tx)-> {

                final Result statementResult = tx.run("<<Cypher write query>>");

                tx.commit();
                tx.close();
                return null;

        });
        session.close();
        driver.close();
    }


}
