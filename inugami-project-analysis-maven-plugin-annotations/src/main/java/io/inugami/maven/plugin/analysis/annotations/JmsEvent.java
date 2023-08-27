package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JmsEvent {
    Class<?> value() default JmsEvent.None.class;

    @SuppressWarnings({"java:S2094"})
    public static class None {

    }
}
