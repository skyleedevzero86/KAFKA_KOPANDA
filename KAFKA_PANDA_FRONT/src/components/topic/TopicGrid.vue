<template>
  <div class="topic-grid">
    <el-row :gutter="16">
      <el-col
        v-for="topic in topics"
        :key="topic.name"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <TopicCard
          :topic="topic"
          @select="$emit('select', topic)"
          @delete="$emit('delete', topic.name)"
        />
      </el-col>
    </el-row>

    <div v-if="topics.length === 0" class="empty-state">
      <el-empty description="토픽이 없습니다" />
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TopicDto } from '@/types/topic'
import TopicCard from './TopicCard.vue'

interface Props {
  topics: TopicDto[]
}

defineProps<Props>()

defineEmits<{
  select: [topic: TopicDto]
  delete: [topicName: string]
}>()
</script>

<style scoped>
.topic-grid {
  margin-top: 16px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}
</style>