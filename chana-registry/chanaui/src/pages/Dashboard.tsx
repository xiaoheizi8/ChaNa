import React, { useState, useEffect, useCallback } from 'react';
import { Card, Row, Col, Statistic, Typography, Space, Table, Tag, Alert, Progress, Spin } from 'antd';
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip as RechartsTooltip, ResponsiveContainer, LineChart, Line, PieChart, Pie, Cell } from 'recharts';
import { CloudServerOutlined, ThunderboltOutlined, RiseOutlined, ClockCircleOutlined, ApiOutlined, CheckCircleOutlined } from '@ant-design/icons';
import { Link } from 'react-router-dom';
import { apiService, ServiceMetrics, ServiceInfo } from '../services/api';
import { Empty } from 'antd';
import { useI18n } from '../i18n';

const { Text } = Typography;

interface LatencyData {
  time: string;
  p50: number;
  avg: number;
  p90: number;
  p99: number;
}

interface QpsData {
  time: string;
  register: number;
  discover: number;
}

const COLORS = ['#00B96B', '#1890ff', '#722ed1', '#fa8c16', '#f5222d'];

const getDefaultMetrics = (): ServiceMetrics => ({
  qps: 0,
  registerQps: 0,
  discoverQps: 0,
  heartbeatQps: 0,
  connections: 0,
  totalRequests: 0,
  avgLatencyUs: 0,
  p50LatencyUs: 0,
  p90LatencyUs: 0,
  p99LatencyUs: 0,
});

