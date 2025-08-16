import { describe, it, expect, beforeEach, vi } from 'vitest'
import { ApiService } from '@/services/api'

// Mock axios
const mockAxios = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  interceptors: {
    request: {
      use: vi.fn()
    },
    response: {
      use: vi.fn()
    }
  }
}

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mockAxios)
  }
}))

describe('ApiService', () => {
  let apiService: ApiService

  beforeEach(() => {
    vi.clearAllMocks()
    apiService = new ApiService()
  })

  describe('생성자', () => {
    it('ApiService 인스턴스를 생성할 수 있다', () => {
      expect(apiService).toBeInstanceOf(ApiService)
    })

    it('기본 설정으로 axios 인스턴스를 생성한다', () => {
      expect(mockAxios.interceptors.request.use).toHaveBeenCalled()
      expect(mockAxios.interceptors.response.use).toHaveBeenCalled()
    })
  })

  describe('GET 요청', () => {
    it('GET 요청을 성공적으로 처리할 수 있다', async () => {
      const mockResponse = { data: { message: 'success' } }
      mockAxios.get.mockResolvedValue(mockResponse)

      const result = await apiService.get('/test')

      expect(mockAxios.get).toHaveBeenCalledWith('/test', undefined)
      expect(result).toEqual({ message: 'success' })
    })

    it('파라미터와 함께 GET 요청을 처리할 수 있다', async () => {
      const mockResponse = { data: { items: ['item1', 'item2'] } }
      const params = { page: 1, size: 10 }
      mockAxios.get.mockResolvedValue(mockResponse)

      const result = await apiService.get('/items', params)

      expect(mockAxios.get).toHaveBeenCalledWith('/items', { params })
      expect(result).toEqual({ items: ['item1', 'item2'] })
    })

    it('GET 요청 실패 시 에러를 던진다', async () => {
      const error = new Error('Network error')
      mockAxios.get.mockRejectedValue(error)

      await expect(apiService.get('/test')).rejects.toThrow('Network error')
    })
  })

  describe('POST 요청', () => {
    it('POST 요청을 성공적으로 처리할 수 있다', async () => {
      const mockResponse = { data: { id: 1, name: 'test' } }
      const requestData = { name: 'test' }
      mockAxios.post.mockResolvedValue(mockResponse)

      const result = await apiService.post('/items', requestData)

      expect(mockAxios.post).toHaveBeenCalledWith('/items', requestData)
      expect(result).toEqual({ id: 1, name: 'test' })
    })

    it('데이터 없이 POST 요청을 처리할 수 있다', async () => {
      const mockResponse = { data: { success: true } }
      mockAxios.post.mockResolvedValue(mockResponse)

      const result = await apiService.post('/test')

      expect(mockAxios.post).toHaveBeenCalledWith('/test', undefined)
      expect(result).toEqual({ success: true })
    })

    it('POST 요청 실패 시 에러를 던진다', async () => {
      const error = new Error('Validation error')
      mockAxios.post.mockRejectedValue(error)

      await expect(apiService.post('/items', {})).rejects.toThrow('Validation error')
    })
  })

  describe('PUT 요청', () => {
    it('PUT 요청을 성공적으로 처리할 수 있다', async () => {
      const mockResponse = { data: { id: 1, name: 'updated' } }
      const requestData = { name: 'updated' }
      mockAxios.put.mockResolvedValue(mockResponse)

      const result = await apiService.put('/items/1', requestData)

      expect(mockAxios.put).toHaveBeenCalledWith('/items/1', requestData)
      expect(result).toEqual({ id: 1, name: 'updated' })
    })

    it('데이터 없이 PUT 요청을 처리할 수 있다', async () => {
      const mockResponse = { data: { success: true } }
      mockAxios.put.mockResolvedValue(mockResponse)

      const result = await apiService.put('/test')

      expect(mockAxios.put).toHaveBeenCalledWith('/test', undefined)
      expect(result).toEqual({ success: true })
    })

    it('PUT 요청 실패 시 에러를 던진다', async () => {
      const error = new Error('Update failed')
      mockAxios.put.mockRejectedValue(error)

      await expect(apiService.put('/items/1', {})).rejects.toThrow('Update failed')
    })
  })

  describe('DELETE 요청', () => {
    it('DELETE 요청을 성공적으로 처리할 수 있다', async () => {
      const mockResponse = { data: { success: true } }
      mockAxios.delete.mockResolvedValue(mockResponse)

      const result = await apiService.delete('/items/1')

      expect(mockAxios.delete).toHaveBeenCalledWith('/items/1')
      expect(result).toEqual({ success: true })
    })

    it('DELETE 요청 실패 시 에러를 던진다', async () => {
      const error = new Error('Delete failed')
      mockAxios.delete.mockRejectedValue(error)

      await expect(apiService.delete('/items/1')).rejects.toThrow('Delete failed')
    })
  })

  describe('인터셉터', () => {
    it('요청 인터셉터가 설정된다', () => {
      expect(mockAxios.interceptors.request.use).toHaveBeenCalled()
    })

    it('응답 인터셉터가 설정된다', () => {
      expect(mockAxios.interceptors.response.use).toHaveBeenCalled()
    })
  })

  describe('에러 처리', () => {
    it('네트워크 에러를 처리할 수 있다', async () => {
      const networkError = new Error('Network Error')
      mockAxios.get.mockRejectedValue(networkError)

      await expect(apiService.get('/test')).rejects.toThrow('Network Error')
    })

    it('HTTP 에러를 처리할 수 있다', async () => {
      const httpError = {
        response: {
          status: 404,
          data: { message: 'Not Found' }
        }
      }
      mockAxios.get.mockRejectedValue(httpError)

      await expect(apiService.get('/test')).rejects.toEqual(httpError)
    })

    it('타임아웃 에러를 처리할 수 있다', async () => {
      const timeoutError = new Error('timeout of 5000ms exceeded')
      mockAxios.get.mockRejectedValue(timeoutError)

      await expect(apiService.get('/test')).rejects.toThrow('timeout of 5000ms exceeded')
    })
  })

  describe('응답 데이터 처리', () => {
    it('응답 데이터를 올바르게 추출한다', async () => {
      const mockResponse = {
        data: {
          items: ['item1', 'item2'],
          total: 2
        },
        status: 200,
        headers: {}
      }
      mockAxios.get.mockResolvedValue(mockResponse)

      const result = await apiService.get('/items')

      expect(result).toEqual({
        items: ['item1', 'item2'],
        total: 2
      })
    })

    it('빈 응답 데이터를 처리할 수 있다', async () => {
      const mockResponse = { data: null }
      mockAxios.get.mockResolvedValue(mockResponse)

      const result = await apiService.get('/empty')

      expect(result).toBeNull()
    })
  })

  describe('URL 처리', () => {
    it('절대 URL을 올바르게 처리한다', async () => {
      const mockResponse = { data: { success: true } }
      mockAxios.get.mockResolvedValue(mockResponse)

      await apiService.get('/api/v1/items')

      expect(mockAxios.get).toHaveBeenCalledWith('/api/v1/items', undefined)
    })

    it('쿼리 파라미터를 올바르게 처리한다', async () => {
      const mockResponse = { data: { success: true } }
      const params = { search: 'test', page: 1 }
      mockAxios.get.mockResolvedValue(mockResponse)

      await apiService.get('/search', params)

      expect(mockAxios.get).toHaveBeenCalledWith('/search', { params })
    })
  })
})
