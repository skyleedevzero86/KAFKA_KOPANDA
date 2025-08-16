import { mount } from '@vue/test-utils'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import LineChart from '@/components/charts/LineChart.vue'

// Mock Chart.js
const mockChart = {
  data: {},
  options: {},
  update: vi.fn(),
  destroy: vi.fn()
}

vi.mock('chart.js', () => ({
  Chart: vi.fn(() => mockChart),
  registerables: []
}))

describe('LineChart', () => {
  const mockData = {
    labels: ['Jan', 'Feb', 'Mar', 'Apr'],
    datasets: [{
      label: 'Sales',
      data: [10, 20, 15, 25],
      borderColor: '#409EFF',
      backgroundColor: 'rgba(64, 158, 255, 0.1)',
      tension: 0.4
    }]
  }

  const mockOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'bottom'
      }
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('라인 차트를 렌더링할 수 있다', () => {
    const wrapper = mount(LineChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    expect(wrapper.find('.line-chart').exists()).toBe(true)
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('props로 전달된 데이터를 차트에 전달한다', () => {
    mount(LineChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    expect(mockChart.data).toEqual(mockData)
    expect(mockChart.options).toEqual(mockOptions)
  })

  it('데이터가 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(LineChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const newData = {
      labels: ['May', 'Jun', 'Jul', 'Aug'],
      datasets: [{
        label: 'Revenue',
        data: [30, 35, 40, 45],
        borderColor: '#67C23A',
        backgroundColor: 'rgba(103, 194, 58, 0.1)',
        tension: 0.4
      }]
    }

    await wrapper.setProps({ data: newData })

    expect(mockChart.update).toHaveBeenCalled()
    expect(mockChart.data).toEqual(newData)
  })

  it('옵션이 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(LineChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const newOptions = {
      responsive: false,
      maintainAspectRatio: true,
      plugins: {
        legend: {
          position: 'top'
        }
      }
    }

    await wrapper.setProps({ options: newOptions })

    expect(mockChart.update).toHaveBeenCalled()
  })

  it('컴포넌트가 언마운트되면 차트를 정리한다', () => {
    const wrapper = mount(LineChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    wrapper.unmount()

    expect(mockChart.destroy).toHaveBeenCalled()
  })

  it('기본 옵션을 사용한다', () => {
    mount(LineChart, {
      props: {
        data: mockData
      }
    })

    expect(mockChart.options).toMatchObject({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'top'
        }
      },
      scales: {
        y: {
          beginAtZero: true
        }
      }
    })
  })

  it('라인 차트 높이를 올바르게 설정한다', () => {
    const wrapper = mount(LineChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const chartElement = wrapper.find('.line-chart')
    expect(chartElement.attributes('style')).toContain('height: 300px')
  })

  it('여러 데이터셋을 올바르게 처리한다', () => {
    const multiDatasetData = {
      labels: ['Jan', 'Feb', 'Mar'],
      datasets: [
        {
          label: 'Sales',
          data: [10, 20, 30],
          borderColor: '#409EFF',
          backgroundColor: 'rgba(64, 158, 255, 0.1)'
        },
        {
          label: 'Expenses',
          data: [5, 15, 25],
          borderColor: '#F56C6C',
          backgroundColor: 'rgba(245, 108, 108, 0.1)'
        }
      ]
    }

    mount(LineChart, {
      props: {
        data: multiDatasetData,
        options: mockOptions
      }
    })

    expect(mockChart.data.datasets).toHaveLength(2)
    expect(mockChart.data.datasets[0].label).toBe('Sales')
    expect(mockChart.data.datasets[1].label).toBe('Expenses')
  })

  it('tension 속성을 올바르게 처리한다', () => {
    const dataWithTension = {
      labels: ['Jan', 'Feb', 'Mar'],
      datasets: [{
        label: 'Smooth Line',
        data: [10, 20, 30],
        tension: 0.8
      }]
    }

    mount(LineChart, {
      props: {
        data: dataWithTension,
        options: mockOptions
      }
    })

    expect(mockChart.data.datasets[0].tension).toBe(0.8)
  })

  it('빈 데이터를 올바르게 처리한다', () => {
    const emptyData = {
      labels: [],
      datasets: [{
        label: 'Empty',
        data: [],
        borderColor: '#409EFF'
      }]
    }

    mount(LineChart, {
      props: {
        data: emptyData,
        options: mockOptions
      }
    })

    expect(mockChart.data.labels).toEqual([])
    expect(mockChart.data.datasets[0].data).toEqual([])
  })
})
