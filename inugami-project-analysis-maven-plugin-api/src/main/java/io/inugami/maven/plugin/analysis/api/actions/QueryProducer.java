package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.maven.plugin.analysis.api.models.QueryDefinition;

import java.util.List;

@FunctionalInterface
public interface QueryProducer {
    List<QueryDefinition> extractQueries();
}
