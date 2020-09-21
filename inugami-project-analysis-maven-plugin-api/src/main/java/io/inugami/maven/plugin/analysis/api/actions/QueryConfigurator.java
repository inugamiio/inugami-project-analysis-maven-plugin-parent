package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.processors.ConfigHandler;
import io.inugami.maven.plugin.analysis.api.models.Gav;

public interface QueryConfigurator {
    default boolean accept(final String queryPath) {
        return false;
    }

    ConfigHandler<String, String> configure(String queryPath, Gav gav, ConfigHandler<String, String> configuration);
}
