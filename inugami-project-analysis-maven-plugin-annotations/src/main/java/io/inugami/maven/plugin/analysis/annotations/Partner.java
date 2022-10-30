package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;


@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Partner {
    String name() default "";

    String shortName() default "";

    String type() default "";

    String service() default "";

    String subService() default "";

    String description() default "";

    String protocol() default "";

    String tokenType() default "";
}
