<template>
  <div class="metrics-view">
    <MetricsDashboard />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import MetricsDashboard from '@/components/metrics/MetricsDashboard.vue'

onMounted(() => {
  console.log('메트릭 화면이 마운트되었습니다')
})
</script>

<style scoped>
.metrics-view {
  height: 100%;
  padding: 20px;
}
</style>