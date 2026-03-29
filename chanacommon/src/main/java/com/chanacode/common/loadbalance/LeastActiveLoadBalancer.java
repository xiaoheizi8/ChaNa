package com.chanacode.common.loadbalance;

import com.chanacode.common.dto.ServiceInstance;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class LeastActiveLoadBalancer implements LoadBalancer {

    private final Map<String, AtomicLong> activeCounts = new ConcurrentHashMap<>();

    @Override
    public ServiceInstance select(List<ServiceInstance> instances) {
        if (instances == null || instances.isEmpty()) {
            return null;
        }
        
        List<ServiceInstance> healthy = instances.stream()
                .filter(ServiceInstance::isHealthy)
                .filter(ServiceInstance::isEnabled)
                .toList();
        
        if (healthy.isEmpty()) {
            return null;
        }
        
        return healthy.stream()
                .min(Comparator.comparingLong(i -> getActiveCount(i.getInstanceId())))
                .orElse(null);
    }
    
    public void incrementActive(String instanceId) {
        activeCounts.computeIfAbsent(instanceId, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    public void decrementActive(String instanceId) {
        AtomicLong count = activeCounts.get(instanceId);
        if (count != null && count.get() > 0) {
            count.decrementAndGet();
        }
    }
    
    private long getActiveCount(String instanceId) {
        return activeCounts.getOrDefault(instanceId, new AtomicLong(0)).get();
    }

    @Override
    public String getName() {
        return "least-active";
    }
}
