import { mount } from '@vue/test-utils'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import GaugeChart from '@/components/charts/GaugeChart.vue'

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

describe('GaugeChart', () => {
  const defaultProps = {
    value: 75,
    max: 100,
    label: 'CPU Usage',
    color: '#409EFF'
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('게이지 차트를 렌더링할 수 있다', () => {
    const wrapper = mount(GaugeChart, {
      props: defaultProps
    })

    expect(wrapper.find('.gauge-chart').exists()).toBe(true)
    expect(wrapper.find('canvas').exists()).toBe(true)
  })

  it('props로 전달된 값을 올바르게 표시한다', () => {
    mount(GaugeChart, {
      props: defaultProps
    })

    expect(mockChart.data).toMatchObject({
      labels: ['CPU Usage'],
      datasets: [{
        data: [75, 25], // value와 (max - value)
        backgroundColor: ['#409EFF', '#f0f0f0'],
        borderWidth: 0
      }]
    })
  })

  it('기본 색상을 사용한다', () => {
    mount(GaugeChart, {
      props: {
        value: 50,
        max: 100,
        label: 'Memory Usage'
      }
    })

    expect(mockChart.data.datasets[0].backgroundColor[0]).toBe('#409EFF')
  })

  it('커스텀 색상을 사용한다', () => {
    const customColor = '#ff0000'
    mount(GaugeChart, {
      props: {
        ...defaultProps,
        color: customColor
      }
    })

    expect(mockChart.data.datasets[0].backgroundColor[0]).toBe(customColor)
  })

  it('값이 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(GaugeChart, {
      props: defaultProps
    })

    await wrapper.setProps({ value: 90 })

    expect(mockChart.update).toHaveBeenCalled()
    expect(mockChart.data.datasets[0].data).toEqual([90, 10])
  })

  it('최대값이 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(GaugeChart, {
      props: defaultProps
    })

    await wrapper.setProps({ max: 200 })

    expect(mockChart.update).toHaveBeenCalled()
    expect(mockChart.data.datasets[0].data).toEqual([75, 125])
  })

  it('라벨이 변경되면 차트를 업데이트한다', async () => {
    const wrapper = mount(GaugeChart, {
      props: defaultProps
    })

    await wrapper.setProps({ label: 'Network Usage' })

    expect(mockChart.update).toHaveBeenCalled()
    expect(mockChart.data.labels[0]).toBe('Network Usage')
  })

  it('게이지 차트 옵션을 올바르게 설정한다', () => {
    mount(GaugeChart, {
      props: defaultProps
    })

    expect(mockChart.options).toMatchObject({
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          display: false
        },
        tooltip: {
          callbacks: {
            label: expect.any(Function)
          }
        }
      },
      cutout: '70%'
    })
  })

  it('툴팁에 올바른 값을 표시한다', () => {
    mount(GaugeChart, {
      props: defaultProps
    })

    const tooltipCallback = mockChart.options.plugins.tooltip.callbacks.label
    const result = tooltipCallback()

    expect(result).toBe('75 / 100')
  })

  it('컴포넌트가 언마운트되면 차트를 정리한다', () => {
    const wrapper = mount(GaugeChart, {
      props: defaultProps
    })

    wrapper.unmount()

    expect(mockChart.destroy).toHaveBeenCalled()
  })

  it('게이지 차트 높이를 올바르게 설정한다', () => {
    const wrapper = mount(GaugeChart, {
      props: defaultProps
    })

    const chartElement = wrapper.find('.gauge-chart')
    expect(chartElement.attributes('style')).toContain('height: 200px')
  })

  it('0% 게이지를 올바르게 표시한다', () => {
    mount(GaugeChart, {
      props: {
        value: 0,
        max: 100,
        label: 'Empty'
      }
    })

    expect(mockChart.data.datasets[0].data).toEqual([0, 100])
  })

  it('100% 게이지를 올바르게 표시한다', () => {
    mount(GaugeChart, {
      props: {
        value: 100,
        max: 100,
        label: 'Full'
      }
    })

    expect(mockChart.data.datasets[0].data).toEqual([100, 0])
  })
})
