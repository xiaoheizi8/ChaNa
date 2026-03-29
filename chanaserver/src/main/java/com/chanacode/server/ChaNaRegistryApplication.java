package com.chanacode.server;

import com.chanacode.server.config.ChaNaBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableChaNaServer
public class ChaNaRegistryApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ChaNaRegistryApplication.class);
        application.setBanner(new ChaNaBanner());
        application.run(args);
    }
}
