package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.constraints;

import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.ConstraintInformationResolver;

import javax.validation.constraints.Min;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

public class MinInfo implements ConstraintInformationResolver {

    @Override
    public boolean accept(final Annotation annotation) {
        return annotation instanceof Min;
    }

    @Override
    public void appendInformation(final Map<String, Serializable> properties, final Annotation annotationRaw) {
        final Min annotation = (Min)annotationRaw;
        properties.put(CONSTRAINT_DETAIL, "> "+annotation.value());
    }
}
