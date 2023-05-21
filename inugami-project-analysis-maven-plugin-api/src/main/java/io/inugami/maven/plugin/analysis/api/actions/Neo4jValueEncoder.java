package io.inugami.maven.plugin.analysis.api.actions;

@FunctionalInterface
public interface Neo4jValueEncoder {
    String encode(Object value);
}