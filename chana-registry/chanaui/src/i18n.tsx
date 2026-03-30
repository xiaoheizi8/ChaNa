import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';

type Language = 'zh' | 'en';

interface Translations {
  title: string;
  version: string;
  refresh: string;
  notifications: string;
  help: string;
  about: string;
  collapse: string;
  dashboard: string;
  services: string;
  instances: string;
  health: string;
  namespaces: string;
  metrics: string;
  settings: string;
  totalQps: string;
  registerQps: string;
  discoverQps: string;
  connections: string;
  latencyMonitor: string;
  realTime: string;
  serviceHealth: string;
  qpsTrend: string;
  servicesList: string;
  avg: string;
  p50: string;
  p90: string;
  p99: string;
  healthyInstances: string;
  unhealthyInstances: string;
  performanceTarget: string;
  writeQps: string;
  readQps: string;
  p99Latency: string;
  maxConnections: string;
  loading: string;
  serviceName: string;
  namespace: string;
  instanceStatus: string;
  instanceVersion: string;
  instanceManage: string;
  instanceId: string;
  host: string;
  port: string;
  weight: string;
  group: string;
  status: string;
  healthy: string;
  unhealthy: string;
  registerAction: string;
  deregisterAction: string;
  healthMonitor: string;
  checkInterval: string;
  lastCheck: string;
  affectedServices: string;
  multiTenant: string;
  description: string;
  serviceCount: string;
  instanceCount: string;
  coreMetrics: string;
  chana: string;
  consul: string;
  zookeeper: string;
  nacos: string;
  eureka: string;
  systemSettings: string;
  nettyPort: string;
  httpPort: string;
  healthCheckInterval: string;
  heartbeatTimeout: string;
  save: string;
  reset: string;
  more: string;
  viewAll: string;
  configs: string;
  configManagement: string;
  configDataId: string;
  configGroup: string;
  configContent: string;
  addConfig: string;
    editConfig: string;
    edit: string;
    confirmDelete: string;
  actions: string;
  configPublishSuccess: string;
  configPublishFailed: string;
  configDeleteSuccess: string;
  configDeleteFailed: string;
  delete: string;
  pleaseInput: string;
  configDataIdPlaceholder: string;
  configGroupPlaceholder: string;
  configContentPlaceholder: string;
}

