package io.inugami.maven.plugin.analysis.plugin.services.neo4j;

import io.inugami.api.spi.SpiLoader;
import io.inugami.maven.plugin.analysis.api.actions.Neo4jValueEncoder;
import io.inugami.maven.plugin.analysis.api.models.Node;
import io.inugami.maven.plugin.analysis.plugin.services.writer.neo4j.DefaultNeo4jEncoder;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultNeo4jDaoTest {

    @Test
    void buildCreateNodeQuery_nominal() {
        final DefaultNeo4jDao     defaultNeo4jDao = buildDefaultNeo4jDao();
        final Map<String, Object> parameters      = new LinkedHashMap<>();
        parameters.put("date", "2023-05-21");

        final String result = defaultNeo4jDao.buildCreateNodeQuery(Node.builder()
                                                                       .type("Version")
                                                                       .uid("uid")
                                                                       .name("name")
                                                                       .addProperty("color", "red")
                                                                       .build(), parameters);

        assertThat(result).isEqualTo("MERGE (n:Version {date:\"2023-05-21\"}) return n");
    }

    private DefaultNeo4jDao buildDefaultNeo4jDao() {

        return DefaultNeo4jDao.builder()
                              .encoders(SpiLoader.getInstance().loadSpiServicesWithDefault(Neo4jValueEncoder.class, new DefaultNeo4jEncoder()))
                              .build();
    }
}