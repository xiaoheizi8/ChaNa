package com.chanacode.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ChaNa注册中心 Spring Boot 启动类
 *
 * <p>集成 Spring Boot 3.2 + Undertow + JDK 21 虚拟线程，
 * 提供高性能 RESTful API 接口。
 *
 * <p>特性：
 * <ul>
 *   <li>虚拟线程支持 - 利用 JDK 21 Project Loom</li>
 *   <li>Undertow 容器 - 高性能 HTTP 服务器</li>
 *   <li>Spring MVC 注解驱动 - 标准 REST 接口</li>
 *   <li>Actuator 健康检查 - 运维监控</li>
 * </ul>
 *
 * @author 一朝风月
 * @version 1.0.0
 * @since 2026-03-27
 */
@SpringBootApplication
@EnableScheduling
public class ChaNaSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChaNaSpringApplication.class, args);
    }
}
