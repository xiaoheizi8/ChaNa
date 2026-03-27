import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios';
import { message } from 'antd';

interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T;
}

class AxiosClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: 'http://localhost:9998',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    this.client.interceptors.request.use(
      (config) => {
        return config;
      },
      (error: AxiosError) => {
        console.error('Request Error:', error);
        return Promise.reject(error);
      }
    );

    this.client.interceptors.response.use(
      (response: AxiosResponse<ApiResponse>) => {
        const { code, message: msg, data } = response.data;
        if (code !== 200) {
          message.error(msg || 'Request failed');
          return Promise.reject(new Error(msg));
        }
        response.data = data;
        return response;
      },
      (error: AxiosError) => {
        if (error.response) {
          const status = error.response.status;
          switch (status) {
            case 401:
              message.error('Unauthorized, please login');
              break;
            case 403:
              message.error('Access denied');
              break;
            case 404:
              message.error('Resource not found');
              break;
            case 500:
              message.error('Server error');
              break;
            default:
              message.error(error.message || 'Request failed');
          }
        } else if (error.request) {
          message.error('Network error, please check your connection');
        }
        return Promise.reject(error);
      }
    );
  }

  async get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.get<ApiResponse<T>>(url, config);
    return response.data as T;
  }

  async post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.post<ApiResponse<T>>(url, data, config);
    return response.data as T;
  }

  async put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.put<ApiResponse<T>>(url, data, config);
    return response.data as T;
  }

  async delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    const response = await this.client.delete<ApiResponse<T>>(url, config);
    return response.data as T;
  }
}

export const axiosClient = new AxiosClient();
export default axiosClient;
