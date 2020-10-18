package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.constraints;

import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.ConstraintInformationResolver;

import javax.validation.constraints.DecimalMax;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

public class DecimalMaxInfo implements ConstraintInformationResolver {

    @Override
    public boolean accept(final Annotation annotation) {
        return annotation instanceof DecimalMax;
    }

    @Override
    public void appendInformation(final Map<String, Serializable> properties, final Annotation annotation) {
        final DecimalMax decimalMax = (DecimalMax)annotation;
        properties.put(CONSTRAINT_DETAIL, "< "+decimalMax.value());
    }
}
