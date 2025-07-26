<template>
  <div class="topic-list">
    <div class="topic-header">
      <h3>토픽 목록</h3>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        새 토픽
      </el-button>
    </div>

    <div v-if="loading" class="loading-container">
      <LoadingSpinner message="토픽 목록을 불러오는 중..." />
    </div>

    <div v-else-if="error" class="error-container">
      <ErrorMessage :error="error" @close="topicStore.clearError()" />
    </div>

    <div v-else-if="topics.length === 0" class="empty-state">
      <el-empty description="토픽이 없습니다" />
    </div>

    <div v-else class="topics-grid">
      <TopicCard
        v-for="topic in topics"
        :key="topic.name"
        :topic="topic"
        @detail="handleDetail"
        @delete="handleDelete"
        @select="handleSelect"
      />
    </div>

    <TopicForm
      v-model="showCreateDialog"
      :connection-id="connectionId"
      @created="handleCreated"
    />

    <ConfirmDialog
      v-model="showDeleteDialog"
      title="토픽 삭제"
      message="정말로 이 토픽을 삭제하시겠습니까?"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { TopicDto } from '@/types/topic'
import TopicCard from './TopicCard.vue'
import TopicForm from './TopicForm.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

interface Props {
  connectionId: string
}

const props = defineProps<Props>()

const topicStore = useTopicStore()

const showCreateDialog = ref(false)
const showDeleteDialog = ref(false)
const deletingTopicName = ref<string | null>(null)

const { topics, loading, error } = topicStore

onMounted(() => {
  topicStore.fetchTopics(props.connectionId)
})

const handleDetail = (topic: TopicDto) => {
  // 토픽 상세보기 로직
  console.log('토픽 상세보기:', topic)
}

const handleDelete = (topicName: string) => {
  deletingTopicName.value = topicName
  showDeleteDialog.value = true
}

const handleSelect = (topic: TopicDto) => {
  topicStore.setCurrentTopic(topic)
}

const handleCreated = (topic: TopicDto) => {
  ElMessage.success('토픽이 성공적으로 생성되었습니다.')
  showCreateDialog.value = false
}

const confirmDelete = async () => {
  if (!deletingTopicName.value) return

  try {
    await topicStore.deleteTopic(props.connectionId, deletingTopicName.value)
    ElMessage.success('토픽이 성공적으로 삭제되었습니다.')
    showDeleteDialog.value = false
    deletingTopicName.value = null
  } catch (error) {
    // 에러는 store에서 처리됨
  }
}
</script>

<style scoped>
.topic-list {
  padding: 20px;
}

.topic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.topic-header h3 {
  margin: 0;
  color: #303133;
}

.loading-container,
.error-container,
.empty-state {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}
</style>