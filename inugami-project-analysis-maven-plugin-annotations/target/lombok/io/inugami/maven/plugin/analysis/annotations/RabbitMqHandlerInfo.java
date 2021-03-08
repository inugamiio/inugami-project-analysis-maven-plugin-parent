package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitMqHandlerInfo {
    String id() default "";
    String routingKey();
    String typeId() default "";
}
