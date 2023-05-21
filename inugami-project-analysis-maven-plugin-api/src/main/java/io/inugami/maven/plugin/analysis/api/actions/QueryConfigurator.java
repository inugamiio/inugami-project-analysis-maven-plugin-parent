package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.Gav;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.inugami.api.functionnals.FunctionalUtils.applyIfNotNull;
import static io.inugami.maven.plugin.analysis.api.constant.Constants.*;

public interface QueryConfigurator {
    default boolean accept(final String queryPath) {
        return false;
    }

    default ConfigHandler<String, String> configure(final String queryPath, final Gav gav, final ConfigHandler<String, String> configuration) {
        return configuration;
    }

    default Map<String, String> gavToMap(final Gav gav) {
        if (gav == null) {
            return new LinkedHashMap<>();
        } else {
            final Map<String, String> result = new LinkedHashMap<>();
            applyIfNotNull(gav.getGroupId(), value -> result.put(GROUP_ID, value));
            applyIfNotNull(gav.getArtifactId(), value -> result.put(ARTIFACT_ID, value));
            applyIfNotNull(gav.getVersion(), value -> result.put(VERSION, value));

            return result;
        }
    }
}