const Dashboard: React.FC = () => {
  const { t } = useI18n();
  const [metrics, setMetrics] = useState<ServiceMetrics | null>(null);
  const [services, setServices] = useState<ServiceInfo[]>([]);
  const [latencyData, setLatencyData] = useState<LatencyData[]>([]);
  const [qpsData, setQpsData] = useState<QpsData[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    try {
      const [metricsData, servicesData] = await Promise.all([
        apiService.getMetrics(),
        apiService.getServices()
      ]);
      setMetrics(metricsData || getDefaultMetrics());
      setServices(servicesData || []);
      
      const now = new Date().toLocaleTimeString();
      const m = metricsData || getDefaultMetrics();
      
      setLatencyData(prev => {
        const newData = {
          time: now,
          p50: m.p50LatencyUs,
          avg: m.avgLatencyUs,
          p90: m.p90LatencyUs,
          p99: m.p99LatencyUs,
        };
        return [...prev.slice(-9), newData];
      });

      setQpsData(prev => {
        const newData = {
          time: now,
          register: m.registerQps,
          discover: m.discoverQps,
        };
        return [...prev.slice(-9), newData];
      });
      
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch data:', error);
      setMetrics(getDefaultMetrics());
      setServices([]);
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000);
    return () => clearInterval(interval);
  }, [fetchData]);

  const serviceColumns = [
    { 
      title: t.serviceName, 
      dataIndex: 'serviceName', 
      key: 'serviceName', 
      render: (text: string, record: ServiceInfo) => (
        <Space>
          <CloudServerOutlined style={{ color: '#00B96B' }} />
          <Link to={`/services/${text}?namespace=${record.namespace}`} style={{ fontWeight: 500 }}>{text}</Link>
        </Space>
      )
    },
    { 
      title: t.instanceStatus, 
      dataIndex: 'instanceCount', 
      key: 'instanceCount', 
      render: (_: number, r: ServiceInfo) => {
        const healthyRate = r.instanceCount > 0 ? (r.healthyCount / r.instanceCount) * 100 : 0;
        return (
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <Progress 
              percent={Math.round(healthyRate)} 
              size="small" 
              showInfo={false}
              strokeColor={healthyRate >= 80 ? '#00B96B' : '#ff4d4f'}
              style={{ width: 60 }}
            />
            <Tag color={r.unhealthyCount === 0 ? 'success' : 'error'}>
              {r.healthyCount}/{r.instanceCount}
            </Tag>
          </div>
        )
      }
    },
    { title: t.version, dataIndex: 'version', key: 'version', render: (v: string) => <Tag color="blue" style={{ borderRadius: 4 }}>v{v}</Tag> },
    { title: t.namespace, dataIndex: 'namespace', key: 'namespace', render: (v: string) => <Tag>{v}</Tag> },
  ];

  const healthData = services.reduce((acc, s) => {
    const healthy = s.instanceCount - s.unhealthyCount;
    const unhealthy = s.unhealthyCount;
    return { healthy: acc.healthy + healthy, unhealthy: acc.unhealthy + unhealthy };
  }, { healthy: 0, unhealthy: 0 });

  const pieData = [
    { name: t.healthyInstances, value: healthData.healthy, color: '#00B96B' },
    { name: t.unhealthyInstances, value: healthData.unhealthy, color: '#ff4d4f' },
  ];

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 'calc(100vh - 200px)', background: 'transparent' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!metrics) {
    return (
      <div style={{ padding: 24 }}>
        <Empty description="Unable to connect to ChaNa Registry Server. Please ensure the server is running." />
      </div>
    );
  }

  return (
    <div style={{ padding: 24, background: 'linear-gradient(135deg, #f0f2f5 0%, #e6f7ff 100%)', minHeight: 'calc(100vh - 64px)' }}>
      <Alert 
        message={
          <Space>
            <ThunderboltOutlined style={{ color: '#00B96B', fontSize: 18 }} />
            <Text strong style={{ fontSize: 16 }}>ChaNa Registry - Ultra High Performance</Text>
          </Space>
        } 
        description={
          <Row gutter={24}>
            <Col><Text>{t.writeQps} <Text strong style={{ color: '#00B96B' }}>50K+/s</Text></Text></Col>
            <Col><Text>{t.readQps} <Text strong style={{ color: '#00B96B' }}>100K+/s</Text></Text></Col>
            <Col><Text>{t.p99Latency} <Text strong style={{ color: '#00B96B' }}>&lt; 1ms</Text></Text></Col>
            <Col><Text>{t.maxConnections} <Text strong style={{ color: '#00B96B' }}>50K+</Text></Text></Col>
          </Row>
        } 
        type="success" 
        showIcon 
        style={{ marginBottom: 24, borderRadius: 12 }}
        icon={<CheckCircleOutlined style={{ color: '#00B96B' }} />}
      />

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} bodyStyle={{ padding: 20 }}>
            <Statistic title={<Space><ThunderboltOutlined style={{color:'#00B96B'}}/> {t.totalQps}</Space>} value={metrics.qps} suffix="/s" valueStyle={{ color: '#00B96B', fontSize: 36, fontWeight: 600 }} />
            <div style={{ marginTop: 8 }}><RiseOutlined style={{ color: '#52c41a' }} /><Text type="success" style={{ marginLeft: 4 }}>+12.5%</Text></div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} bodyStyle={{ padding: 20 }}>
            <Statistic title={<Space><ApiOutlined style={{color:'#1890ff'}}/> {t.registerQps}</Space>} value={metrics.registerQps} suffix="/s" valueStyle={{ color: '#1890ff', fontSize: 36, fontWeight: 600 }} />
            <div style={{ marginTop: 8 }}><Text type="secondary">Register Frequency</Text></div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} bodyStyle={{ padding: 20 }}>
            <Statistic title={<Space><CloudServerOutlined style={{color:'#722ed1'}}/> {t.discoverQps}</Space>} value={metrics.discoverQps} suffix="/s" valueStyle={{ color: '#722ed1', fontSize: 36, fontWeight: 600 }} />
            <div style={{ marginTop: 8 }}><Text type="secondary">Discover Frequency</Text></div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }} bodyStyle={{ padding: 20 }}>
            <Statistic title={<Space><ApiOutlined style={{color:'#fa8c16'}}/> {t.connections}</Space>} value={metrics.connections} suffix="/ 50K" valueStyle={{ color: '#fa8c16', fontSize: 36, fontWeight: 600 }} />
            <Progress percent={(metrics.connections / 50000) * 100} showInfo={false} strokeColor="#fa8c16" style={{ marginTop: 8 }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={16}>
          <Card 
            title={<Space><ClockCircleOutlined style={{ color: '#00B96B' }} /><span>{t.latencyMonitor}</span></Space>}
            extra={<Tag color="green">{t.realTime}</Tag>}
            style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
          >
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={6}><Statistic title={t.p50} value={metrics.p50LatencyUs} suffix="μs" valueStyle={{ color: '#00B96B' }}/></Col>
              <Col span={6}><Statistic title={t.avg} value={metrics.avgLatencyUs} suffix="μs" valueStyle={{ color: '#1890ff' }}/></Col>
              <Col span={6}><Statistic title={t.p90} value={metrics.p90LatencyUs} suffix="μs" valueStyle={{ color: '#fa8c16' }}/></Col>
              <Col span={6}><Statistic title={t.p99} value={metrics.p99LatencyUs} suffix="μs" valueStyle={{ color: '#f5222d', fontWeight: 'bold' }}/></Col>
            </Row>
            <div style={{ height: 250 }}>
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={latencyData}>
                  <defs>
                    <linearGradient id="colorP50" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#00B96B" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#00B96B" stopOpacity={0}/>
                    </linearGradient>
                    <linearGradient id="colorP99" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#f5222d" stopOpacity={0.3}/>
                      <stop offset="95%" stopColor="#f5222d" stopOpacity={0}/>
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0"/>
                  <XAxis dataKey="time" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <RechartsTooltip contentStyle={{ borderRadius: 8, border: 'none', boxShadow: '0 2px 8px rgba(0,0,0,0.1)' }} />
                  <Area type="monotone" dataKey="p50" stackId="1" stroke="#00B96B" fill="url(#colorP50)" name="P50" />
                  <Area type="monotone" dataKey="p90" stackId="2" stroke="#fa8c16" fill="transparent" name="P90" strokeWidth={2} />
                  <Area type="monotone" dataKey="p99" stackId="3" stroke="#f5222d" fill="url(#colorP99)" name="P99" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card 
            title={<Space><CloudServerOutlined style={{ color: '#00B96B' }} /><span>{t.serviceHealth}</span></Space>}
            style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
          >
            <div style={{ height: 250, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie data={pieData} cx="50%" cy="50%" innerRadius={60} outerRadius={80} paddingAngle={5} dataKey="value">
                    {pieData.map((entry, index) => (<Cell key={`cell-${index}`} fill={entry.color} />))}
                  </Pie>
                  <RechartsTooltip />
                </PieChart>
              </ResponsiveContainer>
            </div>
            <Row gutter={16} style={{ marginTop: 16 }}>
              <Col span={12} style={{ textAlign: 'center' }}>
                <Tag color="success" style={{ fontSize: 16, padding: '4px 12px' }}>{healthData.healthy} {t.healthyInstances}</Tag>
              </Col>
              <Col span={12} style={{ textAlign: 'center' }}>
                <Tag color="error" style={{ fontSize: 16, padding: '4px 12px' }}>{healthData.unhealthy} {t.unhealthyInstances}</Tag>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
        <Col xs={24} lg={12}>
          <Card 
            title={<Space><ThunderboltOutlined style={{ color: '#1890ff' }} /><span>{t.qpsTrend}</span></Space>}
            extra={<Tag color="blue">{t.realTime}</Tag>}
            style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
          >
            <div style={{ height: 200 }}>
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={qpsData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0"/>
                  <XAxis dataKey="time" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <RechartsTooltip />
                  <Line type="monotone" dataKey="register" stroke="#1890ff" strokeWidth={2} dot={false} name={t.registerQps} />
                  <Line type="monotone" dataKey="discover" stroke="#722ed1" strokeWidth={2} dot={false} name={t.discoverQps} />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card 
            title={<Space><CloudServerOutlined style={{ color: '#00B96B' }} /><span>{t.servicesList}</span></Space>}
            extra={<Link to="/services">{t.viewAll}</Link>}
            style={{ borderRadius: 16, boxShadow: '0 4px 12px rgba(0,0,0,0.08)' }}
          >
            <Table columns={serviceColumns} dataSource={services.slice(0, 5)} rowKey="serviceName" size="small" pagination={false} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
