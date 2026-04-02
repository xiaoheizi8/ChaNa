package com.chanacode.api.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ChaNaService {
    String serviceName() default "";
    String version() default "1.0.0";
    String group() default "default";
    String namespace() default "public";
    int weight() default 100;
    boolean healthy() default true;
}
