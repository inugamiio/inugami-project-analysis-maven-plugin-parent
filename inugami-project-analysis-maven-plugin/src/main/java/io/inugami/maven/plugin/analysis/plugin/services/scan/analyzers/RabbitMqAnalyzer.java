package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import io.inugami.api.models.data.basic.JsonObject;
import io.inugami.maven.plugin.analysis.api.actions.ClassAnalyzer;
import io.inugami.maven.plugin.analysis.api.models.ScanConext;

import java.util.List;

public class RabbitMqAnalyzer implements ClassAnalyzer {


    // =========================================================================
    // ATTRIBUTES
    // =========================================================================

    // =========================================================================
    // CONSTRUCTORS
    // =========================================================================


    // =========================================================================
    // API
    // =========================================================================
    @Override
    public boolean accept(final Class<?> clazz, final ScanConext context) {
        return false;
    }

    @Override
    public List<JsonObject> analyze(final Class<?> clazz, final ScanConext context) {
        return null;
    }
    // =========================================================================
    // OVERRIDES
    // =========================================================================

    // =========================================================================
    // GETTERS & SETTERS
    // =========================================================================
}
