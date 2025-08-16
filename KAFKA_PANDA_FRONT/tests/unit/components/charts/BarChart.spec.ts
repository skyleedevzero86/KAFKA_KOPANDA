import { mount } from '@vue/test-utils'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import BarChart from '@/components/charts/BarChart.vue'

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

describe('BarChart', () => {
  const mockData = {
    labels: ['Jan', 'Feb', 'Mar'],
    datasets: [{
      label: 'Sales',
      data: [10, 20, 30],
      backgroundColor: ['#ff0000', '#00ff00', '#0000ff']
    }]
  }

  const mockOptions = {
    responsive: true,
    maintainAspectRatio: false
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('차트를 렌더링할 수 있다', () => {
    const wrapper = mount(BarChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    expect(wrapper.find('.bar-chart').exists()).toBe(true)
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('props로 전달된 데이터를 차트에 전달한다', () => {
    mount(BarChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    expect(mockChart.data).toEqual(mockData)
    expect(mockChart.options).toEqual(mockOptions)
  })

  it('데이터가 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(BarChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const newData = {
      labels: ['Apr', 'May', 'Jun'],
      datasets: [{
        label: 'Revenue',
        data: [40, 50, 60],
        backgroundColor: ['#ff0000', '#00ff00', '#0000ff']
      }]
    }

    await wrapper.setProps({ data: newData })

    expect(mockChart.update).toHaveBeenCalled()
  })

  it('옵션이 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(BarChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const newOptions = {
      responsive: false,
      maintainAspectRatio: true
    }

    await wrapper.setProps({ options: newOptions })

    expect(mockChart.update).toHaveBeenCalled()
  })

  it('컴포넌트가 언마운트되면 차트를 정리한다', () => {
    const wrapper = mount(BarChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    wrapper.unmount()

    expect(mockChart.destroy).toHaveBeenCalled()
  })

  it('기본 옵션을 사용한다', () => {
    mount(BarChart, {
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

  it('차트 높이를 올바르게 설정한다', () => {
    const wrapper = mount(BarChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const chartElement = wrapper.find('.bar-chart')
    expect(chartElement.attributes('style')).toContain('height: 300px')
  })
})
