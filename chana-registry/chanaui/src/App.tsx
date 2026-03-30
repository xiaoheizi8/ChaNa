import React, { useState, useEffect } from 'react';
import { Routes, Route, Link, useLocation } from 'react-router-dom';
import { ConfigProvider, Layout, Menu, Typography, Card, Space, Button, Tooltip, Badge, Table, theme, Result } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import enUS from 'antd/locale/en_US';
import { 
  DashboardOutlined, ThunderboltOutlined, CloudServerOutlined, 
  GlobalOutlined as GlobeOutlined, HeartOutlined, SettingOutlined, AppstoreOutlined,
  MenuFoldOutlined, MenuUnfoldOutlined, MoonOutlined, SunOutlined,
  BellOutlined, QuestionCircleOutlined, InfoCircleOutlined,
  FileTextOutlined
} from '@ant-design/icons';
import Dashboard from './pages/Dashboard';
import ServiceDetail from './pages/ServiceDetail';
import HealthMonitor from './pages/HealthMonitor';
import InstanceManage from './pages/InstanceManage';
import Settings from './pages/Settings';
import ConfigManage from './pages/ConfigManage';
import { useI18n, I18nProvider, Language } from './i18n';

const { Header, Sider, Content } = Layout;
const { Text } = Typography;
const { defaultAlgorithm, darkAlgorithm } = theme;

class ErrorBoundary extends React.Component<
  { children: React.ReactNode },
  { hasError: boolean; error?: Error }
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error) {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Result
          status="error"
          title="Something went wrong"
          subTitle={this.state.error?.message || 'An unexpected error occurred'}
          extra={
            <Button type="primary" onClick={() => window.location.reload()}>
              Reload Page
            </Button>
          }
        />
      );
    }
    return this.props.children;
  }
}

const ServiceListPage: React.FC = () => {
  const { t } = useI18n();
  const [services, setServices] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    import('./services/api').then(({ apiService }) => {
      apiService.getServices()
        .then(setServices)
        .catch(() => setServices([]))
        .finally(() => setLoading(false));
    }).catch(() => setLoading(false));
  }, []);

  const columns = [
    { title: t.serviceName, dataIndex: 'serviceName', key: 'serviceName', render: (text: string) => <Text strong>{text}</Text> },
    { title: t.namespace, dataIndex: 'namespace', key: 'namespace' },
    { title: t.instanceStatus, dataIndex: 'instanceCount', key: 'instanceCount' },
    { title: t.instanceVersion, dataIndex: 'version', key: 'version' },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Card title={<Space><CloudServerOutlined style={{ color: '#00B96B' }} />{t.services}</Space>}
        style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
        <Table loading={loading} pagination={{ pageSize: 10 }} columns={columns} dataSource={services} rowKey="serviceName" />
      </Card>
    </div>
  );
};

const NamespacePage: React.FC = () => {
  const { t } = useI18n();
  const [namespaces, setNamespaces] = useState<any[]>([]);

  useEffect(() => {
    import('./services/api').then(({ apiService }) => {
      apiService.getNamespaces().then(setNamespaces).catch(() => {});
    });
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Card title={<Space><GlobeOutlined style={{ color: '#00B96B' }} />{t.multiTenant}</Space>}
        style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
        <Table pagination={false} columns={[
          { title: t.namespace, dataIndex: 'namespace', key: 'namespace' },
          { title: t.description, dataIndex: 'description', key: 'description' },
          { title: t.serviceCount, dataIndex: 'serviceCount', key: 'serviceCount' },
          { title: t.instanceCount, dataIndex: 'instanceCount', key: 'instanceCount' },
        ]} dataSource={namespaces} rowKey="namespace" />
      </Card>
    </div>
  );
};

const CoreMetricsPage: React.FC = () => {
  const { t } = useI18n();
  const [metrics, setMetrics] = useState<any>(null);

  useEffect(() => {
    import('./services/api').then(({ apiService }) => {
      apiService.getMetrics().then(setMetrics).catch(console.error);
    });
  }, []);

  return (
    <div style={{ padding: 24 }}>
      <Card title={<Space><ThunderboltOutlined style={{ color: '#00B96B' }} />{t.coreMetrics}</Space>}
        style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}>
        <Table size="small" pagination={false} dataSource={[
          { metric: t.writeQps, chana: metrics?.registerQps || '-', consul: '5,000', zookeeper: '8,000', nacos: '10,000', eureka: '3,000' },
          { metric: t.readQps, chana: metrics?.discoverQps || '-', consul: '15,000', zookeeper: '20,000', nacos: '30,000', eureka: '10,000' },
          { metric: t.p99Latency, chana: `${metrics?.p99LatencyUs || '-'}μs`, consul: '5ms', zookeeper: '3ms', nacos: '2ms', eureka: '10ms' },
          { metric: t.maxConnections, chana: metrics?.connections || '-', consul: '10,000', zookeeper: '15,000', nacos: '20,000', eureka: '8,000' },
        ]} columns={[
          { title: '指标', dataIndex: 'metric', key: 'metric', render: (v: string) => <Text strong>{v}</Text> },
          { title: t.chana, dataIndex: 'chana', key: 'chana', render: (v: any) => <span className="tag-success">{v}</span> },
          { title: t.consul, dataIndex: 'consul', key: 'consul' },
          { title: t.zookeeper, dataIndex: 'zookeeper', key: 'zookeeper' },
          { title: t.nacos, dataIndex: 'nacos', key: 'nacos' },
          { title: t.eureka, dataIndex: 'eureka', key: 'eureka' },
        ]} />
      </Card>
    </div>
  );
};

