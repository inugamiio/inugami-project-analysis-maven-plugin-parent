package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.constraints;

import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.ConstraintInformationResolver;

import javax.validation.constraints.DecimalMin;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

public class DecimalMinInfo implements ConstraintInformationResolver {

    @Override
    public boolean accept(final Annotation annotation) {
        return annotation instanceof DecimalMin;
    }

    @Override
    public void appendInformation(final Map<String, Serializable> properties, final Annotation annotation) {
        final DecimalMin decimal = (DecimalMin)annotation;
        properties.put(CONSTRAINT_DETAIL, "> "+ decimal.value());
    }
}
