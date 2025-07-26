<template>
  <div ref="chartContainer" class="chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Chart } from '@antv/g2'

interface BarChartData {
  category: string
  value: number
  color?: string
}

interface Props {
  data: BarChartData[]
  title?: string
  height?: number
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  height: 300,
  color: '#409EFF'
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

  chart.scale('value', {
    nice: true
  })

  chart.tooltip({
    showCrosshairs: true,
    shared: true
  })

  chart.axis('category', {
    title: null
  })

  chart.axis('value', {
    title: null
  })

  chart.interval()
    .position('category*value')
    .color(props.color)
    .style({
      fillOpacity: 0.8
    })

  if (props.title) {
    chart.annotation().text({
      position: ['50%', '0%'],
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