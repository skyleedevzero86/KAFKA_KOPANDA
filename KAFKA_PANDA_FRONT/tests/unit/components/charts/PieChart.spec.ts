import { mount } from '@vue/test-utils'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import PieChart from '@/components/charts/PieChart.vue'

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

describe('PieChart', () => {
  const mockData = {
    labels: ['Red', 'Blue', 'Yellow', 'Green'],
    datasets: [{
      data: [12, 19, 3, 5],
      backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0'],
      borderColor: ['#FF6384', '#36A2EB', '#FFCE56', '#4BC0C0']
    }]
  }

  const mockOptions = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        position: 'right'
      }
    }
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('파이 차트를 렌더링할 수 있다', () => {
    const wrapper = mount(PieChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    expect(wrapper.find('.pie-chart').exists()).toBe(true)
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('props로 전달된 데이터를 차트에 전달한다', () => {
    mount(PieChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    expect(mockChart.data).toEqual(mockData)
    expect(mockChart.options).toEqual(mockOptions)
  })

  it('데이터가 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(PieChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const newData = {
      labels: ['Purple', 'Orange', 'Pink'],
      datasets: [{
        data: [8, 15, 12],
        backgroundColor: ['#9B59B6', '#E67E22', '#E91E63'],
        borderColor: ['#9B59B6', '#E67E22', '#E91E63']
      }]
    }

    await wrapper.setProps({ data: newData })

    expect(mockChart.update).toHaveBeenCalled()
    expect(mockChart.data).toEqual(newData)
  })

  it('옵션이 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(PieChart, {
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
    const wrapper = mount(PieChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    wrapper.unmount()

    expect(mockChart.destroy).toHaveBeenCalled()
  })

  it('기본 옵션을 사용한다', () => {
    mount(PieChart, {
      props: {
        data: mockData
      }
    })

    expect(mockChart.options).toMatchObject({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: 'bottom'
        }
      }
    })
  })

  it('파이 차트 높이를 올바르게 설정한다', () => {
    const wrapper = mount(PieChart, {
      props: {
        data: mockData,
        options: mockOptions
      }
    })

    const chartElement = wrapper.find('.pie-chart')
    expect(chartElement.attributes('style')).toContain('height: 300px')
  })

  it('여러 색상을 올바르게 처리한다', () => {
    const colorfulData = {
      labels: ['Category 1', 'Category 2', 'Category 3'],
      datasets: [{
        data: [30, 40, 30],
        backgroundColor: ['#FF6B6B', '#4ECDC4', '#45B7D1'],
        borderColor: ['#FF6B6B', '#4ECDC4', '#45B7D1']
      }]
    }

    mount(PieChart, {
      props: {
        data: colorfulData,
        options: mockOptions
      }
    })

    expect(mockChart.data.datasets[0].backgroundColor).toHaveLength(3)
    expect(mockChart.data.datasets[0].backgroundColor[0]).toBe('#FF6B6B')
  })

  it('빈 데이터를 올바르게 처리한다', () => {
    const emptyData = {
      labels: [],
      datasets: [{
        data: [],
        backgroundColor: [],
        borderColor: []
      }]
    }

    mount(PieChart, {
      props: {
        data: emptyData,
        options: mockOptions
      }
    })

    expect(mockChart.data.labels).toEqual([])
    expect(mockChart.data.datasets[0].data).toEqual([])
  })

  it('단일 데이터 포인트를 올바르게 처리한다', () => {
    const singleData = {
      labels: ['Single'],
      datasets: [{
        data: [100],
        backgroundColor: ['#409EFF'],
        borderColor: ['#409EFF']
      }]
    }

    mount(PieChart, {
      props: {
        data: singleData,
        options: mockOptions
      }
    })

    expect(mockChart.data.labels).toEqual(['Single'])
    expect(mockChart.data.datasets[0].data).toEqual([100])
  })

  it('큰 데이터셋을 올바르게 처리한다', () => {
    const largeData = {
      labels: Array.from({ length: 20 }, (_, i) => `Category ${i + 1}`),
      datasets: [{
        data: Array.from({ length: 20 }, (_, i) => Math.floor(Math.random() * 100)),
        backgroundColor: Array.from({ length: 20 }, () => '#409EFF'),
        borderColor: Array.from({ length: 20 }, () => '#409EFF')
      }]
    }

    mount(PieChart, {
      props: {
        data: largeData,
        options: mockOptions
      }
    })

    expect(mockChart.data.labels).toHaveLength(20)
    expect(mockChart.data.datasets[0].data).toHaveLength(20)
  })

  it('소수점 데이터를 올바르게 처리한다', () => {
    const decimalData = {
      labels: ['Small', 'Medium', 'Large'],
      datasets: [{
        data: [12.5, 33.7, 53.8],
        backgroundColor: ['#FF6384', '#36A2EB', '#FFCE56'],
        borderColor: ['#FF6384', '#36A2EB', '#FFCE56']
      }]
    }

    mount(PieChart, {
      props: {
        data: decimalData,
        options: mockOptions
      }
    })

    expect(mockChart.data.datasets[0].data).toEqual([12.5, 33.7, 53.8])
  })
})
