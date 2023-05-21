package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.maven.plugin.analysis.api.models.Gav;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;

class QueryConfiguratorTest {


    @Test
    void gavToMap_withNullValue() {
        final QueryConfigurator   configurator = buildConfigurator();
        final Map<String, String> result       = configurator.gavToMap(null);
        assertThat(result).isNotNull().isInstanceOf(LinkedHashMap.class);
    }

    @Test
    void gavToMap_withGav() {
        final QueryConfigurator configurator = buildConfigurator();
        final Map<String, String> result = configurator.gavToMap(Gav.builder()
                                                                    .groupId("io.inugami")
                                                                    .artifactId("some-artifact")
                                                                    .version("0.0.1")
                                                                    .build());
        assertThat(result).isNotNull().isInstanceOf(LinkedHashMap.class);
        assertThat(result.get(GROUP_ID)).isEqualTo("io.inugami");
        assertThat(result.get(ARTIFACT_ID)).isEqualTo("some-artifact");
        assertThat(result.get(VERSION)).isEqualTo("0.0.1");

    }

    @Test
    void accept_nominal() {
        final QueryConfigurator configurator = buildConfigurator();
        assertThat(configurator.accept(null)).isFalse();
        assertThat(configurator.accept("/")).isFalse();
    }


    private QueryConfigurator buildConfigurator() {
        return new QueryConfigurator() {
        };
    }
}