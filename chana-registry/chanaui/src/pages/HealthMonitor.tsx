import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Table, Tag, Progress, Typography, Space, Badge } from 'antd';
import { 
  CloudServerOutlined, HeartOutlined, WarningOutlined, CheckCircleOutlined 
} from '@ant-design/icons';
import { apiService, HealthStatus, ServiceInfo } from '../services/api';

const { Title, Text } = Typography;

const HealthMonitor: React.FC = () => {
  const [health, setHealth] = useState<HealthStatus | null>(null);
  const [services, setServices] = useState<ServiceInfo[]>([]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [healthData, servicesData] = await Promise.all([
          apiService.getHealth(),
          apiService.getServices()
        ]);
        setHealth(healthData);
        setServices(servicesData);
      } catch (error) {
        console.error('Failed to fetch health data', error);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 3000);
    return () => clearInterval(interval);
  }, []);

  const columns = [
    {
      title: 'Service',
      dataIndex: 'serviceName',
      key: 'serviceName',
      render: (text: string) => (
        <Space>
          <CloudServerOutlined />
          {text}
        </Space>
      ),
    },
    {
      title: 'Namespace',
      dataIndex: 'namespace',
      key: 'namespace',
    },
    {
      title: 'Health Rate',
      key: 'healthRate',
      render: (_: any, record: ServiceInfo) => {
        const rate = record.instanceCount > 0 
          ? (record.healthyCount / record.instanceCount) * 100 
          : 0;
        return (
          <Progress
            percent={rate}
            size="small"
            status={rate === 100 ? 'success' : rate >= 50 ? 'normal' : 'exception'}
            format={(p) => `${p?.toFixed(0)}%`}
          />
        );
      },
    },
    {
      title: 'Instances',
      key: 'instances',
      render: (_: any, record: ServiceInfo) => (
        <Badge
          status={record.unhealthyCount === 0 ? 'success' : 'error'}
          text={`${record.healthyCount}/${record.instanceCount}`}
        />
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>
        <HeartOutlined style={{ marginRight: 8 }} />
        Health Monitor
      </Title>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Healthy Instances"
              value={health?.healthyInstances || 0}
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Unhealthy Instances"
              value={health?.unhealthyInstances || 0}
              prefix={<WarningOutlined style={{ color: '#f5222d' }} />}
              valueStyle={{ color: '#f5222d' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Protection Mode"
              value={health?.protectionMode ? 'Active' : 'Inactive'}
              valueStyle={{ color: health?.protectionMode ? '#fa8c16' : '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total Services"
              value={services.length}
              prefix={<CloudServerOutlined />}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Service Health Status" style={{ marginTop: 16 }}>
        <Table
          columns={columns}
          dataSource={services}
          rowKey="serviceName"
          pagination={false}
        />
      </Card>
    </div>
  );
};

export default HealthMonitor;
