package com.chanacode.core.registry;

import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.common.constant.RegistryConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceRegistryTest {

    private ServiceRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ServiceRegistry();
    }

    @Test
    void testRegister() {
        ServiceInstance instance = createInstance("test-service-1");
        boolean result = registry.register(instance);
        assertTrue(result);
    }

    @Test
    void testRegisterDuplicate() {
        ServiceInstance instance = createInstance("test-service-1");
        registry.register(instance);
        boolean result = registry.register(instance);
        assertTrue(result);
        assertEquals(1, registry.getTotalInstances());
    }

    @Test
    void testRegisterNull() {
        boolean result = registry.register(null);
        assertFalse(result);
    }

    @Test
    void testDeregister() {
        ServiceInstance instance = createInstance("test-service-1");
        registry.register(instance);
        boolean result = registry.deregister(instance.getInstanceId(), instance.getServiceName(), "public");
        assertTrue(result);
    }

    @Test
    void testDeregisterNotExists() {
        boolean result = registry.deregister("not-exists", "test-service", "public");
        assertFalse(result);
    }

    @Test
    void testDiscover() {
        ServiceInstance instance1 = createInstance("test-service");
        ServiceInstance instance2 = createInstance("test-service");
        instance2.setPort(8081);
        registry.register(instance1);
        registry.register(instance2);

        List<ServiceInstance> instances = registry.discover("test-service", "public");
        assertEquals(2, instances.size());
    }

    @Test
    void testDiscoverNotExists() {
        List<ServiceInstance> instances = registry.discover("not-exists", "public");
        assertTrue(instances.isEmpty());
    }

    @Test
    void testDiscoverHealthy() {
        ServiceInstance instance1 = createInstance("test-service");
        instance1.setHealthy(false);
        ServiceInstance instance2 = createInstance("test-service");
        instance2.setPort(8081);
        registry.register(instance1);
        registry.register(instance2);

        List<ServiceInstance> instances = registry.discoverHealthy("test-service", "public");
        assertEquals(1, instances.size());
    }

    @Test
    void testHeartbeat() {
        ServiceInstance instance = createInstance("test-service");
        registry.register(instance);
        
        boolean result = registry.heartbeat(instance.getInstanceId());
        assertTrue(result);
    }

    @Test
    void testHeartbeatNotExists() {
        boolean result = registry.heartbeat("not-exists");
        assertFalse(result);
    }

    @Test
    void testMarkUnhealthy() {
        ServiceInstance instance = createInstance("test-service");
        registry.register(instance);
        
        boolean result = registry.markUnhealthy(instance.getInstanceId());
        assertTrue(result);
        assertFalse(instance.isHealthy());
    }

    @Test
    void testGetAllServices() {
        registry.register(createInstance("service-1"));
        registry.register(createInstance("service-2"));
        
        assertEquals(2, registry.getServiceCount());
    }

    private ServiceInstance createInstance(String serviceName) {
        return ServiceInstance.builder()
            .instanceId(UUID.randomUUID().toString())
            .serviceName(serviceName)
            .host("localhost")
            .port(8080)
            .namespace(RegistryConstants.DEFAULT_NAMESPACE)
            .healthy(true)
            .enabled(true)
            .weight(100)
            .registrationTime(System.currentTimeMillis())
            .build();
    }
}
