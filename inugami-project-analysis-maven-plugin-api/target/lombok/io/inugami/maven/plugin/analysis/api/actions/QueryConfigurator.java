package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.Gav;

import java.util.LinkedHashMap;
import java.util.Map;

public interface QueryConfigurator {
    default boolean accept(final String queryPath) {
        return false;
    }

    default ConfigHandler<String, String> configure(final String queryPath, final Gav gav, final ConfigHandler<String, String> configuration){
        return configuration;
    }

    default Map<String, String> gavToMap(final Gav gav) {
        if (gav == null) {
            return new LinkedHashMap<>();
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
