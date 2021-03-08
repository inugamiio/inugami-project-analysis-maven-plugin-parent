package io.inugami.maven.plugin.analysis.api.actions;

public interface Neo4jValueEncoder {
    String encode(Object value);
}