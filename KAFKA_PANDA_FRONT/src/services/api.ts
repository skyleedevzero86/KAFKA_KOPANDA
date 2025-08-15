import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

export class ApiService {
  private api: AxiosInstance

  constructor() {
    this.api = axios.create({
      baseURL: import.meta.env['VITE_API_BASE_URL'] || 'http://localhost:8080',
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json'
      },
      withCredentials: false
    })

    this.setupInterceptors()
  }

  private setupInterceptors() {

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


    this.api.interceptors.response.use(
      (response: AxiosResponse) => {
        console.log('API 응답:', response.status, response.config.url)
        return response
      },
      (error) => {
        console.error('API 응답 오류:', error.response?.status, error.response?.data)
        
        if (error.code === 'ERR_NETWORK') {
          ElMessage.error('네트워크 오류가 발생했습니다. 서버가 실행 중인지 확인해주세요.')
        } else if (error.response?.status === 403) {
          ElMessage.error('접근이 거부되었습니다. 권한을 확인해주세요.')
        } else if (error.response?.status === 404) {
          ElMessage.error('요청한 리소스를 찾을 수 없습니다.')
        } else if (error.response?.status === 500) {
          ElMessage.error('서버 내부 오류가 발생했습니다.')
        } else {
          const message = error.response?.data?.message || error.message || '서버 오류가 발생했습니다.'
          ElMessage.error(message)
        }
        
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