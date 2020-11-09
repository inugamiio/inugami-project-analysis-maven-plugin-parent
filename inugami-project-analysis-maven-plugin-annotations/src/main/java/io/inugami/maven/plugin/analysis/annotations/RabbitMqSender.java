package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Partner(type = "RABBIT_MQ")
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RabbitMqSender {
    String id() default "";

    String echangeName() default "";

    String queue() default "";

    String routingKey() default "";
}
