<template>
  <el-card class="topic-card" @click="handleClick">
    <template #header>
      <div class="card-header">
        <span class="topic-name">{{ topic.name }}</span>
        <div class="topic-badges">
          <el-tag v-if="topic.isInternal" size="small" type="warning">시스템</el-tag>
          <el-tag v-if="topic.isHealthy" size="small" type="success">정상</el-tag>
          <el-tag v-else size="small" type="danger">오류</el-tag>
        </div>
      </div>
    </template>

    <div class="card-content">
      <div class="topic-info">
        <p><strong>파티션:</strong> {{ topic.partitionCount }}</p>
        <p><strong>복제 팩터:</strong> {{ topic.replicationFactor }}</p>
        <p><strong>메시지 수:</strong> {{ formatNumber(topic.messageCount) }}</p>
        <p><strong>생성일:</strong> {{ formatDate(topic.createdAt) }}</p>
      </div>

      <div class="card-actions">
        <el-button size="small" @click.stop="handleDelete">
          <el-icon><Delete /></el-icon>
          삭제
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { Delete } from '@element-plus/icons-vue'
import type { TopicDto } from '@/types/topic'
import { formatDate, formatNumber } from '@/utils/formatters'

interface Props {
  topic: TopicDto
}

const props = defineProps<Props>()

const emit = defineEmits<{
  select: [topic: TopicDto]
  delete: [topicName: string]
}>()

const handleClick = () => {
  emit('select', props.topic)
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.topic-name {
  font-weight: bold;
  color: #303133;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-badges {
  display: flex;
  gap: 4px;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.topic-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #606266;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
}
</style>