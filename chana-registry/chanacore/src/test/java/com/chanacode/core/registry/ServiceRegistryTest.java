package com.chanacode.core.registry;

import com.chanacode.common.dto.ServiceInstance;
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
    void testRegister() {
        ServiceInstance instance = createInstance("test-service", "instance-1");
        boolean result = registry.register(instance);
        assertTrue(result);
        assertEquals(1, registry.getTotalInstances());
    }

    @Test
    void testRegisterDuplicate() {
        ServiceInstance instance1 = createInstance("test-service", "instance-1");
        ServiceInstance instance2 = createInstance("test-service", "instance-1");
        
        registry.register(instance1);
        registry.register(instance2);
        
        assertEquals(1, registry.getTotalInstances());
    }

    @Test
    void testDeregister() {
        ServiceInstance instance = createInstance("test-service", "instance-1");
        registry.register(instance);
        
        boolean result = registry.deregister("instance-1", "test-service", "default");
        assertTrue(result);
        assertEquals(0, registry.getTotalInstances());
    }

    @Test
    void testDeregisterNotFound() {
        boolean result = registry.deregister("non-existent", "test-service", "default");
        assertFalse(result);
    }

    @Test
    void testDiscover() {
        registry.register(createInstance("test-service", "instance-1"));
        registry.register(createInstance("test-service", "instance-2"));
        
        List<ServiceInstance> instances = registry.discover("test-service", "default");
        assertEquals(2, instances.size());
    }

    @Test
    void testDiscoverNotFound() {
        List<ServiceInstance> instances = registry.discover("non-existent", "default");
        assertTrue(instances.isEmpty());
    }

    @Test
    void testDiscoverHealthy() {
        ServiceInstance healthy = createInstance("test-service", "instance-1");
        healthy.setHealthy(true);
        
        ServiceInstance unhealthy = createInstance("test-service", "instance-2");
        unhealthy.setHealthy(false);
        
        registry.register(healthy);
        registry.register(unhealthy);
        
        List<ServiceInstance> instances = registry.discoverHealthy("test-service", "default");
        assertEquals(1, instances.size());
    }

    @Test
    void testHeartbeat() {
        ServiceInstance instance = createInstance("test-service", "instance-1");
        registry.register(instance);
        
        boolean result = registry.heartbeat("instance-1");
        assertTrue(result);
    }

    @Test
    void testHeartbeatNotFound() {
        boolean result = registry.heartbeat("non-existent");
        assertFalse(result);
    }

    @Test
    void testMarkUnhealthy() {
        ServiceInstance instance = createInstance("test-service", "instance-1");
        instance.setHealthy(true);
        registry.register(instance);
        
        registry.markUnhealthy("instance-1");
        
        ServiceInstance result = registry.getInstance("instance-1");
        assertFalse(result.isHealthy());
    }

    @Test
    void testGetAllServices() {
        registry.register(createInstance("service-a", "i1"));
        registry.register(createInstance("service-b", "i2"));
        
        var services = registry.getAllServices();
        assertEquals(2, services.size());
    }

    @Test
    void testGetServiceCount() {
        registry.register(createInstance("service-a", "i1"));
        registry.register(createInstance("service-b", "i2"));
        
        assertEquals(2, registry.getServiceCount());
    }

    @Test
    void testGetVersion() {
        ServiceInstance instance = createInstance("test-service", "instance-1");
        registry.register(instance);
        
        long version = registry.getVersion("test-service", "default");
        assertTrue(version > 0);
    }

    @Test
    void testGetTotalRegistrations() {
        registry.register(createInstance("test-service", "instance-1"));
        registry.register(createInstance("test-service", "instance-2"));
        
        assertEquals(2, registry.getTotalRegistrations());
    }

    @Test
    void testGetTotalDiscovers() {
        registry.register(createInstance("test-service", "instance-1"));
        
        registry.discover("test-service", "default");
        registry.discover("test-service", "default");
        
        assertEquals(2, registry.getTotalDiscovers());
    }

    @Test
    void testNamespaceIsolation() {
        ServiceInstance instance1 = createInstance("test-service", "instance-1");
        instance1.setNamespace("ns1");
        
        ServiceInstance instance2 = createInstance("test-service", "instance-2");
        instance2.setNamespace("ns2");
        
        registry.register(instance1);
        registry.register(instance2);
        
        List<ServiceInstance> ns1Instances = registry.discover("test-service", "ns1");
        List<ServiceInstance> ns2Instances = registry.discover("test-service", "ns2");
        
        assertEquals(1, ns1Instances.size());
        assertEquals(1, ns2Instances.size());
    }

    @Test
    void testRegisterWithNullNamespace() {
        ServiceInstance instance = createInstance("test-service", "instance-1");
        instance.setNamespace(null);
        
        registry.register(instance);
        
        List<ServiceInstance> instances = registry.discover("test-service", "default");
        assertEquals(1, instances.size());
    }
}
