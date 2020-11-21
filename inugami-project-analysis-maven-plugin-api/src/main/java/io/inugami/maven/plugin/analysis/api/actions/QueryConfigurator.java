package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.Gav;

import java.util.HashMap;
import java.util.Map;

public interface QueryConfigurator {
    default boolean accept(final String queryPath) {
        return false;
    }

    ConfigHandler<String, String> configure(String queryPath, Gav gav, ConfigHandler<String, String> configuration);

    default Map<String, String> gavToMap(final Gav gav) {
        if (gav == null) {
            return new HashMap<>();
        }
        else {
            return Map.ofEntries(
                    Map.entry("groupId", gav.getGroupId()),
                    Map.entry("artifactId", gav.getArtifactId()),
                    Map.entry("version", gav.getVersion())
                                );
        }
    }
}
