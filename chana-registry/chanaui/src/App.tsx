import React, { useState, useEffect } from 'react';
import { Routes, Route, Link, useLocation } from 'react-router-dom';
import { ConfigProvider, Layout, Menu, Typography, Table, Tag, Card, Row, Col, Statistic, Space } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import { 
  DashboardOutlined, ThunderboltOutlined, CloudServerOutlined, 
  GlobalOutlined, HeartOutlined, SettingOutlined, AppstoreOutlined
} from '@ant-design/icons';
import Dashboard from './pages/Dashboard';
import ServiceDetail from './pages/ServiceDetail';
import HealthMonitor from './pages/HealthMonitor';
import InstanceManage from './pages/InstanceManage';
import Settings from './pages/Settings';
import { apiService, ServiceInfo } from './services/api';

const { Header, Sider, Content } = Layout;
const { Title, Text } = Typography;

const ServiceListPage: React.FC = () => {
  const [services, setServices] = useState<ServiceInfo[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiService.getServices()
      .then(setServices)
      .catch(() => setServices([]))
      .finally(() => setLoading(false));
  }, []);

  const columns = [
    { 
      title: '服务名称', 
      dataIndex: 'serviceName', 
      key: 'serviceName', 
      render: (text: string, record: ServiceInfo) => (
        <Link to={`/services/${text}?namespace=${record.namespace}`}>{text}</Link>
      )
    },
    { title: '命名空间', dataIndex: 'namespace', key: 'namespace' },
    { 
      title: '实例数', 
      dataIndex: 'instanceCount', 
      key: 'instanceCount',
      render: (_: number, r: ServiceInfo) => (
        <Tag color={r.unhealthyCount === 0 ? 'green' : 'red'}>
          {r.healthyCount}/{r.instanceCount}
        </Tag>
      )
    },
    { title: '版本', dataIndex: 'version', key: 'version', render: (v: string) => <Tag>{v}</Tag> },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}><CloudServerOutlined style={{ marginRight: 8 }} />服务列表</Title>
      <Table 
        loading={loading}
        pagination={{ pageSize: 10 }} 
        columns={columns} 
        dataSource={services} 
        rowKey="serviceName"
      />
    </div>
  );
};

const NamespacePage: React.FC = () => {
  const [namespaces, setNamespaces] = useState<any[]>([]);

  useEffect(() => {
    apiService.getNamespaces().then(setNamespaces).catch(() => {});
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}><GlobalOutlined style={{ marginRight: 8 }} />多租户命名空间</Title>
      <Table 
        pagination={false} 
        columns={[
          { title: '命名空间', dataIndex: 'namespace', key: 'namespace' },
          { title: '描述', dataIndex: 'description', key: 'description' },
          { title: '服务数', dataIndex: 'serviceCount', key: 'serviceCount' },
          { title: '实例数', dataIndex: 'instanceCount', key: 'instanceCount' },
        ]} 
        dataSource={namespaces} 
        rowKey="namespace"
      />
    </div>
  );
};

const CoreMetricsPage: React.FC = () => {
  const [metrics, setMetrics] = useState<any>(null);

  useEffect(() => {
    apiService.getMetrics().then(setMetrics).catch(console.error);
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}><ThunderboltOutlined style={{ marginRight: 8 }} />核心性能指标</Title>
      <Table 
        size="small" 
        pagination={false} 
        dataSource={[
          { metric: '写入QPS', chana: metrics?.registerQps || 0, consul: '5,000', zookeeper: '8,000', nacos: '10,000', eureka: '3,000' },
          { metric: '读取QPS', chana: metrics?.discoverQps || 0, consul: '15,000', zookeeper: '20,000', nacos: '30,000', eureka: '10,000' },
          { metric: 'P99延迟', chana: `${metrics?.p99LatencyUs || 0}μs`, consul: '5ms', zookeeper: '3ms', nacos: '2ms', eureka: '10ms' },
          { metric: '最大连接', chana: metrics?.connections || 0, consul: '10,000', zookeeper: '15,000', nacos: '20,000', eureka: '8,000' },
          { metric: '总请求数', chana: metrics?.totalRequests || 0, consul: 'N/A', zookeeper: 'N/A', nacos: 'N/A', eureka: 'N/A' },
          { metric: '平均延迟', chana: `${metrics?.avgLatencyUs || 0}μs`, consul: 'N/A', zookeeper: 'N/A', nacos: 'N/A', eureka: 'N/A' },
        ]} 
        columns={[
          { title: '指标', dataIndex: 'metric', key: 'metric' },
          { title: 'ChaNa ⭐', dataIndex: 'chana', key: 'chana', render: (v: any) => <Tag color="green">{v}</Tag> },
          { title: 'Consul', dataIndex: 'consul', key: 'consul' },
          { title: 'Zookeeper', dataIndex: 'zookeeper', key: 'zookeeper' },
          { title: 'Nacos', dataIndex: 'nacos', key: 'nacos' },
          { title: 'Eureka', dataIndex: 'eureka', key: 'eureka' },
        ]}
      />
    </div>
  );
};

const AppContent: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const location = useLocation();

  return (
    <ConfigProvider locale={zhCN}>
      <Layout style={{ minHeight: '100vh' }}>
        <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed} theme="dark">
          <div style={{ 
            height: 64, 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'center', 
            color: '#fff', 
            fontSize: collapsed ? 16 : 20, 
            fontWeight: 'bold' 
          }}>
            {collapsed ? 'CN' : 'ChaNa ⚡'}
          </div>
          <Menu 
            theme="dark" 
            mode="inline" 
            selectedKeys={[location.pathname]}
            items={[
              { key: '/', icon: <DashboardOutlined />, label: <Link to="/">概览</Link> },
              { key: '/services', icon: <CloudServerOutlined />, label: <Link to="/services">服务列表</Link> },
              { key: '/instances', icon: <AppstoreOutlined />, label: <Link to="/instances">实例管理</Link> },
              { key: '/health', icon: <HeartOutlined />, label: <Link to="/health">健康监控</Link> },
              { key: '/namespaces', icon: <GlobalOutlined />, label: <Link to="/namespaces">命名空间</Link> },
              { key: '/metrics', icon: <ThunderboltOutlined />, label: <Link to="/metrics">核心指标</Link> },
              { key: '/settings', icon: <SettingOutlined />, label: <Link to="/settings">设置</Link> },
            ]}
          />
        </Sider>
        <Layout>
          <Header style={{ 
            padding: '0 24px', 
            background: '#fff', 
            display: 'flex', 
            alignItems: 'center', 
            justifyContent: 'space-between' 
          }}>
            <span style={{ fontSize: 18, fontWeight: 500 }}>ChaNa 注册中心控制台</span>
            <span style={{ color: '#999' }}>服务端点: localhost:9998</span>
          </Header>
          <Content style={{ margin: 0, padding: 0, background: '#f0f2f5', minHeight: 'calc(100vh - 64px)' }}>
            <Routes>
              <Route path="/" element={<Dashboard />} />
              <Route path="/services" element={<ServiceListPage />} />
              <Route path="/services/:serviceName" element={<ServiceDetail />} />
              <Route path="/instances" element={<InstanceManage />} />
              <Route path="/health" element={<HealthMonitor />} />
              <Route path="/namespaces" element={<NamespacePage />} />
              <Route path="/metrics" element={<CoreMetricsPage />} />
              <Route path="/settings" element={<Settings />} />
            </Routes>
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
};

const App: React.FC = () => (
  <AppContent />
);

export default App;
