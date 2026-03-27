export interface CoreMetrics {
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

export interface Service {
  serviceName: string;
  namespace?: string;
  instanceCount: number;
  healthyCount: number;
  unhealthyCount: number;
  version: string;
  qps: number;
  avgLatencyUs: number;
}

export interface Namespace {
  namespace: string;
  description: string;
  serviceCount: number;
  instanceCount: number;
}

export interface LatencyData {
  time: string;
  p50: number;
  avg: number;
  p90: number;
  p99: number;
}
