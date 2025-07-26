<template>
  <el-card class="consumer-group-metrics">
    <template #header>
      <div class="card-header">
        <span>컨슈머 그룹</span>
      </div>
    </template>
    
    <div v-if="!consumerGroups || consumerGroups.length === 0" class="no-data">
      <el-empty description="컨슈머 그룹이 없습니다" />
    </div>
    
    <div v-else class="consumer-groups-list">
      <div
        v-for="group in consumerGroups"
        :key="group.groupId"
        class="consumer-group-item"
      >
        <div class="group-header">
          <span class="group-id">{{ group.groupId }}</span>
          <el-tag :type="getStateType(group.state)" size="small">
            {{ group.state }}
          </el-tag>
        </div>
        <div class="group-info">
          <span>멤버: {{ group.memberCount }}</span>
          <span>토픽: {{ group.topicCount }}</span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { ConsumerGroupDto } from '@/types/metrics'

interface Props {
  consumerGroups: ConsumerGroupDto[] | null
}

defineProps<Props>()

const getStateType = (state: string) => {
  switch (state.toLowerCase()) {
    case 'stable':
      return 'success'
    case 'preparing_rebalance':
    case 'completing_rebalance':
      return 'warning'
    case 'dead':
      return 'danger'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.consumer-group-metrics {
  height: 100%;
}

.card-header {
  font-weight: bold;
  color: #303133;
}

.no-data {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 200px;
}

.consumer-groups-list {
  max-height: 400px;
  overflow-y: auto;
}

.consumer-group-item {
  padding: 12px;
  border-bottom: 1px solid #e6e6e6;
}

.consumer-group-item:last-child {
  border-bottom: none;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.group-id {
  font-weight: bold;
  color: #303133;
  font-family: 'Courier New', monospace;
}

.group-info {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #909399;
}
</style>