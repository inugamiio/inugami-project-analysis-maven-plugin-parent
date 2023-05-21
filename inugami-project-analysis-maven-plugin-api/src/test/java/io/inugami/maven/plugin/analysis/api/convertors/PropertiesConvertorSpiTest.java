package io.inugami.maven.plugin.analysis.api.convertors;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PropertiesConvertorSpiTest {

    @Test
    void accept_nominal() {
        assertThat(new DefaultPropertiesConvertorSpi().accept("string")).isTrue();
    }

    @Test
    void accept_badValue() {
        assertThat(new DefaultPropertiesConvertorSpi().accept(null)).isFalse();
        assertThat(new DefaultPropertiesConvertorSpi().accept("other")).isFalse();
    }

    private static class DefaultPropertiesConvertorSpi implements PropertiesConvertorSpi {

        @Override
        public boolean accept(final String type) {
            return matchType(type, List.of("string"));
        }

        @Override
        public Map<String, String> convert(final String content) {
            return null;
        }
    }
}