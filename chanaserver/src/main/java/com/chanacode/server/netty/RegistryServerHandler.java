package com.chanacode.server.netty;

import com.chanacode.common.dto.RegistryRequest;
import com.chanacode.common.dto.RegistryResponse;
import com.chanacode.common.dto.ServiceInstance;
import com.chanacode.core.registry.ServiceRegistry;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSON;

public class RegistryServerHandler extends SimpleChannelInboundHandler<RegistryRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RegistryServerHandler.class);
    private final ServiceRegistry serviceRegistry;

    public RegistryServerHandler(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RegistryRequest request) {
        String type = request.getType();
        RegistryResponse response = new RegistryResponse();
        response.setRequestId(request.getRequestId());

        try {
            switch (type) {
                case "register":
                    handleRegister(request, response);
                    break;
                case "deregister":
                    handleDeregister(request, response);
                    break;
                case "discover":
                    handleDiscover(request, response);
                    break;
                case "heartbeat":
                    handleHeartbeat(request, response);
                    break;
                case "batchRegister":
                    handleBatchRegister(request, response);
                    break;
                case "batchDiscover":
                    handleBatchDiscover(request, response);
                    break;
                case "metadataUpdate":
                    handleMetadataUpdate(request, response);
                    break;
                default:
                    response.setSuccess(false);
                    response.setMessage("Unknown request type: " + type);
            }
        } catch (Exception e) {
            logger.error("Error processing request: {}", type, e);
            response.setSuccess(false);
            response.setMessage(e.getMessage());
        }

        ctx.writeAndFlush(response);
    }

    private void handleRegister(RegistryRequest request, RegistryResponse response) {
        ServiceInstance instance = request.getInstance();
        boolean result = serviceRegistry.register(instance);
        response.setSuccess(result);
        response.setMessage(result ? "Registered successfully" : "Registration failed");
    }

    private void handleDeregister(RegistryRequest request, RegistryResponse response) {
        String instanceId = request.getInstanceId();
        String serviceName = request.getServiceName();
        String namespace = request.getNamespace();
        boolean result = serviceRegistry.deregister(instanceId, serviceName, namespace);
        response.setSuccess(result);
        response.setMessage(result ? "Deregistered successfully" : "Deregistration failed");
    }

    private void handleDiscover(RegistryRequest request, RegistryResponse response) {
        String serviceName = request.getServiceName();
        String namespace = request.getNamespace();
        List<ServiceInstance> instances = serviceRegistry.discover(namespace, serviceName);
        response.setSuccess(true);
        response.setMessage("Discovered " + instances.size() + " instances");
        response.setInstances(instances);
    }

    private void handleHeartbeat(RegistryRequest request, RegistryResponse response) {
        String instanceId = request.getInstanceId();
        boolean result = serviceRegistry.heartbeat(instanceId);
        response.setSuccess(result);
        response.setMessage(result ? "Heartbeat updated" : "Instance not found");
    }

    private void handleBatchRegister(RegistryRequest request, RegistryResponse response) {
        Map<String, Object> ext = request.getExt();
        if (ext != null && ext.get("instances") != null) {
            List<ServiceInstance> instances = (List<ServiceInstance>) ext.get("instances");
            int count = 0;
            for (ServiceInstance instance : instances) {
                if (serviceRegistry.register(instance)) {
                    count++;
                }
            }
            response.setSuccess(true);
            response.setMessage("Registered " + count + " instances");
            response.setExt(java.util.Collections.singletonMap("successCount", count));
        } else {
            response.setSuccess(false);
            response.setMessage("No instances provided");
        }
    }

    private void handleBatchDiscover(RegistryRequest request, RegistryResponse response) {
        List<String> serviceNames = request.getServiceNames();
        String namespace = request.getNamespace();
        Map<String, List<ServiceInstance>> batchInstances = new java.util.concurrent.ConcurrentHashMap<>();
        for (String serviceName : serviceNames) {
            List<ServiceInstance> instances = serviceRegistry.discover(namespace, serviceName);
            batchInstances.put(serviceName, instances);
        }
        response.setSuccess(true);
        response.setMessage("Batch discover completed");
        response.setBatchInstances(batchInstances);
    }

    private void handleMetadataUpdate(RegistryRequest request, RegistryResponse response) {
        String instanceId = request.getInstanceId();
        Map<String, String> metadata = request.getMetadata();
        ServiceInstance instance = serviceRegistry.getInstance(instanceId);
        if (instance != null) {
            instance.setMetadata(metadata);
            response.setSuccess(true);
            response.setMessage("Metadata updated");
        } else {
            response.setSuccess(false);
            response.setMessage("Instance not found");
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Exception in server handler", cause);
        ctx.close();
    }
}
