<template>
  <div class="consumer-group-metrics">
    <el-table :data="consumerGroups" style="width: 100%">
      <el-table-column prop="groupId" label="그룹 ID" />
      <el-table-column prop="state" label="상태" width="120">
        <template #default="{ row }">
          <el-tag :type="getStateType(row.state)">
            {{ row.state }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="memberCount" label="멤버 수" width="100" />
      <el-table-column prop="topicCount" label="토픽 수" width="100" />
      <el-table-column label="오프셋" width="200">
        <template #default="{ row }">
          <div v-if="Object.keys(row.offsets).length > 0">
            <div v-for="(offset, topic) in row.offsets" :key="topic" class="offset-item">
              <span class="topic-name">{{ topic }}:</span>
              <span class="offset-value">{{ formatNumber(offset) }}</span>
            </div>
          </div>
          <span v-else class="no-offsets">오프셋 정보 없음</span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import type { ConsumerGroupDto } from '@/types/metrics'
import { formatNumber } from '@/utils/formatters'

interface Props {
  consumerGroups: ConsumerGroupDto[]
}

defineProps<Props>()

const getStateType = (state: string) => {
  switch (state.toUpperCase()) {
    case 'STABLE':
      return 'success'
    case 'PREPARING_REBALANCE':
    case 'COMPLETING_REBALANCE':
      return 'warning'
    case 'DEAD':
    case 'EMPTY':
      return 'danger'
    default:
      return 'info'
  }
}
</script>

<style scoped>
.consumer-group-metrics {
  margin-bottom: 20px;
}

.offset-item {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
}

.topic-name {
  color: #606266;
}

.offset-value {
  font-weight: bold;
  color: #409EFF;
}

.no-offsets {
  color: #909399;
  font-style: italic;
}
</style>