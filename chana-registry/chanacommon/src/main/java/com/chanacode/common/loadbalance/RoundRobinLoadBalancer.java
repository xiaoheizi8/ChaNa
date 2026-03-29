package com.chanacode.common.loadbalance;

import com.chanacode.common.dto.ServiceInstance;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class RoundRobinLoadBalancer implements LoadBalancer {

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
        
        int index = (int) (sequence.getAndIncrement() % healthy.size());
        return healthy.get(index);
    }

    @Override
    public String getName() {
        return "round-robin";
    }
}
