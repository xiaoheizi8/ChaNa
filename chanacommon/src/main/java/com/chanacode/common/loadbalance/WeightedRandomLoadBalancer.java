package com.chanacode.common.loadbalance;

import com.chanacode.common.dto.ServiceInstance;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class WeightedRandomLoadBalancer implements LoadBalancer {

    private final AtomicLong sequence = new AtomicLong(0);

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
        
        int totalWeight = healthy.stream()
                .mapToInt(ServiceInstance::getWeight)
                .sum();
        
        if (totalWeight <= 0) {
            return healthy.get((int) (System.currentTimeMillis() % healthy.size()));
        }
        
        int random = (int) (Math.random() * totalWeight);
        int current = 0;
        
        for (ServiceInstance instance : healthy) {
            current += instance.getWeight();
            if (random < current) {
                return instance;
            }
        }
        
        return healthy.get(healthy.size() - 1);
    }

    @Override
    public String getName() {
        return "weighted-random";
    }
}
