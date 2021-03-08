package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitMqEvent {
    Class<?> value() default RabbitMqEvent.None.class;

    public static class None {

    }
}
