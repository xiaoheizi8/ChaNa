package com.chanacode.common.dto;

import lombok.*;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryStats {
    
    private long totalServices;
    private long totalInstances;
    private long totalConnections;
    private long totalRequests;
    private long totalRegistrations;
    private long totalDiscovers;
    private long qps;
    private Map<String, Long> serviceInstanceCounts;
    private long startTime;
    private long uptime;
}
