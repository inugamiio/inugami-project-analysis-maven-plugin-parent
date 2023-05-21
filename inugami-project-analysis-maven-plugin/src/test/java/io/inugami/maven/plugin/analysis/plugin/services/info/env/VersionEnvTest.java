package io.inugami.maven.plugin.analysis.plugin.services.info.env;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

class VersionEnvTest {


    @Test
    void sortEnvironment_withEnvs_shouldSort() {
        final VersionEnv service = new VersionEnv();

        assertThat(service.sortEnvironment(Map.ofEntries(
                Map.entry("PREP1", 3L),
                Map.entry("DEV", 1L),
                Map.entry("PREP2", 3L),
                Map.entry("INT", 2L)
        )))
                .isEqualTo(List.of("DEV", "INT", "PREP1", "PREP2"));
    }
}