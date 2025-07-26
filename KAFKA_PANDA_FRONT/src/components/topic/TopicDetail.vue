<template>
  <el-dialog
    v-model="visible"
    title="토픽 상세 정보"
    width="800px"
    :before-close="handleClose"
  >
    <div v-if="topic" class="topic-detail">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="토픽 이름">
          {{ topic.name }}
        </el-descriptions-item>
        <el-descriptions-item label="파티션 수">
          {{ topic.partitionCount }}
        </el-descriptions-item>
        <el-descriptions-item label="복제 팩터">
          {{ topic.replicationFactor }}
        </el-descriptions-item>
        <el-descriptions-item label="메시지 수">
          {{ formatNumber(topic.messageCount) }}
        </el-descriptions-item>
        <el-descriptions-item label="내부 토픽">
          {{ topic.isInternal ? '예' : '아니오' }}
        </el-descriptions-item>
        <el-descriptions-item label="상태">
          <el-tag :type="topic.isHealthy ? 'success' : 'danger'">
            {{ topic.isHealthy ? '정상' : '비정상' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="생성일">
          {{ formatDate(topic.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="수정일">
          {{ formatDate(topic.updatedAt) }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="config-section">
        <h4>설정</h4>
        <el-table :data="configData" border>
          <el-table-column prop="key" label="키" />
          <el-table-column prop="value" label="값" />
        </el-table>
      </div>

      <div class="partitions-section">
        <h4>파티션 정보</h4>
        <el-table :data="topic.partitions" border>
          <el-table-column prop="partitionNumber" label="파티션" width="100" />
          <el-table-column prop="leader" label="리더" width="80" />
          <el-table-column prop="earliestOffset" label="최초 오프셋" width="120" />
          <el-table-column prop="latestOffset" label="최신 오프셋" width="120" />
          <el-table-column prop="messageCount" label="메시지 수" width="100" />
          <el-table-column label="상태" width="100">
            <template #default="{ row }">
              <el-tag
                :type="row.isHealthy ? 'success' : 'danger'"
                size="small"
              >
                {{ row.isHealthy ? '정상' : '비정상' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { formatNumber, formatDate } from '@/utils/formatters'
import type { TopicDetailDto } from '@/types/topic'

interface Props {
  modelValue: boolean
  topic: TopicDetailDto | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const visible = ref(props.modelValue)

watch(() => props.modelValue, (newValue) => {
  visible.value = newValue
})

watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

const topic = computed(() => props.topic)

const configData = computed(() => {
  if (!topic.value) return []
  return Object.entries(topic.value.config).map(([key, value]) => ({
    key,
    value
  }))
})

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

.config-section,
.partitions-section {
  margin-top: 20px;
}

.config-section h4,
.partitions-section h4 {
  margin: 0 0 12px 0;
  color: #303133;
}
</style>