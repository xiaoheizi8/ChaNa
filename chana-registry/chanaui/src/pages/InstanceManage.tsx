import React, { useState, useEffect } from 'react';
import { Card, Table, Tag, Button, Space, Modal, Form, Input, InputNumber, Select, message, Typography } from 'antd';
import { PlusOutlined, DeleteOutlined, EditOutlined } from '@ant-design/icons';
import { apiService, ServiceInstance } from '../services/api';

const { Title } = Typography;

const InstanceManage: React.FC = () => {
  const [instances, setInstances] = useState<ServiceInstance[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  const fetchInstances = async () => {
    setLoading(true);
    try {
      const services = await apiService.getServices();
      const allInstances: ServiceInstance[] = [];
      
      for (const service of services) {
        const detail = await apiService.getServiceDetail(service.serviceName, service.namespace);
        allInstances.push(...detail.instances);
      }
      
      setInstances(allInstances);
    } catch (error) {
      message.error('Failed to fetch instances');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInstances();
  }, []);

  const handleRegister = async (values: any) => {
    try {
      await apiService.registerService({
        ...values,
        instanceId: `instance-${Date.now()}`,
        healthy: true,
        registrationTime: Date.now(),
        lastHeartbeatTime: Date.now(),
      });
      message.success('Instance registered successfully');
      setModalVisible(false);
      form.resetFields();
      fetchInstances();
    } catch (error) {
      message.error('Failed to register instance');
    }
  };

  const handleDeregister = async (instance: ServiceInstance) => {
    Modal.confirm({
      title: 'Confirm Deregister',
      content: `Are you sure you want to deregister ${instance.instanceId}?`,
      onOk: async () => {
        try {
          await apiService.deregisterService(
            instance.instanceId,
            instance.serviceName,
            instance.namespace || 'default'
          );
          message.success('Instance deregistered successfully');
          fetchInstances();
        } catch (error) {
          message.error('Failed to deregister instance');
        }
      },
    });
  };

  const columns = [
    {
      title: 'Instance ID',
      dataIndex: 'instanceId',
      key: 'instanceId',
      render: (text: string) => <Tag>{text.substring(0, 12)}...</Tag>,
    },
    {
      title: 'Service',
      dataIndex: 'serviceName',
      key: 'serviceName',
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
      title: 'Action',
      key: 'action',
      render: (_: any, record: ServiceInstance) => (
        <Space>
          <Button
            type="text"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDeregister(record)}
          >
            Deregister
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Title level={3} style={{ margin: 0 }}>Instance Management</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
          Register Instance
        </Button>
      </Space>

      <Card>
        <Table
          columns={columns}
          dataSource={instances}
          rowKey="instanceId"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>

      <Modal
        title="Register New Instance"
        open={modalVisible}
        onCancel={() => setModalVisible(false)}
        onOk={() => form.submit()}
      >
        <Form form={form} layout="vertical" onFinish={handleRegister}>
          <Form.Item name="serviceName" label="Service Name" rules={[{ required: true }]}>
            <Input placeholder="e.g., order-service" />
          </Form.Item>
          <Form.Item name="host" label="Host" rules={[{ required: true }]}>
            <Input placeholder="e.g., 192.168.1.100" />
          </Form.Item>
          <Form.Item name="port" label="Port" rules={[{ required: true }]}>
            <InputNumber min={1} max={65535} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="namespace" label="Namespace" initialValue="default">
            <Select>
              <Select.Option value="default">default</Select.Option>
              <Select.Option value="production">production</Select.Option>
              <Select.Option value="test">test</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="weight" label="Weight" initialValue={100}>
            <InputNumber min={1} max={1000} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="version" label="Version" initialValue="1.0.0">
            <Input placeholder="e.g., 1.0.0" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default InstanceManage;
