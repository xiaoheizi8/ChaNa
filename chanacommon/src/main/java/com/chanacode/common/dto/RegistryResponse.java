package com.chanacode.common.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryResponse {
    
    private long requestId;
    private boolean success;
    private String message;
    private List<ServiceInstance> instances;
    private Map<String, List<ServiceInstance>> batchInstances;
    private Map<String, Object> ext;
}
