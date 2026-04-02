package com.chanacode.server.http;

import com.chanacode.common.dto.RegistryStats;
import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.core.registry.ServiceRegistry;
import com.chanacode.core.config.ConfigManager;
import com.chanacode.core.namespace.NamespaceManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

public class HttpApiHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(HttpApiHandler.class);
    private final ServiceRegistry serviceRegistry;

    public HttpApiHandler(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        String response;
        try {
            if (path.equals("/services")) {
                response = handleGetServices();
            } else if (path.startsWith("/service/")) {
                String serviceName = path.substring("/service/".length());
                response = handleGetService(serviceName);
            } else if (path.equals("/stats")) {
                response = handleGetStats();
            } else if (path.equals("/namespaces")) {
                response = handleGetNamespaces();
            } else if (path.equals("/configs")) {
                response = handleGetConfigs();
            } else {
                response = "{\"error\":\"Not found\"}";
            }
        } catch (Exception e) {
            logger.error("Error handling request: {}", path, e);
            response = "{\"error\":\"" + e.getMessage() + "\"}";
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private String handleGetServices() {
        Set<String> services = serviceRegistry.getAllServices();
        return "{\"services\":" + toJson(services) + "}";
    }

    private String handleGetService(String serviceName) {
        List<ServiceInstance> instances = serviceRegistry.discover(serviceName, "public");
        return "{\"serviceName\":\"" + serviceName + "\",\"instances\":" + toJson(instances) + "}";
    }

    private String handleGetStats() {
        RegistryStats stats = RegistryStats.builder()
            .totalServices(serviceRegistry.getServiceCount())
            .totalInstances(serviceRegistry.getTotalInstances())
            .totalRegistrations(serviceRegistry.getTotalRegistrations())
            .totalDiscovers(serviceRegistry.getTotalDiscovers())
            .startTime(System.currentTimeMillis())
            .uptime(System.currentTimeMillis())
            .build();
        return toJson(stats);
    }

    private String handleGetNamespaces() {
        Set<String> namespaces = NamespaceManager.getInstance().getAllNamespaces();
        return "{\"namespaces\":" + toJson(namespaces) + "}";
    }

    private String handleGetConfigs() {
        Map<String, Object> configs = ConfigManager.getInstance().getAllConfigs();
        return toJson(configs);
    }

    private String toJson(Object obj) {
        return com.alibaba.fastjson.JSON.toJSONString(obj);
    }
}
