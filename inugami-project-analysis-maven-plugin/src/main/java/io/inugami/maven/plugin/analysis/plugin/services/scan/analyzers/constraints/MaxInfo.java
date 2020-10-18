package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.constraints;

import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.ConstraintInformationResolver;

import javax.validation.constraints.Max;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

public class MaxInfo implements ConstraintInformationResolver {

    public static final String INTEGER = "integer";
    public static final String FRACTION = "fraction";

    @Override
    public boolean accept(final Annotation annotation) {
        return annotation instanceof Max;
    }

    @Override
    public void appendInformation(final Map<String, Serializable> properties, final Annotation annotationRaw) {
        final Max annotation = (Max)annotationRaw;
        properties.put(CONSTRAINT_DETAIL, "< "+annotation.value());
    }
}
