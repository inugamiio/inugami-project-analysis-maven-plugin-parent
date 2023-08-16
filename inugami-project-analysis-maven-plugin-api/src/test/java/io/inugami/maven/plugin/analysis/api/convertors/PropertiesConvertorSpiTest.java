package io.inugami.maven.plugin.analysis.api.convertors;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.inugami.commons.test.UnitTestData.OTHER;
import static org.assertj.core.api.Assertions.assertThat;

class PropertiesConvertorSpiTest {

    public static final String VALUE = "value";

    @Test
    void accept_nominal() {
        assertThat(new DefaultPropertiesConvertorSpi().accept("string")).isTrue();
    }

    @Test
    void accept_badValue() {
        assertThat(new DefaultPropertiesConvertorSpi().accept(null)).isFalse();
        assertThat(new DefaultPropertiesConvertorSpi().accept("other")).isFalse();
    }

    @Test
    void matchType_withNullValues(){
        PropertiesConvertorSpi convertorSpi = new PropertiesConvertorSpi(){

            @Override
            public boolean accept(final String type) {
                return false;
            }

            @Override
            public Map<String, String> convert(final String content) {
                return null;
            }
        };

        assertThat(convertorSpi.matchType(null, List.of(VALUE))).isFalse();
        assertThat(convertorSpi.matchType(VALUE, null)).isFalse();
        assertThat(convertorSpi.matchType(OTHER, List.of(VALUE))).isFalse();
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