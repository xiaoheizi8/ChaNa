package com.chanacode.spring.boot.starter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用ChaNa客户端
 *
 * <p>在Spring Boot应用启动类上添加此注解即可启用ChaNa服务注册中心客户端功能。
 *
 * <p>功能说明:
 * <ul>
 *   <li>自动配置ChaNaClient</li>
 *   <li>自动注册带有@ChaNaService注解的服务</li>
 *   <li>支持服务发现</li>
 * </ul>
 *
 * <p>使用示例:
 * <pre>
 * {@code @}EnableChaNaClient
 * {@code @}SpringBootApplication
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SpringApplication.run(MyApplication.class, args);
 *     }
 * }
 * </pre>
 *
 * <p>配置示例:
 * <pre>
 * chana:
 *   client:
 *     enabled: true
 *     server-host: localhost
 *     server-port: 9999
 *     namespace: production
 * </pre>
 *
 * @author ChaNa Team
 * @version 3.0.0
 * @since 3.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ChaNaClientAutoConfiguration.class)
public @interface EnableChaNaClient {
}
