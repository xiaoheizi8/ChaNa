import React from 'react';
import { Card, Form, Input, InputNumber, Switch, Button, Typography, Space, Divider, message } from 'antd';

const { Title, Text } = Typography;

const Settings: React.FC = () => {
  const [form] = Form.useForm();

  const handleSave = (values: any) => {
    console.log('Settings saved:', values);
    message.success('Settings saved successfully');
  };

  return (
    <div style={{ padding: 24 }}>
      <Title level={3}>Settings</Title>

      <Card title="Server Configuration" style={{ marginBottom: 16 }}>
        <Form
          form={form}
          layout="vertical"
          initialValues={{
            nettyPort: 9999,
            httpPort: 9998,
            bossThreads: 2,
            workerThreads: 16,
          }}
          onFinish={handleSave}
        >
          <Form.Item label="Netty Port" name="nettyPort">
            <InputNumber min={1} max={65535} />
          </Form.Item>
          <Form.Item label="HTTP API Port" name="httpPort">
            <InputNumber min={1} max={65535} />
          </Form.Item>
          <Form.Item label="Boss Threads" name="bossThreads">
            <InputNumber min={1} max={32} />
          </Form.Item>
          <Form.Item label="Worker Threads" name="workerThreads">
            <InputNumber min={1} max={128} />
          </Form.Item>
          <Button type="primary" htmlType="submit">Save</Button>
        </Form>
      </Card>

      <Card title="Health Check Configuration" style={{ marginBottom: 16 }}>
        <Form layout="vertical" initialValues={{}} onFinish={handleSave}>
          <Form.Item label="Health Check Interval (seconds)" name="healthCheckInterval">
            <InputNumber min={1} max={60} defaultValue={5} />
          </Form.Item>
          <Form.Item label="Heartbeat Timeout (milliseconds)" name="heartbeatTimeout">
            <InputNumber min={1000} max={60000} defaultValue={15000} />
          </Form.Item>
          <Form.Item label="Protection Threshold" name="protectionThreshold" extra="Percentage of healthy instances below which protection mode activates">
            <InputNumber min={0} max={1} step={0.1} defaultValue={0.2} />
          </Form.Item>
          <Button type="primary" htmlType="submit">Save</Button>
        </Form>
      </Card>

      <Card title="Cache Configuration">
        <Form layout="vertical" initialValues={{}} onFinish={handleSave}>
          <Form.Item label="L1 Cache TTL (seconds)" name="l1CacheTtl">
            <InputNumber min={1} max={60} defaultValue={5} />
          </Form.Item>
          <Form.Item label="L2 Cache TTL (seconds)" name="l2CacheTtl">
            <InputNumber min={10} max={300} defaultValue={30} />
          </Form.Item>
          <Form.Item label="L1 Cache Size" name="l1CacheSize">
            <InputNumber min={100} max={100000} defaultValue={10000} />
          </Form.Item>
          <Button type="primary" htmlType="submit">Save</Button>
        </Form>
      </Card>
    </div>
  );
};

export default Settings;
