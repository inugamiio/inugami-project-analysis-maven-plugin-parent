package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeignClientDefinition {
    Class<?> value() default FeignClientDefinition.None.class;

    @SuppressWarnings({"java:S2094"})
    public static class None {
    }
}
