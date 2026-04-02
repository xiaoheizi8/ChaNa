package com.chanacode.common.loadbalance;

import com.chanacode.common.dto.ServiceInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadBalancerTest {

    private List<ServiceInstance> instances;

    @BeforeEach
    void setUp() {
        instances = new ArrayList<>();
        
        ServiceInstance i1 = createInstance("i1", 100);
        i1.setHealthy(true);
        i1.setEnabled(true);
        instances.add(i1);
        
        ServiceInstance i2 = createInstance("i2", 200);
        i2.setHealthy(true);
        i2.setEnabled(true);
        instances.add(i2);
        
        ServiceInstance i3 = createInstance("i3", 300);
        i3.setHealthy(true);
        i3.setEnabled(true);
        instances.add(i3);
    }

    private ServiceInstance createInstance(String instanceId, int weight) {
        ServiceInstance instance = new ServiceInstance();
        instance.setInstanceId(instanceId);
        instance.setServiceName("test-service");
        instance.setHost("localhost");
        instance.setPort(8080);
        instance.setWeight(weight);
        return instance;
    }

    @Test
    void testRoundRobinSelect() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        
        ServiceInstance first = balancer.select(instances);
        assertNotNull(first);
        
        ServiceInstance second = balancer.select(instances);
        assertNotNull(second);
        
        ServiceInstance third = balancer.select(instances);
        assertNotNull(third);
    }

    @Test
    void testRoundRobinEmptyList() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        ServiceInstance result = balancer.select(List.of());
        assertNull(result);
    }

    @Test
    void testRoundRobinNullList() {
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        ServiceInstance result = balancer.select(null);
        assertNull(result);
    }

    @Test
    void testRoundRobinUnhealthy() {
        instances.get(0).setHealthy(false);
        instances.get(1).setHealthy(false);
        
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        ServiceInstance result = balancer.select(instances);
        assertNotNull(result);
        assertEquals(instances.get(2), result);
    }

    @Test
    void testRoundRobinDisabled() {
        instances.get(0).setEnabled(false);
        instances.get(1).setEnabled(false);
        
        RoundRobinLoadBalancer balancer = new RoundRobinLoadBalancer();
        ServiceInstance result = balancer.select(instances);
        assertNotNull(result);
        assertEquals(instances.get(2), result);
    }

    @Test
    void testWeightedRandomSelect() {
        WeightedRandomLoadBalancer balancer = new WeightedRandomLoadBalancer();
        
        ServiceInstance result = balancer.select(instances);
        assertNotNull(result);
    }

    @Test
    void testWeightedRandomEmptyList() {
        WeightedRandomLoadBalancer balancer = new WeightedRandomLoadBalancer();
        ServiceInstance result = balancer.select(List.of());
        assertNull(result);
    }

    @Test
    void testWeightedRandomZeroWeight() {
        instances.get(0).setWeight(0);
        instances.get(1).setWeight(0);
        instances.get(2).setWeight(0);
        
        WeightedRandomLoadBalancer balancer = new WeightedRandomLoadBalancer();
        ServiceInstance result = balancer.select(instances);
        assertNotNull(result);
    }

    @Test
    void testLeastActiveSelect() {
        LeastActiveLoadBalancer balancer = new LeastActiveLoadBalancer();
        
        ServiceInstance result = balancer.select(instances);
        assertNotNull(result);
    }

    @Test
    void testLeastActiveEmptyList() {
        LeastActiveLoadBalancer balancer = new LeastActiveLoadBalancer();
        ServiceInstance result = balancer.select(List.of());
        assertNull(result);
    }

    @Test
    void testLeastActiveIncrementDecrement() {
        LeastActiveLoadBalancer balancer = new LeastActiveLoadBalancer();
        
        balancer.incrementActive("i1");
        balancer.incrementActive("i1");
        balancer.incrementActive("i2");
        
        ServiceInstance selected = balancer.select(instances);
        assertEquals("i3", selected.getInstanceId());
    }

    @Test
    void testLeastActiveDecrement() {
        LeastActiveLoadBalancer balancer = new LeastActiveLoadBalancer();
        
        balancer.incrementActive("i1");
        balancer.incrementActive("i2");
        balancer.decrementActive("i1");
        
        ServiceInstance selected = balancer.select(instances);
        assertEquals("i1", selected.getInstanceId());
    }

    @Test
    void testLoadBalancerNames() {
        RoundRobinLoadBalancer rr = new RoundRobinLoadBalancer();
        assertEquals("round-robin", rr.getName());

        WeightedRandomLoadBalancer wr = new WeightedRandomLoadBalancer();
        assertEquals("weighted-random", wr.getName());

        LeastActiveLoadBalancer la = new LeastActiveLoadBalancer();
        assertEquals("least-active", la.getName());
    }
}
