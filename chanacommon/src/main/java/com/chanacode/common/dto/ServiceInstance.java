package com.chanacode.common.dto;

import lombok.*;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "instanceId")
public class ServiceInstance {
    
    private String instanceId;
    private String serviceName;
    private String host;
    private int port;
    private String version;
    private String group;
    private String namespace;
    private Map<String, String> metadata;
    private long registrationTime;
    private long lastHeartbeatTime;
    private int weight;
    private volatile boolean healthy;
    private volatile boolean enabled;
    private int cpuCores;
    private int memoryMb;
    private CopyOnWriteArrayList<String> tags;

    public boolean isExpired(long currentTime) {
        return (currentTime - lastHeartbeatTime) > 15_000;
    }

    public void updateHeartbeat(long currentTime) {
        this.lastHeartbeatTime = currentTime;
        this.healthy = true;
    }

    public String getAddress() {
        return host + ":" + port;
    }
}
