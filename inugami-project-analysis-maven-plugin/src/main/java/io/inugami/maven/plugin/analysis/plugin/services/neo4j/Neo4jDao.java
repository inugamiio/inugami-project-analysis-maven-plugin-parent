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
package io.inugami.maven.plugin.analysis.plugin.services.neo4j;

import io.inugami.api.models.JsonBuilder;
import io.inugami.api.processors.ConfigHandler;
import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.api.actions.Neo4jValueEncoder;
import io.inugami.maven.plugin.analysis.api.models.Relationship;
import io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j.DefaultNeo4jEncoder;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import java.io.Serializable;
import java.util.*;

@Slf4j
public class Neo4jDao {

    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final List<Neo4jValueEncoder> encoders;
    private final Driver                  driver;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    public Neo4jDao(final Properties properties) {
        final String boltUri  = String.valueOf(properties.get("inugami.maven.plugin.analysis.writer.neo4j.url"));
        final String login    = String.valueOf(properties.get("inugami.maven.plugin.analysis.writer.neo4j.user"));
        final String password = String.valueOf(properties.get("inugami.maven.plugin.analysis.writer.neo4j.password"));

        encoders = SpiLoader.INSTANCE.loadSpiServicesWithDefault(Neo4jValueEncoder.class, new DefaultNeo4jEncoder());
        driver   = GraphDatabase.driver(boltUri, AuthTokens.basic(login, password));
    }

    public Neo4jDao(final ConfigHandler<String, String> configuration) {
        encoders = SpiLoader.INSTANCE.loadSpiServicesWithDefault(Neo4jValueEncoder.class, new DefaultNeo4jEncoder());
        driver   = GraphDatabase.driver(configuration.grab("inugami.maven.plugin.analysis.writer.neo4j.url"),
                                        AuthTokens.basic(configuration
                                                                 .grab("inugami.maven.plugin.analysis.writer.neo4j.user"),
                                                         configuration
                                                                 .grab("inugami.maven.plugin.analysis.writer.neo4j.password")));
    }

    public Neo4jDao(final String boltUri, final String login, final String password) {
        encoders = SpiLoader.INSTANCE.loadSpiServicesWithDefault(Neo4jValueEncoder.class, new DefaultNeo4jEncoder());
        driver   = GraphDatabase.driver(boltUri, AuthTokens.basic(login, password));
    }


    public void shutdown() {
        driver.session().close();
        driver.close();
    }

    // =========================================================================
    // API
    // =========================================================================
    public void deleteNodes(final List<String> nodesToDeletes) {
        if (nodesToDeletes != null) {
            final int size = nodesToDeletes.size();

            log.info("{} nodes to delete", size);
            for (int i = 0; i < size; i++) {
                if (i % 10 == 0) {
                    log.info("delete node : {}/{}", i, size);
                }
                final String uid = nodesToDeletes.get(i);
                deleteNode(uid);
            }
            log.info("deleting nodes done");
        }
    }

    public void deleteNode(final String uid) {
        final Session session = driver.session();
        final Node result = session.writeTransaction(new TransactionWork<Node>() {
            @Override
            public Node execute(final Transaction tx) {
                final Result statementResult = tx.run(buildDeleteNodeQuery(uid));
                Node         result          = null;
                if (statementResult != null) {
                    final List<Record> record = statementResult.list();
                    if (record != null && !record.isEmpty()) {
                        result = record.get(0).get(0).asNode();
                    }
                }
                tx.commit();
                tx.close();
                return result;
            }
        });
    }

    public void saveNodes(final List<io.inugami.maven.plugin.analysis.api.models.Node> nodes) {
        if (nodes != null) {
            final int size = nodes.size();
            log.info("{} nodes to create", size);
            for (int i = 0; i < size; i++) {
                if (i % 50 == 0) {
                    log.info("create node : {}/{}", i, size);
                }
                final io.inugami.maven.plugin.analysis.api.models.Node node       = nodes.get(i);
                final Map<String, Object>                              parameters = new HashMap<>();
                if (node.getProperties() != null) {
                    parameters.putAll(node.getProperties());
                }
                parameters.put("name", node.getUid());
                parameters.put("shortName", node.getName());
                final String cypherQuery = buildCreateNodeQuery(node, parameters);

                processSave(cypherQuery);
            }
            log.info("creating nodes done");
        }
    }

    public void saveRelationships(final List<Relationship> relationships) {
        if (relationships != null) {
            final int size = relationships.size();
            log.info("{} relationships to create", size);
            for (int i = 0; i < size; i++) {
                final Relationship relationship = relationships.get(i);
                if (i % 50 == 0) {
                    log.info("create node : {}/{}", i, size);
                }
                final String cypherQuery = buildRelationshipQuery(relationship);
                processSave(cypherQuery);
            }
            log.info("creating relationships done");
        }
    }

