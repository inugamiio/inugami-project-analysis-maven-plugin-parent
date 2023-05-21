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
import io.inugami.maven.plugin.analysis.api.services.neo4j.Neo4jDao;
import io.inugami.maven.plugin.analysis.api.tools.SecurityUtils;
import io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j.DefaultNeo4jEncoder;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;

import java.io.Serializable;
import java.util.*;

@Slf4j
public class DefaultNeo4jDao implements Neo4jDao {

    public static final String                  ERR_MESSAGE = "enable to execute query : \n{}";
    // =========================================================================
    // ATTRIBUTES
    // =========================================================================
    private final       List<Neo4jValueEncoder> encoders;
    private final       Driver                  driver;

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================
    protected DefaultNeo4jDao() {
        encoders = null;
        driver = null;
    }

    public DefaultNeo4jDao(final Properties properties) {
        final String boltUri  = String.valueOf(properties.get("inugami.maven.plugin.analysis.writer.neo4j.url"));
        final String login    = String.valueOf(properties.get("inugami.maven.plugin.analysis.writer.neo4j.user"));
        final String password = decodePassword(properties);


        encoders = SpiLoader.getInstance().loadSpiServicesWithDefault(Neo4jValueEncoder.class, new DefaultNeo4jEncoder());
        driver = GraphDatabase.driver(boltUri, AuthTokens.basic(login, password));
    }


    public DefaultNeo4jDao(final ConfigHandler<String, String> configuration) {
        encoders = SpiLoader.getInstance().loadSpiServicesWithDefault(Neo4jValueEncoder.class, new DefaultNeo4jEncoder());
        driver = GraphDatabase.driver(configuration.grab("inugami.maven.plugin.analysis.writer.neo4j.url"),
                                      AuthTokens.basic(configuration
                                                               .grab("inugami.maven.plugin.analysis.writer.neo4j.user"),
                                                       decodePassword(configuration)));
    }

    public DefaultNeo4jDao(final String boltUri, final String login, final String password) {
        encoders = SpiLoader.getInstance().loadSpiServicesWithDefault(Neo4jValueEncoder.class, new DefaultNeo4jEncoder());
        driver = GraphDatabase.driver(boltUri, AuthTokens.basic(login, password));
    }


    @Override
    public void shutdown() {
        driver.session().close();
        driver.close();
    }

    private String decodePassword(final ConfigHandler<String, String> config) {
        final String password   = config.grab("inugami.maven.plugin.analysis.writer.neo4j.password");
        final String secretPass = config.grabOrDefault("inugami.maven.plugin.analysis.secret", null);
        return SecurityUtils.decodeAes(password, secretPass);
    }

    private String decodePassword(final Properties properties) {
        final Object password   = properties.get("inugami.maven.plugin.analysis.writer.neo4j.password");
        final Object secretPass = properties.get("inugami.maven.plugin.analysis.secret");
        return SecurityUtils.decodeAes(password, secretPass);
    }


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public void deleteNodes(final List<String> nodesToDeletes) {
        if (nodesToDeletes != null) {
            final int size = nodesToDeletes.size();

            log.info("{} nodes to delete", size);
            String previous = null;
            for (int i = 0; i < size; i++) {
                previous = writeProgression(i, size, previous);

                final String uid = nodesToDeletes.get(i);
                deleteNode(uid);
            }
            log.info("deleting nodes done");
        }
    }


    @Override
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

    @Override
    public void saveNodes(final List<io.inugami.maven.plugin.analysis.api.models.Node> nodes) {
        if (nodes == null) {
            return;
        }
        final int size = nodes.size();
        log.info("{} nodes to create", size);
        String previous = null;
        for (int i = 0; i < size; i++) {
            previous = writeProgression(i, size, previous);
            final io.inugami.maven.plugin.analysis.api.models.Node node       = nodes.get(i);
            final Map<String, Object>                              parameters = extractProperties(node);

            parameters.put("name", node.getUid());
            parameters.put("shortName", node.getName());
            final String cypherQuery = buildCreateNodeQuery(node, parameters);

            processSave(cypherQuery);
        }
        log.info("creating nodes done");
    }

    private static Map<String, Object> extractProperties(final io.inugami.maven.plugin.analysis.api.models.Node node) {
        final Map<String, Object> parameters = new LinkedHashMap<>();
        if (node.getProperties() != null) {
            for (final Map.Entry<String, Serializable> entry : node.getProperties().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    parameters.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return parameters;
    }

    @Override
    public void saveRelationships(final List<Relationship> relationships) {
        if (relationships != null) {
            final int size = relationships.size();
            log.info("{} relationships to create", size);
            String previous = null;
            for (int i = 0; i < size; i++) {
                final Relationship relationship = relationships.get(i);
                previous = writeProgression(i, size, previous);
                final String cypherQuery = buildRelationshipQuery(relationship);
                processSave(cypherQuery);
            }
            log.info("creating relationships done");
        }
    }

    @Override
    public void deleteRelationship(final List<Relationship> relationshipsToDeletes) {
        if (relationshipsToDeletes != null) {
            final int size = relationshipsToDeletes.size();
            log.info("{} relationships to delete", size);
            String previous = null;
            for (int i = 0; i < size; i++) {
                final Relationship relationship = relationshipsToDeletes.get(i);
                previous = writeProgression(i, size, previous);
                final String cypherQuery = buildRelationshipToDeleteQuery(relationship);
                processSave(cypherQuery);
            }
            log.info("delete relationships done");
        }
    }

    @Override
    public void processScripts(final List<String> scripts, final ConfigHandler<String, String> configuration) {
        if (scripts != null) {
            String    previous = null;
            final int size     = scripts.size();
            log.info("{} script to process", size);
            for (int i = 0; i < size; i++) {
                previous = writeProgression(i, size, previous);
                final String script      = scripts.get(i);
                final String cypherQuery = configuration == null ? script : configuration.applyProperties(script);
                processSave(cypherQuery);
            }
        }
    }


    @Override
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
        } catch (final Exception error) {
            log.error(error.getMessage(), error);
            log.error(ERR_MESSAGE, cypherQuery);
        }
    }

