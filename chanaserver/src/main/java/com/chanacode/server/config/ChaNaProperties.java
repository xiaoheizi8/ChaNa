package com.chanacode.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "chana.server")
public class ChaNaProperties {
    
    private int port = 9999;
    private int workerThreads = 16;
    private boolean httpEnabled = true;
    private int httpPort = 8080;
    private boolean enableAuth = false;
    private String token = "";
}
