<template>
  <div class="gauge-chart">
    <canvas ref="chartRef"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

interface Props {
  value: number
  max: number
  label: string
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  color: '#409EFF'
})

const chartRef = ref<HTMLCanvasElement>()
let chart: Chart | null = null

const createGaugeData = () => {
  const percentage = (props.value / props.max) * 100
  return {
    labels: [props.label],
    datasets: [{
      data: [percentage, 100 - percentage],
      backgroundColor: [props.color, '#f0f0f0'],
      borderWidth: 0
    }]
  }
}

const createGaugeOptions = () => ({
  responsive: true,
  maintainAspectRatio: false,
  cutout: '80%',
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      callbacks: {
        label: () => `${props.value} / ${props.max}`
      }
    }
  }
})

onMounted(() => {
  if (chartRef.value) {
    const ctx = chartRef.value.getContext('2d')
    if (ctx) {
      chart = new Chart(ctx, {
        type: 'doughnut',
        data: createGaugeData(),
        options: createGaugeOptions()
      })
    }
  }
})

watch(() => props.value, () => {
  if (chart) {
    chart.data = createGaugeData()
    chart.options = createGaugeOptions()
    chart.update()
  }
})

watch(() => props.max, () => {
  if (chart) {
    chart.data = createGaugeData()
    chart.options = createGaugeOptions()
    chart.update()
  }
})

onUnmounted(() => {
  if (chart) {
    chart.destroy()
  }
})
</script>

<style scoped>
.gauge-chart {
  position: relative;
  height: 200px;
  text-align: center;
}
</style>