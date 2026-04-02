package com.chanacode.common.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryRequest {
    
    private long requestId;
    private String type;
    private String serviceName;
    private String namespace;
    private ServiceInstance instance;
    private String instanceId;
    private Map<String, String> metadata;
    private List<String> serviceNames;
    private Integer ttlSeconds;
    private Map<String, Object> ext;

    public static RegistryRequest register(ServiceInstance instance) {
        RegistryRequest req = new RegistryRequest();
        req.setType("register");
        req.setInstance(instance);
        req.setServiceName(instance.getServiceName());
        req.setNamespace(instance.getNamespace());
        return req;
    }

    public static RegistryRequest deregister(ServiceInstance instance, String serviceName, String namespace) {
        RegistryRequest req = new RegistryRequest();
        req.setType("deregister");
        req.setInstance(instance);
        req.setServiceName(serviceName);
        req.setNamespace(namespace);
        return req;
    }

    public static RegistryRequest discover(String serviceName, String namespace) {
        RegistryRequest req = new RegistryRequest();
        req.setType("discover");
        req.setServiceName(serviceName);
        req.setNamespace(namespace);
        return req;
    }

    public static RegistryRequest heartbeat(String instanceId, String serviceName, String namespace) {
        RegistryRequest req = new RegistryRequest();
        req.setType("heartbeat");
        req.setInstanceId(instanceId);
        req.setServiceName(serviceName);
        req.setNamespace(namespace);
        return req;
    }

    public static RegistryRequest batchRegister(List<ServiceInstance> instances) {
        RegistryRequest req = new RegistryRequest();
        req.setType("batchRegister");
        req.setExt(Map.of("instances", instances));
        return req;
    }

    public static RegistryRequest batchDiscover(List<String> serviceNames, String namespace) {
        RegistryRequest req = new RegistryRequest();
        req.setType("batchDiscover");
        req.setServiceNames(serviceNames);
        req.setNamespace(namespace);
        return req;
    }

    public static RegistryRequest metadataUpdate(String instanceId, Map<String, String> metadata) {
        RegistryRequest req = new RegistryRequest();
        req.setType("metadataUpdate");
        req.setInstanceId(instanceId);
        req.setMetadata(metadata);
        return req;
    }

    public static RegistryRequest leaseRenew(String instanceId, int ttlSeconds) {
        RegistryRequest req = new RegistryRequest();
        req.setType("leaseRenew");
        req.setInstanceId(instanceId);
        req.setTtlSeconds(ttlSeconds);
        return req;
    }
}
