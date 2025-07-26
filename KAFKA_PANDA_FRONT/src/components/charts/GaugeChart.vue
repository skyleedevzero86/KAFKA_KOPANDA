<template>
  <div ref="chartContainer" class="chart-container"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { Chart } from '@antv/g2'

interface Props {
  value: number
  min?: number
  max?: number
  title?: string
  height?: number
  color?: string
}

const props = withDefaults(defineProps<Props>(), {
  min: 0,
  max: 100,
  height: 200,
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

  const data = [
    { type: 'value', value: props.value }
  ]

  chart.data(data)

  chart.coordinate('polar', {
    startAngle: (-9 / 8) * Math.PI,
    endAngle: (1 / 8) * Math.PI,
    radius: 0.75
  })

  chart.scale('value', {
    min: props.min,
    max: props.max,
    tickCount: 5
  })

  chart.axis('value', {
    line: null,
    label: {
      offset: -36,
      style: {
        fontSize: 12,
        textAlign: 'center',
        textBaseline: 'middle'
      }
    },
    grid: null,
    tickLine: null
  })

  chart.axis('1', false)

  chart.point()
    .position('value*1')
    .shape('pointer')
    .color(props.color)

  chart.annotation().arc({
    start: [props.min, 1],
    end: [props.max, 1],
    style: {
      stroke: '#CBCBCB',
      lineWidth: 18,
      lineDash: null
    }
  })

  chart.annotation().arc({
    start: [props.min, 1],
    end: [props.value, 1],
    style: {
      stroke: props.color,
      lineWidth: 18,
      lineDash: null
    }
  })

  chart.annotation().text({
    position: ['50%', '85%'],
    content: `${props.value}`,
    style: {
      fontSize: 24,
      fontWeight: 'bold',
      textAlign: 'center'
    }
  })

  if (props.title) {
    chart.annotation().text({
      position: ['50%', '95%'],
      content: props.title,
      style: {
        fontSize: 12,
        textAlign: 'center'
      }
    })
  }

  chart.render()
}

const updateChart = () => {
  if (chart) {
    chart.destroy()
    initChart()
  }
}

watch(() => [props.value, props.min, props.max], updateChart)

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