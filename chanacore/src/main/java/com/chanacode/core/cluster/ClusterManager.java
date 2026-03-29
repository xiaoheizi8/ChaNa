package com.chanacode.core.cluster;

import com.chanacode.common.dto.ServiceInstance;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ClusterManager {

    private final String nodeId;
    private final String address;
    private final Map<String, ClusterNode> nodes;
    private final ConcurrentHashMap<String, ServiceInstance> localRegistry;
    private final BlockingQueue<ClusterMessage> messageQueue;
    private final ExecutorService executor;
    private final ScheduledExecutorService scheduler;
    
    private volatile ClusterNode.Role role;
    private volatile boolean running;

    public ClusterManager(String nodeId, String address) {
        this.nodeId = nodeId;
        this.address = address;
        this.nodes = new ConcurrentHashMap<>();
        this.localRegistry = new ConcurrentHashMap<>();
        this.messageQueue = new LinkedBlockingQueue<>();
        this.executor = Executors.newCachedThreadPool();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.role = ClusterNode.Role.FOLLOWER;
        this.running = false;
    }

    public void start() {
        running = true;
        executor.submit(this::processMessages);
        scheduler.scheduleAtFixedRate(this::syncWithCluster, 5, 5, TimeUnit.SECONDS);
        log.info("ClusterManager started for node: {}", nodeId);
    }

    public void stop() {
        running = false;
        executor.shutdown();
        scheduler.shutdown();
        log.info("ClusterManager stopped for node: {}", nodeId);
    }

    public void addNode(String nodeId, String address) {
        ClusterNode node = new ClusterNode(nodeId, address);
        nodes.put(nodeId, node);
        log.info("Added cluster node: {} at {}", nodeId, address);
    }

    public void removeNode(String nodeId) {
        nodes.remove(nodeId);
        log.info("Removed cluster node: {}", nodeId);
    }

    public void registerLocal(ServiceInstance instance) {
        localRegistry.put(instance.getInstanceId(), instance);
        broadcast(new ClusterMessage(ClusterMessage.Type.REGISTER, instance));
    }

    public void deregisterLocal(String instanceId) {
        localRegistry.remove(instanceId);
        broadcast(new ClusterMessage(ClusterMessage.Type.DEREGISTER, instanceId));
    }

    public void heartbeat(String instanceId) {
        ServiceInstance instance = localRegistry.get(instanceId);
        if (instance != null) {
            instance.updateHeartbeat(System.currentTimeMillis());
        }
    }

    private void syncWithCluster() {
        for (ClusterNode node : nodes.values()) {
            if (!node.isReachable()) continue;
            
            try {
                List<ServiceInstance> remoteInstances = node.fetchInstances();
                mergeInstances(remoteInstances);
            } catch (Exception e) {
                log.warn("Failed to sync with node {}: {}", node.getNodeId(), e.getMessage());
            }
        }
    }

    private void mergeInstances(List<ServiceInstance> remoteInstances) {
        for (ServiceInstance instance : remoteInstances) {
            ServiceInstance local = localRegistry.get(instance.getInstanceId());
            if (local == null || instance.getLastHeartbeatTime() > local.getLastHeartbeatTime()) {
                localRegistry.put(instance.getInstanceId(), instance);
            }
        }
    }

    private void broadcast(ClusterMessage message) {
        for (ClusterNode node : nodes.values()) {
            if (node.isReachable()) {
                executor.submit(() -> node.send(message));
            }
        }
    }

    private void processMessages() {
        while (running) {
            try {
                ClusterMessage message = messageQueue.poll(100, TimeUnit.MILLISECONDS);
                if (message != null) {
                    handleMessage(message);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void handleMessage(ClusterMessage message) {
        switch (message.getType()) {
            case REGISTER -> {
                ServiceInstance instance = (ServiceInstance) message.getData();
                localRegistry.put(instance.getInstanceId(), instance);
            }
            case DEREGISTER -> localRegistry.remove(message.getData());
            case SYNC -> mergeInstances((List<ServiceInstance>) message.getData());
        }
    }

    public List<ServiceInstance> getAllInstances() {
        return new ArrayList<>(localRegistry.values());
    }

    public Set<String> getClusterNodes() {
        return nodes.keySet();
    }

    public boolean isLeader() {
        return role == ClusterNode.Role.LEADER;
    }

    public static class ClusterNode {
        private final String nodeId;
        private final String address;
        private volatile boolean reachable;
        private volatile long lastHeartbeat;
        private final AtomicInteger failCount;

        public enum Role { LEADER, FOLLOWER, CANDIDATE }

        public ClusterNode(String nodeId, String address) {
            this.nodeId = nodeId;
            this.address = address;
            this.reachable = true;
            this.lastHeartbeat = System.currentTimeMillis();
            this.failCount = new AtomicInteger(0);
        }

        public boolean isReachable() {
            return reachable && failCount.get() < 3;
        }

        public List<ServiceInstance> fetchInstances() {
            return List.of();
        }

        public void send(ClusterMessage message) {}

        public String getNodeId() { return nodeId; }
        public String getAddress() { return address; }
    }

    public static class ClusterMessage {
        private final Type type;
        private final Object data;
        private final long timestamp;

        public enum Type { REGISTER, DEREGISTER, SYNC, HEARTBEAT }

        public ClusterMessage(Type type, Object data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }

        public Type getType() { return type; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
}