    public void processSave(final String cypherQuery) {
        final Session session = driver.session();

        try {
            final Node result = session.writeTransaction(new TransactionWork<Node>() {
                @Override
                public Node execute(final Transaction tx) {
                    final Result statementResult = tx.run(cypherQuery);
                    Node         result          = null;

                    if (statementResult != null) {
                        final List<Record> record = statementResult.list();
                        if (record != null && !record.isEmpty()) {
                            result = record.get(0).get(0).asNode();
                        }
                    }
                    tx.commit();
                    tx.close();


                    return result;
                }
            });
        }
        catch (final Exception error) {
            log.error(error.getMessage(), error);
            log.error("enable to execute query : \n{}", cypherQuery);
        }
    }

    public Node getNode(final String name, final String type) {
        final Session             session    = driver.session();
        final Map<String, Object> parameters = Map.ofEntries(Map.entry("name", "Foobar"));
        final String              query      = searchQuery(name, type);
        Node                      result     = null;
        try {
            result = session.readTransaction(new TransactionWork<Node>() {
                @Override
                public Node execute(final Transaction tx) {

                    final Result statementResult = tx.run(searchQuery(name, type), parameters);
                    Node         result          = null;

                    if (statementResult != null) {
                        final List<Record> record = statementResult.list();
                        if (record != null && !record.isEmpty()) {
                            result = record.get(0).get(0).asNode();
                        }
                    }
                    tx.commit();
                    tx.close();

                    return result;
                }
            });
        }
        catch (final Exception error) {
            log.error(error.getMessage(), error);
            log.error("enable to execute query : \n{}", query);
        }
        return result;
    }


    public List<Record> search(final String query) {
        final Session      session = driver.session();
        final List<Record> record  = new ArrayList<>();
        try {
            session.readTransaction(new TransactionWork<Node>() {
                @Override
                public Node execute(final Transaction tx) {

                    final Result statementResult = tx.run(query);
                    final Node   result          = null;
                    List<Record> resultSet       = null;
                    if (statementResult != null) {
                        resultSet = statementResult.list();
                    }
                    tx.commit();
                    tx.close();

                    if (resultSet != null) {
                        record.addAll(resultSet);
                    }
                    return null;
                }
            });
        }
        catch (final Exception error) {
            log.error(error.getMessage(), error);
            log.error("enable to execute query : \n{}", query);
        }
        return record;
    }


    // =========================================================================
    // QUERIES
    // =========================================================================
    public String searchQuery(final String name, final String type) {
        return new StringBuilder().append("MATCH (n:").append(type).append(") where n.name=\"")
                                  .append(name).append("\" return n")
                                  .toString();
    }

    public String buildDeleteNodeQuery(final String uid) {
        return new JsonBuilder().write("MATCH (n) where n.name=").valueQuot(uid)
                                .write(" detach delete n")
                                .toString();
    }

    public String buildCreateNodeQuery(final io.inugami.maven.plugin.analysis.api.models.Node node,
                                       final Map<String, Object> parameters) {
        final StringBuilder query  = new StringBuilder();
        final String        action = "MERGE";
        query.append(String.format("%s (n:%s {", action, node.getType()));

        final Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, Object> entry        = iterator.next();
            final String                    encodedValue = convertValue(entry.getValue());
            query.append(entry.getKey()).append(":").append(encodedValue);
            if (iterator.hasNext()) {
                query.append(",");
            }
        }

        query.append(String.format("})", action, node.getType()));
        query.append(" return n");
        return query.toString();
    }


    public String buildRelationshipQuery(final Relationship relationship) {
        final StringBuilder query = new StringBuilder();
        query.append(" MATCH (f) WHERE f.name=\"").append(relationship.getFrom()).append("\"").append("\n");
        query.append(" MATCH (to) WHERE to.name=\"").append(relationship.getTo()).append("\"").append("\n");
        query.append(" MERGE (f)-[r:").append(relationship.getType());

        if (relationship.getProperties() != null && !relationship.getProperties().isEmpty()) {
            query.append(" {");
            final Iterator<Map.Entry<String, Serializable>> iterator = relationship.getProperties().entrySet()
                                                                                   .iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Serializable> entry        = iterator.next();
                final String                          encodedValue = convertValue(entry.getValue());
                query.append(entry.getKey()).append(":").append(encodedValue);
                if (iterator.hasNext()) {
                    query.append(",");
                }
            }
            query.append("}");
        }


        query.append("]->(to)").append("\n");


        return query.toString();
    }

    public String convertValue(final Object value) {
        String result = null;
        for (final Neo4jValueEncoder encoder : encoders) {
            result = encoder.encode(value);
            if (result != null) {
                break;
            }
        }
        return result;
    }


}
