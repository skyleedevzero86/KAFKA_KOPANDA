<template>
  <div class="metrics-view">
    <MetricsDashboard />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import MetricsDashboard from '@/components/metrics/MetricsDashboard.vue'

onMounted(() => {
  console.log('Metrics 컴포넌트 마운트됨')
})
</script>

<style scoped>
.metrics-view {
  height: 100%;
  padding: 20px;
}
</style>