<template>
  <div class="connections-page">
    <div class="page-header">
      <h2>연결 관리</h2>
      <p>Kafka 클러스터 연결을 관리합니다.</p>
    </div>
    
    <ConnectionList />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onActivated } from 'vue'
import ConnectionList from '@/components/connection/ConnectionList.vue'

onMounted(() => {
  console.log('Connections 컴포넌트 마운트됨')
})

onActivated(() => {
  console.log('Connections 컴포넌트 활성화됨')
})
</script>

<style scoped>
.connections-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.page-header p {
  margin: 0;
  color: #606266;
}
</style>