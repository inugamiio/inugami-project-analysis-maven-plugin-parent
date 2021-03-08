package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;
@Partner(type = "JMS")
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JmsSender {
    String id() default "";

    String destination();

}
