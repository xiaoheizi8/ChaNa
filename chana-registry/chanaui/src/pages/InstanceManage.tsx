import React, { useState, useEffect } from 'react';
import { Card, Table, Tag, Button, Space, Modal, Form, Input, InputNumber, Select, Typography } from 'antd';
import { PlusOutlined, DeleteOutlined, AppstoreOutlined } from '@ant-design/icons';
import { apiService, ServiceInstance } from '../services/api';
import { useI18n } from '../i18n';

const { Title } = Typography;

const InstanceManage: React.FC = () => {
  const { t } = useI18n();
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
        allInstances.push(...(detail?.instances || []));
      }
      setInstances(allInstances);
    } catch (error) {
      console.error('Failed to fetch instances', error);
      setInstances([]);
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
      setModalVisible(false);
      form.resetFields();
      fetchInstances();
    } catch (error) {
      console.error('Failed to register instance', error);
    }
  };

  const handleDeregister = async (instance: ServiceInstance) => {
    Modal.confirm({
      title: t.deregisterAction + '?',
      content: `${instance.instanceId}`,
      onOk: async () => {
        try {
          await apiService.deregisterService(instance.instanceId, instance.serviceName, instance.namespace || 'default');
          fetchInstances();
        } catch (error) {
          console.error('Failed to deregister', error);
        }
      },
    });
  };

  const columns = [
    { title: t.instanceId, dataIndex: 'instanceId', key: 'instanceId', render: (text: string) => <Tag>{text?.substring(0, 12)}...</Tag> },
    { title: t.serviceName, dataIndex: 'serviceName', key: 'serviceName' },
    { title: t.host, dataIndex: 'host', key: 'host' },
    { title: t.port, dataIndex: 'port', key: 'port' },
    { title: t.status, dataIndex: 'healthy', key: 'healthy', render: (healthy: boolean) => <Tag color={healthy ? 'green' : 'red'}>{healthy ? t.healthy : t.unhealthy}</Tag> },
    { title: t.weight, dataIndex: 'weight', key: 'weight' },
    { title: t.status, key: 'action', render: (_: any, record: ServiceInstance) => <Button type="text" danger icon={<DeleteOutlined />} onClick={() => handleDeregister(record)}>{t.deregisterAction}</Button> },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ marginBottom: 16, width: '100%', justifyContent: 'space-between' }}>
        <Title level={3} style={{ margin: 0 }}><AppstoreOutlined style={{ marginRight: 8 }} />{t.instanceManage}</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>{t.registerAction}</Button>
      </Space>
      <Card>
        <Table columns={columns} dataSource={instances} rowKey="instanceId" loading={loading} pagination={{ pageSize: 10 }} />
      </Card>
      <Modal title={t.registerAction} open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()}>
        <Form form={form} layout="vertical" onFinish={handleRegister}>
          <Form.Item name="serviceName" label={t.serviceName} rules={[{ required: true }]}><Input placeholder="order-service" /></Form.Item>
          <Form.Item name="host" label={t.host} rules={[{ required: true }]}><Input placeholder="192.168.1.100" /></Form.Item>
          <Form.Item name="port" label={t.port} rules={[{ required: true }]}><InputNumber min={1} max={65535} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="namespace" label={t.namespace} initialValue="default"><Select><Select.Option value="default">default</Select.Option><Select.Option value="production">production</Select.Option><Select.Option value="test">test</Select.Option></Select></Form.Item>
          <Form.Item name="weight" label={t.weight} initialValue={100}><InputNumber min={1} max={1000} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="version" label={t.instanceVersion} initialValue="1.0.0"><Input placeholder="1.0.0" /></Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default InstanceManage;
