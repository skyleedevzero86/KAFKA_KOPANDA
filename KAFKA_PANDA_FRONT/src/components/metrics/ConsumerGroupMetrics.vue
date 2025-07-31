<template>
  <div class="consumer-group-metrics">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>컨슈머 그룹</span>
          <el-button 
            size="small" 
            type="primary" 
            @click="createTestGroup"
            :loading="creating"
          >
            테스트 그룹 생성
          </el-button>
        </div>
      </template>

      <div v-if="consumerGroups.length === 0" class="empty-state">
        <el-empty description="컨슈머 그룹이 없습니다">
          <el-button type="primary" @click="createTestGroup">
            테스트 컨슈머 그룹 생성
          </el-button>
        </el-empty>
      </div>

      <el-table v-else :data="consumerGroups" style="width: 100%">
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
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ConsumerGroupDto } from '@/types/metrics'
import { formatNumber } from '@/utils/formatters'
import { useMetricsStore } from '@/stores/metrics'
import { useConnectionStore } from '@/stores/connection'

interface Props {
  consumerGroups: ConsumerGroupDto[]
}

defineProps<Props>()

const metricsStore = useMetricsStore()
const connectionStore = useConnectionStore()
const creating = ref(false)

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

const createTestGroup = async () => {
  if (!connectionStore.currentConnection) {
    ElMessage.warning('연결을 선택해주세요')
    return
  }

  try {
    creating.value = true
    await metricsStore.createTestConsumerGroup(
      connectionStore.currentConnection.id,
      'test-topic' 
    )
    ElMessage.success('테스트 컨슈머 그룹이 생성되었습니다')
  } catch (error) {
    ElMessage.error('테스트 컨슈머 그룹 생성에 실패했습니다')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.consumer-group-metrics {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.empty-state {
  padding: 20px;
  text-align: center;
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