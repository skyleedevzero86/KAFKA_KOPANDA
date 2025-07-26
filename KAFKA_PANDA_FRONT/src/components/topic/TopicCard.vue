<template>
  <el-card class="topic-card" :class="{ internal: topic.isInternal }" @click="handleClick">
    <template #header>
      <div class="card-header">
        <span class="topic-name">{{ topic.name }}</span>
        <div class="topic-badges">
          <el-tag v-if="topic.isInternal" type="info" size="small">내부</el-tag>
          <el-tag
            :type="topic.isHealthy ? 'success' : 'danger'"
            size="small"
          >
            {{ topic.isHealthy ? '정상' : '비정상' }}
          </el-tag>
        </div>
      </div>
    </template>

    <div class="card-content">
      <div class="topic-info">
        <div class="info-row">
          <span class="info-label">파티션:</span>
          <span class="info-value">{{ topic.partitionCount }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">복제 팩터:</span>
          <span class="info-value">{{ topic.replicationFactor }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">메시지 수:</span>
          <span class="info-value">{{ formatNumber(topic.messageCount) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">생성일:</span>
          <span class="info-value">{{ formatDate(topic.createdAt) }}</span>
        </div>
      </div>

      <div class="card-actions">
        <el-button size="small" @click.stop="handleDetail">
          <el-icon><View /></el-icon>
          상세보기
        </el-button>
        <el-button
          v-if="!topic.isInternal"
          size="small"
          type="danger"
          @click.stop="handleDelete"
        >
          <el-icon><Delete /></el-icon>
          삭제
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { View, Delete } from '@element-plus/icons-vue'
import { formatNumber, formatDate } from '@/utils/formatters'
import type { TopicDto } from '@/types/topic'

interface Props {
  topic: TopicDto
}

const props = defineProps<Props>()

const emit = defineEmits<{
  detail: [topic: TopicDto]
  delete: [topicName: string]
  select: [topic: TopicDto]
}>()

const handleClick = () => {
  emit('select', props.topic)
}

const handleDetail = () => {
  emit('detail', props.topic)
}

const handleDelete = () => {
  emit('delete', props.topic.name)
}
</script>

<style scoped>
.topic-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.topic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.topic-card.internal {
  opacity: 0.8;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic-name {
  font-weight: bold;
  color: #303133;
  font-family: 'Courier New', monospace;
}

.topic-badges {
  display: flex;
  gap: 8px;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  color: #909399;
  font-size: 14px;
}

.info-value {
  color: #303133;
  font-weight: 500;
}

.card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>