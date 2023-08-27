package io.inugami.maven.plugin.analysis.api.utils.reflection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownJsonTypeTest {

    @Test
    void convertToJson_nominal() {
        assertThat(new UnknownJsonType().convertToJson()).isEqualTo("\"<object>\"");
    }
}