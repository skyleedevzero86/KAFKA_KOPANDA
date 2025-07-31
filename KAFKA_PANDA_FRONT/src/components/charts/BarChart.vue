<template>
  <div class="bar-chart">
    <canvas ref="chartRef"></canvas>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Chart, registerables } from 'chart.js'

Chart.register(...registerables)

interface Props {
  data: {
    labels: string[]
    datasets: {
      label: string
      data: number[]
      backgroundColor?: string[]
      borderColor?: string[]
    }[]
  }
  options?: any
}

const props = withDefaults(defineProps<Props>(), {
  options: () => ({
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

const chartRef = ref<HTMLCanvasElement>()
let chart: Chart | null = null

onMounted(() => {
  if (chartRef.value) {
    const ctx = chartRef.value.getContext('2d')
    if (ctx) {
      chart = new Chart(ctx, {
        type: 'bar',
        data: props.data,
        options: props.options
      })
    }
  }
})

watch(() => props.data, (newData) => {
  if (chart) {
   
    chart.data = { ...newData };
    chart.update()
  }
}, { deep: true })

watch(() => props.options, (newOptions) => {
  if (chart) {
    chart.options = { ...chart.options, ...newOptions }
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
.bar-chart {
  position: relative;
  height: 300px;
}
</style>