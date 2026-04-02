package com.chanacode.api.client;

import com.chanacode.common.dto.ServiceInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChaNaClientTest {

    private ChaNaClient client;

    @BeforeEach
    void setUp() {
        client = new ChaNaClient("localhost", 9999);
    }

    @AfterEach
    void tearDown() {
        try {
            client.close();
        } catch (Exception e) {
            // ignore
        }
    }

    private ServiceInstance createInstance(String serviceName, String instanceId) {
        ServiceInstance instance = new ServiceInstance();
        instance.setServiceName(serviceName);
        instance.setInstanceId(instanceId);
        instance.setHost("localhost");
        instance.setPort(8080);
        instance.setHealthy(true);
        return instance;
    }

    @Test
    void testConstructor() {
        assertNotNull(client);
    }

    @Test
    void testIsConnectedBeforeConnect() {
        assertFalse(client.isConnected());
    }

    @Test
    void testCreateClientWithDifferentPort() {
        ChaNaClient client2 = new ChaNaClient("127.0.0.1", 8888);
        assertFalse(client2.isConnected());
        client2.close();
    }

    @Test
    void testCreateClientWithDifferentHost() {
        ChaNaClient client2 = new ChaNaClient("registry.example.com", 9999);
        assertFalse(client2.isConnected());
        client2.close();
    }

    @Test
    void testMultipleClientsCreation() {
        ChaNaClient client1 = new ChaNaClient("localhost", 9999);
        ChaNaClient client2 = new ChaNaClient("localhost", 9999);
        
        assertNotNull(client1);
        assertNotNull(client2);
        
        client1.close();
        client2.close();
    }
}
