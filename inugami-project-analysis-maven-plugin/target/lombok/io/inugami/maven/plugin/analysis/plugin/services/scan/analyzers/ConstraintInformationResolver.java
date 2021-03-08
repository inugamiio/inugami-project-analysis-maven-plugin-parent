package io.inugami.maven.plugin.analysis.plugin.services.scan.analyzers;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Map;

public interface ConstraintInformationResolver {
    String CONSTRAINT_DETAIL = "constraintDetail";
    boolean accept(Annotation annotation);
    void appendInformation(Map<String, Serializable> properties, Annotation annotation);
}
