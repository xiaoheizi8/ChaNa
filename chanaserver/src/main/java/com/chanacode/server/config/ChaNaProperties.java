package com.chanacode.server.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@ConfigurationProperties(prefix = "chana")
@Validated
public class ChaNaProperties {

    private Netty netty = new Netty();
    private Http http = new Http();
    private HealthCheck healthCheck = new HealthCheck();
    private Security security = new Security();

    @Data
    public static class Netty {
        @Min(value = 1024, message = "端口号不能小于1024")
        @Max(value = 65535, message = "端口号不能大于65535")
        private int port = 9999;
        
        @Min(1)
        @Max(32)
        private int bossThreads = 1;
        
        @Min(1)
        @Max(128)
        private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    }

    @Data
    public static class Http {
        @Min(1024)
        @Max(65535)
        private int port = 9998;
    }

    @Data
    public static class HealthCheck {
        @Min(1)
        @Max(300)
        private int intervalSeconds = 5;
        
        @Min(5)
        @Max(600)
        private int heartbeatTimeoutSeconds = 30;
    }

    @Data
    public static class Security {
        private boolean enabled = true;
        private String token = "";
        private int maxRequestSize = 1024 * 1024; // 1MB
    }
}
