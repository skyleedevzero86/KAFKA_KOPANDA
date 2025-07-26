<template>
  <div class="connections-view">
    <div class="view-header">
      <h2>연결 관리</h2>
      <p>Kafka 클러스터 연결을 관리합니다</p>
    </div>

    <div v-if="loading" class="loading-container">
      <LoadingSpinner message="연결 목록을 불러오는 중..." />
    </div>

    <div v-else-if="error" class="error-container">
      <ErrorMessage :error="error" @close="connectionStore.clearError()" />
    </div>

    <div v-else class="connections-content">
      <ConnectionList />
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import ConnectionList from '@/components/connection/ConnectionList.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const connectionStore = useConnectionStore()

const { loading, error } = connectionStore

onMounted(() => {
  connectionStore.fetchConnections()
})
</script>

<style scoped>
.connections-view {
  padding: 20px;
}

.view-header {
  margin-bottom: 30px;
}

.view-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.view-header p {
  margin: 0;
  color: #909399;
}

.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.connections-content {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
</style>
