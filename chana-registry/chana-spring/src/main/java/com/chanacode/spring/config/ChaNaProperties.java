package com.chanacode.spring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chana")
public class ChaNaProperties {

    private RegistryProperties registry = new RegistryProperties();
    private CacheProperties cache = new CacheProperties();
    private HealthProperties health = new HealthProperties();
    private NamespaceProperties namespace = new NamespaceProperties();

    @Data
    public static class RegistryProperties {
        private String host = "localhost";
        private int port = 9999;
        private int httpPort = 9998;
        private int connectTimeout = 5000;
        private int readTimeout = 10000;
        private int heartbeatTimeout = 30000;
        private int heartbeatInterval = 5000;
        private int maxWeight = 100;
        private int minWeight = 1;
        private boolean autoRemoveUnhealthy = true;
        private double protectionThreshold = 0.2;
    }

    @Data
    public static class CacheProperties {
        private int l1Ttl = 5;
        private int l2Ttl = 30;
    }

    @Data
    public static class HealthProperties {
        private int windowSize = 10;
        private int failureThreshold = 3;
    }

    @Data
    public static class NamespaceProperties {
        private String defaultNamespace = "default";
    }
}
