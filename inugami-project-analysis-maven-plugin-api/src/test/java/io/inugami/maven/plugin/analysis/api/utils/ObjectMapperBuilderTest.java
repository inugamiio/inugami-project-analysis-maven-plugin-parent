package io.inugami.maven.plugin.analysis.api.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ObjectMapperBuilderTest {

    @Test
    void build_nominal() {
        assertThat(ObjectMapperBuilder.build()).isNotNull().isInstanceOf(ObjectMapper.class);
    }
}