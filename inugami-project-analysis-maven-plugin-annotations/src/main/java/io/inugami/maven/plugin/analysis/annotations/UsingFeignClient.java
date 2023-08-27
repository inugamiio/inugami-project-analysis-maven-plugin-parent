package io.inugami.maven.plugin.analysis.annotations;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UsingFeignClient {
    Class<?> feignConfigurationBean() default UsingFeignClient.None.class;

    @SuppressWarnings({"java:S2094"})
    public static class None {
    }
}
