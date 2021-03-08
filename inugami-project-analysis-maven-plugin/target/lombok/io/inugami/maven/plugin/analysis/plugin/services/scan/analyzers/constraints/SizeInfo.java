package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.constraints;

import io.inugami.api.models.JsonBuilder;
import io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers.ConstraintInformationResolver;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

public class SizeInfo implements ConstraintInformationResolver {

    @Override
    public boolean accept(final Annotation annotation) {
        return annotation instanceof Size;
    }

    @Override
    public void appendInformation(final Map<String, Serializable> properties, final Annotation annotationRaw) {
        final Size        annotation = (Size) annotationRaw;
        final JsonBuilder info       = new JsonBuilder();
        info.openObject();
        info.addField("min").write(annotation.min()).addSeparator();
        info.addField("max").write(annotation.max()).addSeparator();
        info.closeObject();

        properties.put(CONSTRAINT_DETAIL, info.toString());
    }
}
