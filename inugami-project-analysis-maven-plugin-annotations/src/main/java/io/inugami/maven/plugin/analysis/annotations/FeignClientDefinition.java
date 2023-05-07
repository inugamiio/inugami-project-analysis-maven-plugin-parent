package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClientDefinition {
    Class<?> value() default FeignClientDefinition.None.class;

    public static class None {
    }
}
