import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card, Table, Tag, Button, Space, Typography, Descriptions, Spin, message } from 'antd';
import { ArrowLeftOutlined, SyncOutlined } from '@ant-design/icons';
import { apiService, ServiceInstance } from '../services/api';

const { Title, Text } = Typography;

const ServiceDetail: React.FC = () => {
  const { serviceName } = useParams<{ serviceName: string }>();
  const [loading, setLoading] = useState(true);
  const [instances, setInstances] = useState<ServiceInstance[]>([]);
  const [namespace, setNamespace] = useState('default');

  const fetchDetail = async () => {
    if (!serviceName) return;
    try {
      const detail = await apiService.getServiceDetail(serviceName, namespace);
      setInstances(detail.instances);
    } catch (error) {
      message.error('Failed to fetch service detail');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDetail();
  }, [serviceName, namespace]);

  const columns = [
    {
      title: 'Instance ID',
      dataIndex: 'instanceId',
      key: 'instanceId',
      render: (text: string) => <Text code>{text.substring(0, 8)}...</Text>,
    },
    {
      title: 'Host',
      dataIndex: 'host',
      key: 'host',
    },
    {
      title: 'Port',
      dataIndex: 'port',
      key: 'port',
    },
    {
      title: 'Status',
      dataIndex: 'healthy',
      key: 'healthy',
      render: (healthy: boolean) => (
        <Tag color={healthy ? 'green' : 'red'}>
          {healthy ? 'Healthy' : 'Unhealthy'}
        </Tag>
      ),
    },
    {
      title: 'Weight',
      dataIndex: 'weight',
      key: 'weight',
    },
    {
      title: 'Version',
      dataIndex: 'version',
      key: 'version',
      render: (v: string) => <Tag color="blue">{v}</Tag>,
    },
    {
      title: 'Last Heartbeat',
      dataIndex: 'lastHeartbeatTime',
      key: 'lastHeartbeatTime',
      render: (time: number) => new Date(time).toLocaleString(),
    },
    {
      title: 'Registration Time',
      dataIndex: 'registrationTime',
      key: 'registrationTime',
      render: (time: number) => new Date(time).toLocaleString(),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ marginBottom: 16 }}>
        <Link to="/services">
          <Button icon={<ArrowLeftOutlined />}>Back</Button>
        </Link>
        <Button icon={<SyncOutlined />} onClick={fetchDetail}>
          Refresh
        </Button>
      </Space>

      <Card>
        <Descriptions title={`Service: ${serviceName}`} bordered>
          <Descriptions.Item label="Service Name">{serviceName}</Descriptions.Item>
          <Descriptions.Item label="Namespace">{namespace}</Descriptions.Item>
          <Descriptions.Item label="Total Instances">{instances.length}</Descriptions.Item>
          <Descriptions.Item label="Healthy">
            {instances.filter(i => i.healthy).length}
          </Descriptions.Item>
          <Descriptions.Item label="Unhealthy">
            {instances.filter(i => !i.healthy).length}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card title="Instance List" style={{ marginTop: 16 }}>
        <Spin spinning={loading}>
          <Table
            columns={columns}
            dataSource={instances}
            rowKey="instanceId"
            pagination={{ pageSize: 10 }}
          />
        </Spin>
      </Card>
    </div>
  );
};

export default ServiceDetail;
