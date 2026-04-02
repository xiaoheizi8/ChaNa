import { useState, useEffect } from 'react';
import { Card, Table, Button, Modal, Form, Input, message, Space, Popconfirm, Tag } from 'antd';
import { PlusOutlined, DeleteOutlined, ReloadOutlined } from '@ant-design/icons';
import apiService, { ConfigInfo } from '../services/api';
import { useI18n } from '../i18n';

const { TextArea } = Input;

const ConfigManage: React.FC = () => {
  const { t } = useI18n();
  const [configs, setConfigs] = useState<ConfigInfo[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingConfig, setEditingConfig] = useState<ConfigInfo | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchConfigs();
  }, []);

  const fetchConfigs = async () => {
    setLoading(true);
    try {
      const data = await apiService.getAllConfigs();
      const configList: ConfigInfo[] = Object.entries(data).map(([key, content]) => {
        const parts = key.split('.');
        const dataId = parts[0] || key;
        const group = parts[1] || 'DEFAULT_GROUP';
        return { dataId, group, content };
      });
      setConfigs(configList);
    } catch (error) {
      console.error('Failed to fetch configs:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      await apiService.publishConfig(values);
      message.success(t.configPublishSuccess);
      setModalVisible(false);
      form.resetFields();
      fetchConfigs();
    } catch (error) {
      message.error(t.configPublishFailed);
    }
  };

  const handleDelete = async (dataId: string, group: string) => {
    try {
      await apiService.deleteConfig(dataId, group);
      message.success(t.configDeleteSuccess);
      fetchConfigs();
    } catch (error) {
      message.error(t.configDeleteFailed);
    }
  };

  const handleEdit = (record: ConfigInfo) => {
    setEditingConfig(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const columns = [
    {
      title: t.configDataId,
      dataIndex: 'dataId',
      key: 'dataId',
    },
    {
      title: t.configGroup,
      dataIndex: 'group',
      key: 'group',
      render: (group: string) => <Tag color="blue">{group}</Tag>,
    },
    {
      title: t.configContent,
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: t.actions,
      key: 'action',
      render: (_: any, record: ConfigInfo) => (
        <Space>
          <Button type="link" size="small" onClick={() => handleEdit(record)}>
            {t.edit}
          </Button>
          <Popconfirm
            title={t.confirmDelete}
            onConfirm={() => handleDelete(record.dataId, record.group)}
          >
            <Button type="link" danger size="small">
              <DeleteOutlined />
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Card
      title={t.configManagement}
      extra={
        <Space>
          <Button icon={<ReloadOutlined />} onClick={fetchConfigs}>
            {t.refresh}
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={() => {
            setEditingConfig(null);
            form.resetFields();
            setModalVisible(true);
          }}>
            {t.addConfig}
          </Button>
        </Space>
      }
    >
      <Table
        columns={columns}
        dataSource={configs}
        rowKey={(record) => `${record.dataId}-${record.group}`}
        loading={loading}
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title={editingConfig ? t.editConfig : t.addConfig}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => {
          setModalVisible(false);
          form.resetFields();
        }}
        width={600}
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="dataId"
            label={t.configDataId}
            rules={[{ required: true, message: t.pleaseInput }]}
          >
            <Input placeholder={t.configDataIdPlaceholder} />
          </Form.Item>
          <Form.Item
            name="group"
            label={t.configGroup}
            initialValue="DEFAULT_GROUP"
          >
            <Input placeholder={t.configGroupPlaceholder} />
          </Form.Item>
          <Form.Item
            name="content"
            label={t.configContent}
            rules={[{ required: true, message: t.pleaseInput }]}
          >
            <TextArea rows={8} placeholder={t.configContentPlaceholder} />
          </Form.Item>
        </Form>
      </Modal>
    </Card>
  );
};

export default ConfigManage;
