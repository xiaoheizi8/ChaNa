import { axiosClient } from '../utils/axios';

export interface ServiceMetrics {
  qps: number;
  registerQps: number;
  discoverQps: number;
  heartbeatQps: number;
  connections: number;
  avgLatencyUs: number;
  p50LatencyUs: number;
  p90LatencyUs: number;
  p99LatencyUs: number;
  totalRequests: number;
}

export interface ServiceInfo {
  serviceName: string;
  namespace: string;
  instanceCount: number;
  healthyCount: number;
  unhealthyCount: number;
  version: string;
  qps?: number;
  avgLatencyUs?: number;
}

export interface ServiceInstance {
  instanceId: string;
  serviceName: string;
  host: string;
  port: number;
  healthy: boolean;
  weight: number;
  version: string;
  lastHeartbeatTime: number;
  registrationTime: number;
}

export interface ServiceDetail {
  serviceName: string;
  namespace: string;
  total: number;
  instances: ServiceInstance[];
}

export interface NamespaceInfo {
  namespace: string;
  description: string;
  serviceCount: number;
  instanceCount: number;
}

export interface HealthStatus {
  status: string;
  timestamp: number;
  healthyInstances: number;
  unhealthyInstances: number;
  protectionMode: boolean;
}

export interface ServerStats {
  totalInstances: number;
  totalServices: number;
  totalRegistrations: number;
  totalDiscovers: number;
  globalVersion: number;
}

class ChanaApiService {
  async getMetrics(): Promise<ServiceMetrics> {
    return axiosClient.get<ServiceMetrics>('/v1/metrics');
  }

  async getServices(): Promise<ServiceInfo[]> {
    return axiosClient.get<ServiceInfo[]>('/v1/services');
  }

  async getServiceDetail(serviceName: string, namespace = 'default'): Promise<ServiceDetail> {
    return axiosClient.get<ServiceDetail>(`/v1/services/${serviceName}?namespace=${namespace}`);
  }

  async getNamespaces(): Promise<NamespaceInfo[]> {
    return axiosClient.get<NamespaceInfo[]>('/v1/namespaces');
  }

  async getHealth(): Promise<HealthStatus> {
    return axiosClient.get<HealthStatus>('/v1/health');
  }

  async getStats(): Promise<ServerStats> {
    return axiosClient.get<ServerStats>('/v1/stats');
  }

  async registerService(instance: Partial<ServiceInstance>): Promise<void> {
    return axiosClient.post('/v1/services/register', instance);
  }

  async deregisterService(instanceId: string, serviceName: string, namespace = 'default'): Promise<void> {
    return axiosClient.post('/v1/services/deregister', {
      instanceId,
      serviceName,
      namespace,
    });
  }

  async sendHeartbeat(instanceId: string, serviceName: string, namespace = 'default'): Promise<void> {
    return axiosClient.post('/v1/heartbeat', {
      instanceId,
      serviceName,
      namespace,
    });
  }
}

export const apiService = new ChanaApiService();
export default apiService;
