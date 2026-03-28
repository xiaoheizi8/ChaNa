import React from 'react';
import { Card, Form, InputNumber, Button, Typography, Space } from 'antd';
import { SettingOutlined, SaveOutlined } from '@ant-design/icons';
import { useI18n } from '../i18n';

const { Title } = Typography;

const Settings: React.FC = () => {
  const { t } = useI18n();
  const [form] = Form.useForm();

  const handleSave = (values: any) => {
    console.log('Settings saved:', values);
  };

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}><SettingOutlined style={{ marginRight: 8 }} />{t.systemSettings}</Title>

      <Card title="Server Configuration" style={{ marginBottom: 16 }}>
        <Form form={form} layout="vertical" initialValues={{ nettyPort: 9999, httpPort: 9998 }} onFinish={handleSave}>
          <Form.Item label={t.nettyPort} name="nettyPort"><InputNumber min={1} max={65535} style={{ width: 200 }} /></Form.Item>
          <Form.Item label={t.httpPort} name="httpPort"><InputNumber min={1} max={65535} style={{ width: 200 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit" icon={<SaveOutlined />}>{t.save}</Button></Form.Item>
        </Form>
      </Card>

      <Card title="Health Check" style={{ marginBottom: 16 }}>
        <Form layout="vertical" initialValues={{ healthCheckInterval: 5, heartbeatTimeout: 15000 }} onFinish={handleSave}>
          <Form.Item label={t.healthCheckInterval} name="healthCheckInterval"><InputNumber min={1} max={60} style={{ width: 200 }} /></Form.Item>
          <Form.Item label={t.heartbeatTimeout} name="heartbeatTimeout"><InputNumber min={1000} max={60000} style={{ width: 200 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit" icon={<SaveOutlined />}>{t.save}</Button></Form.Item>
        </Form>
      </Card>

      <Card title="Cache">
        <Form layout="vertical" initialValues={{ l1CacheTtl: 5, l2CacheTtl: 30 }} onFinish={handleSave}>
          <Form.Item label="L1 Cache TTL (s)" name="l1CacheTtl"><InputNumber min={1} max={60} style={{ width: 200 }} /></Form.Item>
          <Form.Item label="L2 Cache TTL (s)" name="l2CacheTtl"><InputNumber min={10} max={300} style={{ width: 200 }} /></Form.Item>
          <Form.Item><Button type="primary" htmlType="submit" icon={<SaveOutlined />}>{t.save}</Button></Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default Settings;
