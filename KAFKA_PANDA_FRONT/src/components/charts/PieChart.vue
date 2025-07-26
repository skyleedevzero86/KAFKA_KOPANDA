<template>
  <div ref="chartContainer" class="chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Chart } from '@antv/g2'

interface PieChartData {
  type: string
  value: number
  color?: string
}

interface Props {
  data: PieChartData[]
  title?: string
  height?: number
  showLabel?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  height: 300,
  showLabel: true
})

const chartContainer = ref<HTMLElement>()
let chart: Chart | null = null

const initChart = () => {
  if (!chartContainer.value) return

  chart = new Chart({
    container: chartContainer.value,
    autoFit: true,
    height: props.height
  })

  chart.data(props.data)

  chart.coordinate('theta', {
    radius: 0.75
  })

  chart.tooltip({
    show: true,
    formatter: (datum) => {
      return { name: datum.type, value: datum.value }
    }
  })

  const interval = chart.interval()
    .position('value')
    .color('type')
    .label('type', {
      content: (data) => {
        if (props.showLabel) {
          return `${data.type}\n${data.value}`
        }
        return ''
      }
    })
    .adjust('stack')

  if (props.title) {
    chart.annotation().text({
      position: ['50%', '50%'],
      content: props.title,
      style: {
        textAlign: 'center',
        fontSize: 14,
        fontWeight: 'bold'
      }
    })
  }

  chart.render()
}

const updateChart = () => {
  if (chart) {
    chart.changeData(props.data)
  }
}

watch(() => props.data, updateChart, { deep: true })

onMounted(() => {
  initChart()
})

onUnmounted(() => {
  if (chart) {
    chart.destroy()
  }
})
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
}
</style>