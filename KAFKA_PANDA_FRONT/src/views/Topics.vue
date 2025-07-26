<template>
  <div class="topics-view">
    <div class="view-header">
      <h2>토픽 관리</h2>
      <p>Kafka 토픽을 관리합니다</p>
    </div>

    <div v-if="!currentConnection" class="no-connection">
      <el-empty description="연결된 클러스터가 없습니다">
        <el-button type="primary" @click="$router.push('/connections')">
          연결 관리로 이동
        </el-button>
      </el-empty>
    </div>

    <div v-else class="topics-content">
      <TopicList :connection-id="currentConnection.id" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import TopicList from '@/components/topic/TopicList.vue'

const connectionStore = useConnectionStore()

const currentConnection = computed(() => connectionStore.currentConnection)
</script>

<style scoped>
.topics-view {
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

.topics-content {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}
</style>