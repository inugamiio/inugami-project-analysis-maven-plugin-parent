package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.maven.plugin.analysis.api.models.QueryDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QueryProducerTest {

    @Test
    void extractQueries_nominal() {
        assertThat(buildProducer().extractQueries()).isNotNull().hasSize(1);
    }

    private static QueryProducer buildProducer() {
        return () -> List.of(QueryDefinition.builder()
                                            .build());
    }
}