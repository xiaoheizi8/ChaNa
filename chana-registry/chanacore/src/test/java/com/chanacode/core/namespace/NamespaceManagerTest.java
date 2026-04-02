package com.chanacode.core.namespace;

import com.chanacode.common.constant.RegistryConstants;
import com.chanacode.common.dto.ServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NamespaceManagerTest {

    private NamespaceManager namespaceManager;

    @BeforeEach
    void setUp() {
        namespaceManager = new NamespaceManager();
    }

    private ServiceInstance createInstance(String serviceName, String instanceId, String group) {
        ServiceInstance instance = new ServiceInstance();
        instance.setServiceName(serviceName);
        instance.setInstanceId(instanceId);
        instance.setHost("localhost");
        instance.setPort(8080);
        instance.setGroup(group);
        instance.setHealthy(true);
        return instance;
    }

    @Test
    void testCreateNamespace() {
        boolean result = namespaceManager.createNamespace("dev", "Development environment");
        assertTrue(result);
        
        NamespaceManager.Namespace ns = namespaceManager.getNamespace("dev");
        assertEquals("dev", ns.getName());
    }

    @Test
    void testCreateDuplicateNamespace() {
        namespaceManager.createNamespace("dev", "Dev");
        boolean result = namespaceManager.createNamespace("dev", "Dev again");
        assertFalse(result);
    }

    @Test
    void testDeleteNamespace() {
        namespaceManager.createNamespace("dev", "Dev");
        boolean result = namespaceManager.deleteNamespace("dev");
        assertTrue(result);
    }

    @Test
    void testDeleteDefaultNamespace() {
        boolean result = namespaceManager.deleteNamespace(RegistryConstants.DEFAULT_NAMESPACE);
        assertFalse(result);
    }

    @Test
    void testGetAllNamespaces() {
        namespaceManager.createNamespace("dev", "Dev");
        namespaceManager.createNamespace("prod", "Prod");
        
        List<NamespaceManager.NamespaceInfo> all = namespaceManager.getAllNamespaces();
        assertTrue(all.size() >= 3);
    }

    @Test
    void testGetNamespaceNotFound() {
        NamespaceManager.Namespace ns = namespaceManager.getNamespace("non-existent");
        assertEquals(RegistryConstants.DEFAULT_NAMESPACE, ns.getName());
    }

    @Test
    void testRegisterService() {
        ServiceInstance instance = createInstance("test-service", "i1", "DEFAULT_GROUP");
        boolean result = namespaceManager.registerService("default", instance);
        assertTrue(result);
    }

    @Test
    void testDeregisterService() {
        ServiceInstance instance = createInstance("test-service", "i1", "DEFAULT_GROUP");
        namespaceManager.registerService("default", instance);
        
        boolean result = namespaceManager.deregisterService("default", "i1", "test-service");
        assertTrue(result);
    }

    @Test
    void testDiscoverServices() {
        ServiceInstance i1 = createInstance("test-service", "i1", "DEFAULT_GROUP");
        ServiceInstance i2 = createInstance("test-service", "i2", "DEFAULT_GROUP");
        
        namespaceManager.registerService("default", i1);
        namespaceManager.registerService("default", i2);
        
        List<ServiceInstance> result = namespaceManager.discoverServices("default", "test-service", null, null);
        assertEquals(2, result.size());
    }

    @Test
    void testDiscoverServicesByGroup() {
        ServiceInstance i1 = createInstance("test-service", "i1", "GROUP_A");
        ServiceInstance i2 = createInstance("test-service", "i2", "GROUP_B");
        
        namespaceManager.registerService("default", i1);
        namespaceManager.registerService("default", i2);
        
        List<ServiceInstance> result = namespaceManager.discoverServices("default", "test-service", "GROUP_A", null);
        assertEquals(1, result.size());
        assertEquals("i1", result.get(0).getInstanceId());
    }

    @Test
    void testGetVersion() {
        ServiceInstance instance = createInstance("test-service", "i1", "DEFAULT_GROUP");
        namespaceManager.registerService("default", instance);
        
        long version = namespaceManager.getVersion("default");
        assertTrue(version > 0);
    }

    @Test
    void testNamespaceServiceCount() {
        ServiceInstance i1 = createInstance("service-a", "i1", "DEFAULT_GROUP");
        ServiceInstance i2 = createInstance("service-b", "i2", "DEFAULT_GROUP");
        
        namespaceManager.registerService("default", i1);
        namespaceManager.registerService("default", i2);
        
        NamespaceManager.Namespace ns = namespaceManager.getNamespace("default");
        assertEquals(2, ns.getServiceCount());
    }

    @Test
    void testNamespaceInstanceCount() {
        ServiceInstance i1 = createInstance("test-service", "i1", "DEFAULT_GROUP");
        ServiceInstance i2 = createInstance("test-service", "i2", "DEFAULT_GROUP");
        
        namespaceManager.registerService("default", i1);
        namespaceManager.registerService("default", i2);
        
        NamespaceManager.Namespace ns = namespaceManager.getNamespace("default");
        assertEquals(2, ns.getInstanceCount());
    }
}
