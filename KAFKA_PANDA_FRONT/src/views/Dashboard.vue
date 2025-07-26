<template>
  <div class="dashboard">
    <div class="dashboard-header">
      <h2>대시보드</h2>
      <p>Kafka 클러스터 상태 및 개요</p>
    </div>

    <div v-if="!currentConnection" class="no-connection">
      <el-empty description="연결된 클러스터가 없습니다">
        <el-button type="primary" @click="$router.push('/connections')">
          연결 관리로 이동
        </el-button>
      </el-empty>
    </div>

    <div v-else class="dashboard-content">
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
.dashboard {
  padding: 20px;
}

.dashboard-header {
  margin-bottom: 30px;
}

.dashboard-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.dashboard-header p {
  margin: 0;
  color: #909399;
}

.no-connection {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.dashboard-content {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
</style>