package com.chanacode.server;

import com.chanacode.server.config.ChaNaServerAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ChaNaServerAutoConfiguration.class)
public @interface EnableChaNaServer {
}
