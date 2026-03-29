package com.chanacode.server.config;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringBootVersion;
import org.springframework.core.env.Environment;
import org.springframework.core.env.EnvironmentCapable;

import java.io.PrintStream;

public class ChaNaBanner implements Banner {

    private static final String BANNER = """
            
            \u001b[1;32m╔══════════════════════════════════════════════════════════════════╗
            ║                                                                  ║
            ║   \u001b[1;33m   _ __                          _   __  __   ___  _   __    \u001b[1;32m  ║
            ║   \u001b[1;33m  / / /___  ____  _      __      / | / /  |/  / / /| | / /_   \u001b[1;32m  ║
            ║   \u001b[1;33m / / / __ \\/ __ \\| | /| / /____/  |/ /  |/  / / __/ __/ / __ \\  \u001b[1;32m  ║
            ║   \u001b[1;33m/ / / /_/ / /_/ / |/ |/ /_____/ /|  /  |  / / /_ / /_/ / /_/ /  \u001b[1;32m  ║
            ║   \u001b[1;33m/_/  \\____/\\____/|__/|__/     /_/ |_/   |___/ \\__/\\____/\\____/   \u001b[1;32m  ║
            ║                                                                  ║
            ║   \u001b[1;36m:: ChaNa Registry ::\u001b[0m          \u001b[1;32mUltra High Performance Service Registry\u001b[0m  ║
            ║                                                                  ║
            ╚══════════════════════════════════════════════════════════════════╝
            \u001b[0m
            """;

    private static final String VERSION_INFO = """
            
            \u001b[1;37m  Version:\u001b[0m       \u001b[1;32m%s\u001b[0m
            \u001b[1;37m  Spring Boot:\u001b[0m   \u001b[1;34m%s\u001b[0m
            \u001b[1;37m  JDK:\u001b[0m          \u001b[1;35m%s\u001b[0m
            \u001b[1;37m  Author:\u001b[0m       \u001b[1;33mChaNa Team\u001b[0m
            """;

    private static final String FEATURES = """
            
            \u001b[1;32m  ★ Write QPS:\u001b[0m     \u001b[1;32m50,000+\u001b[0m/s
            \u001b[1;32m  ★ Read QPS:\u001b[0m      \u001b[1;32m100,000+\u001b[0m/s
            \u001b[1;32m  ★ P99 Latency:\u001b[0m   \u001b[1;32m< 1ms\u001b[0m
            \u001b[1;32m  ★ Max Connections:\u001b[0m \u001b[1;32m50,000+\u001b[0m
            """;

    private static final String STARTING = """
            
            \u001b[1;33m  ⚡ Starting ChaNa Registry Server...\u001b[0m
            """;

    @Override
    public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {
        String version = environment.getProperty("application.version", "3.0.0");
        String springBootVersion = SpringBootVersion.getVersion();
        String javaVersion = System.getProperty("java.version", "17+");

        out.println(BANNER);
        out.println(String.format(VERSION_INFO, version, springBootVersion, javaVersion));
        out.println(FEATURES);
        out.println(STARTING);
        out.println();
    }
}