const translations: Record<Language, Translations> = {
  zh: {
    title: 'ChaNa 注册中心控制台',
    version: 'v3.0.0',
    refresh: '刷新数据',
    notifications: '通知',
    help: '帮助',
    about: '关于',
    collapse: '收起',
    dashboard: '概览',
    services: '服务列表',
    instances: '实例管理',
    health: '健康监控',
    namespaces: '命名空间',
    metrics: '核心指标',
    settings: '系统设置',
    totalQps: '总QPS',
    registerQps: '注册QPS',
    discoverQps: '发现QPS',
    connections: '连接数',
    latencyMonitor: '实时延迟监控 (μs)',
    realTime: '实时',
    serviceHealth: '服务健康状态',
    qpsTrend: 'QPS趋势',
    servicesList: '服务列表',
    avg: '平均值',
    p50: 'P50',
    p90: 'P90',
    p99: 'P99',
    healthyInstances: '健康',
    unhealthyInstances: '异常',
    performanceTarget: '性能目标',
    writeQps: '写入',
    readQps: '读取',
    p99Latency: 'P99延迟',
    maxConnections: '支持',
    loading: '正在加载监控数据...',
    serviceName: '服务名称',
    namespace: '命名空间',
    instanceStatus: '实例状态',
    instanceVersion: '版本',
    instanceManage: '实例管理',
    instanceId: '实例ID',
    host: '主机',
    port: '端口',
    weight: '权重',
    group: '分组',
    status: '状态',
    healthy: '健康',
    unhealthy: '异常',
    registerAction: '注册',
    deregisterAction: '注销',
    healthMonitor: '健康监控',
    checkInterval: '检查间隔',
    lastCheck: '最后检查',
    affectedServices: '受影响服务',
    multiTenant: '多租户命名空间',
    description: '描述',
    serviceCount: '服务数',
    instanceCount: '实例数',
    coreMetrics: '核心性能指标对比',
    chana: 'ChaNa ⭐',
    consul: 'Consul',
    zookeeper: 'Zookeeper',
    nacos: 'Nacos',
    eureka: 'Eureka',
    systemSettings: '系统设置',
    nettyPort: 'Netty端口',
    httpPort: 'HTTP端口',
    healthCheckInterval: '健康检查间隔',
    heartbeatTimeout: '心跳超时',
    save: '保存',
    reset: '重置',
    more: '更多',
    viewAll: '查看全部',
    configs: '配置管理',
    configManagement: '配置中心',
    configDataId: '配置ID',
    configGroup: '分组',
    configContent: '配置内容',
    addConfig: '添加配置',
    editConfig: '编辑配置',
    edit: '编辑',
    delete: '删除',
    actions: '操作',
    confirmDelete: '确定删除该配置?',
    configPublishSuccess: '配置发布成功',
    configPublishFailed: '配置发布失败',
    configDeleteSuccess: '配置删除成功',
    configDeleteFailed: '配置删除失败',
    pleaseInput: '请输入',
    configDataIdPlaceholder: '请输入配置ID',
    configGroupPlaceholder: '请输入分组',
    configContentPlaceholder: '请输入配置内容',
  },
  en: {
    title: 'ChaNa Registry Console',
    version: 'v3.0.0',
    refresh: 'Refresh',
    notifications: 'Notifications',
    help: 'Help',
    about: 'About',
    collapse: 'Collapse',
    dashboard: 'Dashboard',
    services: 'Services',
    instances: 'Instances',
    health: 'Health',
    namespaces: 'Namespaces',
    metrics: 'Metrics',
    settings: 'Settings',
    totalQps: 'Total QPS',
    registerQps: 'Register QPS',
    discoverQps: 'Discover QPS',
    connections: 'Connections',
    latencyMonitor: 'Real-time Latency (μs)',
    realTime: 'Live',
    serviceHealth: 'Service Health',
    qpsTrend: 'QPS Trend',
    servicesList: 'Service List',
    avg: 'Average',
    p50: 'P50',
    p90: 'P90',
    p99: 'P99',
    healthyInstances: 'Healthy',
    unhealthyInstances: 'Unhealthy',
    performanceTarget: 'Performance Target',
    writeQps: 'Write',
    readQps: 'Read',
    p99Latency: 'P99 Latency',
    maxConnections: 'Max',
    loading: 'Loading metrics...',
    serviceName: 'Service Name',
    namespace: 'Namespace',
    instanceStatus: 'Instance Status',
    instanceVersion: 'Version',
    instanceManage: 'Instance Management',
    instanceId: 'Instance ID',
    host: 'Host',
    port: 'Port',
    weight: 'Weight',
    group: 'Group',
    status: 'Status',
    healthy: 'Healthy',
    unhealthy: 'Unhealthy',
    registerAction: 'Register',
    deregisterAction: 'Deregister',
    healthMonitor: 'Health Monitor',
    checkInterval: 'Check Interval',
    lastCheck: 'Last Check',
    affectedServices: 'Affected Services',
    multiTenant: 'Multi-tenant Namespaces',
    description: 'Description',
    serviceCount: 'Services',
    instanceCount: 'Instances',
    coreMetrics: 'Core Performance Metrics',
    chana: 'ChaNa ⭐',
    consul: 'Consul',
    zookeeper: 'Zookeeper',
    nacos: 'Nacos',
    eureka: 'Eureka',
    systemSettings: 'System Settings',
    nettyPort: 'Netty Port',
    httpPort: 'HTTP Port',
    healthCheckInterval: 'Health Check Interval',
    heartbeatTimeout: 'Heartbeat Timeout',
    save: 'Save',
    reset: 'Reset',
    more: 'More',
    viewAll: 'View All',
    configs: 'Configs',
    configManagement: 'Config Center',
    configDataId: 'Data ID',
    configGroup: 'Group',
    configContent: 'Content',
    addConfig: 'Add Config',
    editConfig: 'Edit Config',
    edit: 'Edit',
    delete: 'Delete',
    actions: 'Actions',
    confirmDelete: 'Are you sure to delete this config?',
    configPublishSuccess: 'Config published successfully',
    configPublishFailed: 'Config publish failed',
    configDeleteSuccess: 'Config deleted successfully',
    configDeleteFailed: 'Config delete failed',
    pleaseInput: 'Please input',
    configDataIdPlaceholder: 'Enter config data ID',
    configGroupPlaceholder: 'Enter config group',
    configContentPlaceholder: 'Enter config content',
  }
};

interface I18nContextType {
  language: Language;
  setLanguage: (lang: Language) => void;
  t: Translations;
}

const I18nContext = createContext<I18nContextType | null>(null);

export const I18nProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [language, setLanguage] = useState<Language>(() => {
    const saved = localStorage.getItem('chana-language');
    return (saved as Language) || 'zh';
  });

  useEffect(() => {
    localStorage.setItem('chana-language', language);
  }, [language]);

  return (
    <I18nContext.Provider value={{ language, setLanguage, t: translations[language] }}>
      {children}
    </I18nContext.Provider>
  );
};

export const useI18n = () => {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error('useI18n must be used within I18nProvider');
  }
  return context;
};

export type { Language, Translations };
