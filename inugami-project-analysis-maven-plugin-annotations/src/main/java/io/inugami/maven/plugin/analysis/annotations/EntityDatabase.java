package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntityDatabase {
    String DEFAULT_TYPE = "SQL";

    String value() default "";
    String type() default DEFAULT_TYPE;
}
