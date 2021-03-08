package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.constraints;

import io.inugami.api.models.JsonBuilder;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.ConstraintInformationResolver;

import javax.validation.constraints.Digits;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

public class DigitsInfo implements ConstraintInformationResolver {

    public static final String INTEGER = "integer";
    public static final String FRACTION = "fraction";
    public static final String RULE = "rule";
    public static final String LESS_THAN = "less than";

    @Override
    public boolean accept(final Annotation annotation) {
        return annotation instanceof Digits;
    }

    @Override
    public void appendInformation(final Map<String, Serializable> properties, final Annotation annotationRaw) {
        final Digits annotation = (Digits)annotationRaw;

        final JsonBuilder info = new JsonBuilder();
        info.openObject();
        info.addField(RULE).valueQuot(LESS_THAN);
        info.addField(INTEGER).write(annotation.integer()).addSeparator();
        info.addField(FRACTION).write(annotation.fraction()).addSeparator();
        info.closeObject();

        properties.put(CONSTRAINT_DETAIL, info.toString());
    }
}
