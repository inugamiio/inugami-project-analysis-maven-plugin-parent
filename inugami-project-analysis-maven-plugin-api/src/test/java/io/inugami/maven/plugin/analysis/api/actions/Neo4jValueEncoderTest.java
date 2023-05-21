package io.inugami.maven.plugin.analysis.api.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Neo4jValueEncoderTest {

    @Test
    void encode_withNullValue() {
        assertThat(buildEncoder().encode(null)).isNull();
    }

    @Test
    void encode_withValue() {
        assertThat(buildEncoder().encode(Integer.valueOf(1))).isEqualTo("1");
    }

    private static Neo4jValueEncoder buildEncoder() {
        return value -> value == null ? null : String.valueOf(value);
    }
}