<template>
  <div class="metrics-view">
    <div class="view-header">
      <h2>메트릭</h2>
      <p>Kafka 클러스터 메트릭을 확인합니다</p>
    </div>

    <div v-if="!currentConnection" class="no-connection">
      <el-empty description="연결된 클러스터가 없습니다">
        <el-button type="primary" @click="$router.push('/connections')">
          연결 관리로 이동
        </el-button>
      </el-empty>
    </div>

    <div v-else class="metrics-content">
      <MetricsDashboard :connection-id="currentConnection.id" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import MetricsDashboard from '@/components/metrics/MetricsDashboard.vue'

const connectionStore = useConnectionStore()

const currentConnection = computed(() => connectionStore.currentConnection)
</script>

<style scoped>
.metrics-view {
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

.no-connection {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.metrics-content {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
</style>