    @Override
    public Node getNode(final String name, final String type) {
        return getNode(name, type, null);
    }

    @Override
    public Node getNode(final String name, final String type, final Map<String, Object> parameters) {
        final Session             session = driver.session();
        final Map<String, Object> params  = parameters == null ? new HashMap<>() : parameters;
        final String              query   = searchQuery(name, type);
        Node                      result  = null;
        try {
            result = session.readTransaction(new TransactionWork<Node>() {
                @Override
                public Node execute(final Transaction tx) {

                    final Result statementResult = tx.run(searchQuery(name, type), params);
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
        } catch (final Exception error) {
            log.error(error.getMessage(), error);
            log.error(ERR_MESSAGE, query);
        }
        return result;
    }

    @Override
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
        } catch (final Exception error) {
            log.error(error.getMessage(), error);
            log.error(ERR_MESSAGE, query);
        }
        return record;
    }


    // =========================================================================
    // QUERIES
    // =========================================================================
    @Override
    public String searchQuery(final String name, final String type) {
        return new StringBuilder().append("MATCH (n:").append(type).append(") where n.name=\"")
                                  .append(name).append("\" return n")
                                  .toString();
    }

    @Override
    public String buildDeleteNodeQuery(final String uid) {
        return new JsonBuilder().write("MATCH (n) where n.name=").valueQuot(uid)
                                .write(" detach delete n")
                                .toString();
    }

    @Override
    public String buildCreateNodeQuery(final io.inugami.maven.plugin.analysis.api.models.Node node,
                                       final Map<String, Object> parameters) {
        final StringBuilder query  = new StringBuilder();
        final String        action = "MERGE";
        query.append(String.format("%s (n:%s {", action, node.getType()));

        final Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator();
        boolean                                   first    = true;
        while (iterator.hasNext()) {
            final Map.Entry<String, Object> entry        = iterator.next();
            final String                    encodedValue = convertValue(entry.getValue());
            if (encodedValue != null) {
                if (!first) {
                    query.append(",");
                }
                query.append(entry.getKey()).append(":").append(encodedValue);
                first = false;
            }
        }

        query.append(String.format("})", action, node.getType()));
        query.append(" return n");
        return query.toString();
    }


    @Override
    public String buildRelationshipQuery(final Relationship relationship) {
        final StringBuilder query = new StringBuilder();
        query.append(" MATCH (f) WHERE f.name=\"").append(relationship.getFrom()).append("\"").append("\n");
        query.append(" MATCH (to) WHERE to.name=\"").append(relationship.getTo()).append("\"").append("\n");
        query.append(" MERGE (f)-[r:").append(relationship.getType());

        if (relationship.getProperties() != null && !relationship.getProperties().isEmpty()) {
            query.append(" {");
            boolean first = true;
            final Iterator<Map.Entry<String, Serializable>> iterator = relationship.getProperties().entrySet()
                                                                                   .iterator();
            while (iterator.hasNext()) {
                final Map.Entry<String, Serializable> entry        = iterator.next();
                final String                          encodedValue = convertValue(entry.getValue());
                if (encodedValue != null) {
                    if (!first) {
                        query.append(",");
                    }
                    query.append(entry.getKey()).append(":").append(encodedValue);
                    first = false;
                }

            }
            query.append("}");
        }
        query.append("]->(to)").append("\n");
        return query.toString();
    }

    @Override
    public String buildRelationshipToDeleteQuery(final Relationship relationship) {
        final JsonBuilder query = new JsonBuilder();

        query.write(" MATCH (f)-[r:");
        if (relationship.getType() != null) {
            query.write(relationship.getType());
        }
        query.write("]->(to) where ");
        query.write("f.uid=").valueQuot(relationship.getFrom());
        query.write(" and ");
        query.write("to.uid=").valueQuot(relationship.getTo());
        query.write(" delete r");
        return query.toString();
    }

    @Override
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


    private String writeProgression(final int cursor, final int size, final String previous) {
        final int percent = (int) (((Double.valueOf(cursor) + 1) / size) * 100);

        final String current = new StringBuilder().append(percent).append("%").toString();
        if (!current.equals(previous)) {
            System.out.println(current);
        }
        return current;
    }


}
