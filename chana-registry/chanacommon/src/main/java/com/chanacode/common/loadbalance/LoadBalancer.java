package com.chanacode.common.loadbalance;

import com.chanacode.common.dto.ServiceInstance;

import java.util.List;

public interface LoadBalancer {
    
    ServiceInstance select(List<ServiceInstance> instances);
    
    String getName();
}
