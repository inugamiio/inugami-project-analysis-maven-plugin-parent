package io.inugami.maven.plugin.analysis.api.actions;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;

import java.util.List;

public interface ClassAnalyzer {
    default boolean accept(final Class<?> clazz, final ScanConext context) {
        return true;
    }

    List<JsonObject> analyze(Class<?> clazz, ScanConext context);
}