const AppContent: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const [darkMode, setDarkMode] = useState(false);
  const location = useLocation();
  const { t, language, setLanguage } = useI18n();

  const toggleLanguage = () => {
    setLanguage(language === 'zh' ? 'en' : 'zh');
  };

  const menuItems = [
    { key: '/', icon: <DashboardOutlined />, label: <Link to="/">{t.dashboard}</Link> },
    { key: '/services', icon: <CloudServerOutlined />, label: <Link to="/services">{t.services}</Link> },
    { key: '/instances', icon: <AppstoreOutlined />, label: <Link to="/instances">{t.instances}</Link> },
    { key: '/configs', icon: <FileTextOutlined />, label: <Link to="/configs">{t.configs}</Link> },
    { key: '/health', icon: <HeartOutlined />, label: <Link to="/health">{t.health}</Link> },
    { key: '/namespaces', icon: <GlobeOutlined />, label: <Link to="/namespaces">{t.namespaces}</Link> },
    { key: '/metrics', icon: <ThunderboltOutlined />, label: <Link to="/metrics">{t.metrics}</Link> },
    { key: '/settings', icon: <SettingOutlined />, label: <Link to="/settings">{t.settings}</Link> },
  ];

  return (
    <ConfigProvider locale={language === 'zh' ? zhCN : enUS} theme={{
      algorithm: darkMode ? darkAlgorithm : defaultAlgorithm,
      token: { colorPrimary: '#00B96B', borderRadius: 8 }
    }}>
      <Layout style={{ minHeight: '100vh' }}>
        <Sider 
          collapsible collapsed={collapsed} onCollapse={setCollapsed} trigger={null} width={220} theme="dark"
          style={{ background: darkMode ? '#0f0f1a' : 'linear-gradient(180deg, #1a1a2e 0%, #16213e 100%)', boxShadow: '2px 0 8px rgba(0,0,0,0.15)' }}>
          <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', borderBottom: '1px solid rgba(255,255,255,0.08)' }}>
            <Space>
              <ThunderboltOutlined style={{ color: '#00B96B', fontSize: 24 }} />
              {!collapsed && <span style={{ color: '#fff', fontSize: 18, fontWeight: 700, background: 'linear-gradient(135deg, #00B96B 0%, #1890ff 100%)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>ChaNa</span>}
            </Space>
          </div>
          <Menu theme="dark" mode="inline" selectedKeys={[location.pathname]} style={{ background: 'transparent', borderRight: 'none', marginTop: 8 }} items={menuItems} />
          <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, padding: '16px', borderTop: '1px solid rgba(255,255,255,0.08)' }}>
            <Button type="text" icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />} onClick={() => setCollapsed(!collapsed)} style={{ color: '#fff', width: '100%' }}>{!collapsed && t.collapse}</Button>
          </div>
        </Sider>
        <Layout>
          <Header style={{ padding: '0 24px', background: darkMode ? '#1a1a2e' : '#fff', display: 'flex', alignItems: 'center', justifyContent: 'space-between', boxShadow: '0 2px 8px rgba(0,0,0,0.06)', position: 'sticky', top: 0, zIndex: 100 }}>
            <Space>
              <Text strong style={{ fontSize: 18 }}>{t.title}</Text>
              <span className="tag-success">{t.version}</span>
            </Space>
            <Space size="middle">
              <Tooltip title={t.refresh as string}><Button type="text" icon={<DashboardOutlined />} /></Tooltip>
              <Tooltip title={t.notifications as string}><Badge count={0}><Button type="text" icon={<BellOutlined />} /></Badge></Tooltip>
              <Tooltip title={language === 'zh' ? 'Switch to English' : '切换到中文'}>
                <Button type="text" icon={<GlobeOutlined />} onClick={toggleLanguage} style={{ fontWeight: 500 }}>
                  {language === 'zh' ? 'EN' : '中'}
                </Button>
              </Tooltip>
              <Tooltip title={darkMode ? 'Light Mode' : 'Dark Mode'}>
                <Button type="text" icon={darkMode ? <SunOutlined /> : <MoonOutlined />} onClick={() => setDarkMode(!darkMode)} />
              </Tooltip>
              <Tooltip title={t.help as string}><Button type="text" icon={<QuestionCircleOutlined />} /></Tooltip>
              <Tooltip title={t.about as string}><Button type="text" icon={<InfoCircleOutlined />} /></Tooltip>
            </Space>
          </Header>
          <Content style={{ margin: 0, padding: 0, background: darkMode ? '#0f0f1a' : 'linear-gradient(135deg, #f0f2f5 0%, #e6f7ff 100%)', minHeight: 'calc(100vh - 64px)' }}>
            <ErrorBoundary>
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/services" element={<ServiceListPage />} />
                <Route path="/services/:serviceName" element={<ServiceDetail />} />
                <Route path="/instances" element={<InstanceManage />} />
                <Route path="/configs" element={<ConfigManage />} />
                <Route path="/health" element={<HealthMonitor />} />
                <Route path="/namespaces" element={<NamespacePage />} />
                <Route path="/metrics" element={<CoreMetricsPage />} />
                <Route path="/settings" element={<Settings />} />
              </Routes>
            </ErrorBoundary>
          </Content>
        </Layout>
      </Layout>
    </ConfigProvider>
  );
};

const App: React.FC = () => (
  <I18nProvider>
    <AppContent />
  </I18nProvider>
);

export default App;
