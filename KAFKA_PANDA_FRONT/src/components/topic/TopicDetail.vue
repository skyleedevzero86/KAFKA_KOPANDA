<template>
  <el-dialog
    v-model="visible"
    :title="`토픽 상세 정보: ${topic?.name}`"
    width="900px"
    :before-close="handleClose"
  >
    <div v-if="topic" class="topic-detail">
      <!-- 토픽 기본 정보 -->
      <el-card class="info-card">
        <template #header>
          <span>토픽 상세 정보</span>
        </template>
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="info-item">
              <strong>토픽명:</strong> {{ topic.name }}
            </div>
            <div class="info-item">
              <strong>파티션 수:</strong> {{ topic.partitionCount }}
            </div>
            <div class="info-item">
              <strong>복제 팩터:</strong> {{ topic.replicationFactor }}
            </div>
            <div class="info-item">
              <strong>메시지 수:</strong> {{ formatNumber(topic.messageCount) }}
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <strong>내부 토픽:</strong> {{ topic.isInternal ? '예' : '아니오' }}
            </div>
            <div class="info-item">
              <strong>상태:</strong> 
              <el-tag :type="topic.isHealthy ? 'success' : 'danger'">
                {{ topic.isHealthy ? '정상' : '오류' }}
              </el-tag>
            </div>
            <div class="info-item">
              <strong>생성일:</strong> {{ formatDate(topic.createdAt) }}
            </div>
            <div class="info-item">
              <strong>수정일:</strong> {{ formatDate(topic.updatedAt) }}
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 파티션 상세 정보 -->
      <el-card class="partition-card">
        <template #header>
          <span>파티션 상세 정보</span>
        </template>
        <div v-if="topic.partitions && topic.partitions.length > 0">
          <el-table :data="topic.partitions" style="width: 100%">
            <el-table-column prop="partitionNumber" label="파티션 번호" width="120" />
            <el-table-column prop="leader" label="리더" width="80" />
            <el-table-column label="복제본" width="120">
              <template #default="{ row }">
                {{ row.replicas.join(', ') }}
              </template>
            </el-table-column>
            <el-table-column label="동기화된 복제본" width="150">
              <template #default="{ row }">
                {{ row.inSyncReplicas.join(', ') }}
              </template>
            </el-table-column>
            <el-table-column prop="earliestOffset" label="최초 오프셋" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.earliestOffset) }}
              </template>
            </el-table-column>
            <el-table-column prop="latestOffset" label="최신 오프셋" width="120">
              <template #default="{ row }">
                {{ formatNumber(row.latestOffset) }}
              </template>
            </el-table-column>
            <el-table-column prop="messageCount" label="메시지 수" width="100">
              <template #default="{ row }">
                {{ formatNumber(row.messageCount) }}
              </template>
            </el-table-column>
            <el-table-column label="상태" width="100">
              <template #default="{ row }">
                <el-tag :type="row.isHealthy ? 'success' : 'danger'">
                  {{ row.isHealthy ? '정상' : '오류' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="복제 상태" width="120">
              <template #default="{ row }">
                <el-tag :type="row.isUnderReplicated ? 'warning' : 'success'">
                  {{ row.isUnderReplicated ? '복제 부족' : '정상' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="no-data">
          <el-empty description="파티션 정보가 없습니다" />
        </div>
      </el-card>

      <!-- 토픽 설정 -->
      <el-card class="config-card">
        <template #header>
          <span>토픽 설정</span>
        </template>
        <div v-if="Object.keys(topic.config).length > 0">
          <el-table :data="configTableData" style="width: 100%">
            <el-table-column prop="key" label="설정 키" width="200" />
            <el-table-column prop="value" label="설정 값" width="200" />
            <el-table-column prop="description" label="설명" />
          </el-table>
        </div>
        <div v-else class="no-data">
          <el-empty description="설정 정보가 없습니다" />
        </div>
      </el-card>
    </div>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">닫기</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TopicDetailDto } from '@/types/topic'
import { formatNumber, formatDate } from '@/utils/formatters'

interface Props {
  modelValue: boolean
  topic: TopicDetailDto | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const configTableData = computed(() => {
  if (!props.topic?.config) return []
  
  return Object.entries(props.topic.config).map(([key, value]) => ({
    key,
    value,
    description: getConfigDescription(key)
  }))
})

const getConfigDescription = (key: string): string => {
  const descriptions: Record<string, string> = {
    'retention.ms': '메시지 보관 기간 (밀리초)',
    'cleanup.policy': '정리 정책',
    'max.message.bytes': '최대 메시지 크기 (바이트)',
    'compression.type': '압축 타입',
    'min.insync.replicas': '최소 동기화 복제본 수',
    'segment.bytes': '세그먼트 크기 (바이트)',
    'segment.ms': '세그먼트 시간 (밀리초)',
    'delete.retention.ms': '삭제 보관 기간 (밀리초)',
    'flush.messages': '플러시 메시지 수',
    'flush.ms': '플러시 시간 (밀리초)'
  }
  
  return descriptions[key] || '설정 설명 없음'
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.topic-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-card,
.partition-card,
.config-card {
  margin-bottom: 0;
}

.info-item {
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-item strong {
  color: #606266;
  min-width: 100px;
}

.no-data {
  padding: 40px;
  text-align: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  font-size: 12px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: 600;
}
</style>