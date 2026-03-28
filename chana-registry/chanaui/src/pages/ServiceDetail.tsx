import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card, Table, Tag, Button, Space, Typography, Descriptions, Spin } from 'antd';
import { ArrowLeftOutlined, SyncOutlined, CloudServerOutlined } from '@ant-design/icons';
import { apiService, ServiceInstance } from '../services/api';
import { useI18n } from '../i18n';

const { Title, Text } = Typography;

const ServiceDetail: React.FC = () => {
  const { t } = useI18n();
  const { serviceName } = useParams<{ serviceName: string }>();
  const [loading, setLoading] = useState(true);
  const [instances, setInstances] = useState<ServiceInstance[]>([]);
  const [namespace] = useState('default');

  const fetchDetail = async () => {
    if (!serviceName) return;
    setLoading(true);
    try {
      const detail = await apiService.getServiceDetail(serviceName, namespace);
      setInstances(detail?.instances || []);
    } catch (error) {
      console.error('Failed to fetch service detail', error);
      setInstances([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [serviceName]);

  const columns = [
    { title: t.instanceId, dataIndex: 'instanceId', key: 'instanceId', render: (text: string) => <Text code>{text?.substring(0, 8)}...</Text> },
    { title: t.host, dataIndex: 'host', key: 'host' },
    { title: t.port, dataIndex: 'port', key: 'port' },
    { title: t.status, dataIndex: 'healthy', key: 'healthy', render: (healthy: boolean) => <Tag color={healthy ? 'green' : 'red'}>{healthy ? t.healthy : t.unhealthy}</Tag> },
    { title: t.weight, dataIndex: 'weight', key: 'weight' },
    { title: t.instanceVersion, dataIndex: 'version', key: 'version', render: (v: string) => <Tag color="blue">{v}</Tag> },
    { title: t.lastCheck, dataIndex: 'lastHeartbeatTime', key: 'lastHeartbeatTime', render: (time: number) => time ? new Date(time).toLocaleString() : '-' },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ marginBottom: 16 }}>
        <Link to="/services"><Button icon={<ArrowLeftOutlined />}>Back</Button></Link>
        <Button icon={<SyncOutlined />} onClick={fetchDetail}>{t.refresh}</Button>
      </Space>
      <Card>
        <Descriptions title={<Space><CloudServerOutlined />{serviceName}</Space>} bordered>
          <Descriptions.Item label={t.serviceName}>{serviceName}</Descriptions.Item>
          <Descriptions.Item label={t.namespace}>{namespace}</Descriptions.Item>
          <Descriptions.Item label={t.instanceCount}>{instances.length}</Descriptions.Item>
          <Descriptions.Item label={t.healthy}>{instances.filter(i => i.healthy).length}</Descriptions.Item>
          <Descriptions.Item label={t.unhealthy}>{instances.filter(i => !i.healthy).length}</Descriptions.Item>
        </Descriptions>
      </Card>
      <Card title={t.servicesList} style={{ marginTop: 16 }}>
        <Spin spinning={loading}>
          <Table columns={columns} dataSource={instances} rowKey="instanceId" pagination={{ pageSize: 10 }} />
        </Spin>
      </Card>
    </div>
  );
};

export default ServiceDetail;
