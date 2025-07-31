<template>
  <div class="gauge-chart">
    <canvas ref="chartRef"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
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

const chartData = computed(() => ({
  labels: [props.label],
  datasets: [{
    data: [props.value, props.max - props.value],
    backgroundColor: [props.color, '#f0f0f0'],
    borderWidth: 0
  }]
}))

const chartOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: false
    },
    tooltip: {
      callbacks: {
        label: () => `${props.value} / ${props.max}`
      }
    }
  },
  cutout: '70%'
}))

onMounted(() => {
  if (chartRef.value) {
    const ctx = chartRef.value.getContext('2d')
    if (ctx) {
      chart = new Chart(ctx, {
        type: 'doughnut',
        data: chartData.value,
        options: chartOptions.value
      })
    }
  }
})

watch(() => props.value, () => {
  if (chart) {
    chart.data = chartData.value
    chart.options = chartOptions.value
    chart.update()
  }
}, { deep: true })

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
}
</style>