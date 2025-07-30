import axios, { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export class ApiService {
  private api: AxiosInstance

  constructor() {
    this.api = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {
    // 요청 인터셉터
    this.api.interceptors.request.use(
      (config) => {
        console.log('API 요청:', config.method?.toUpperCase(), config.url)
        return config
      },
      (error) => {
        console.error('API 요청 오류:', error)
        return Promise.reject(error)
      }
    )

    // 응답 인터셉터
    this.api.interceptors.response.use(
      (response: AxiosResponse) => {
        console.log('API 응답:', response.status, response.config.url)
        return response
      },
      (error) => {
        console.error('API 응답 오류:', error.response?.status, error.response?.data)
        
        const message = error.response?.data?.message || error.message || '서버 오류가 발생했습니다.'
        ElMessage.error(message)
        
        return Promise.reject(error)
      }
    )
  }

  async get<T>(url: string, params?: any): Promise<T> {
    const response = await this.api.get<T>(url, { params })
    return response.data
  }

  async post<T>(url: string, data?: any): Promise<T> {
    const response = await this.api.post<T>(url, data)
    return response.data
  }

  async put<T>(url: string, data?: any): Promise<T> {
    const response = await this.api.put<T>(url, data)
    return response.data
  }

  async delete<T>(url: string): Promise<T> {
    const response = await this.api.delete<T>(url)
    return response.data
  }
}

export const apiService = new ApiService()