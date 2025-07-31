<template>
  <div class="topic-list">
    <div class="list-header">
      <h3>토픽 관리</h3>
      <div class="header-actions">
        <el-button @click="refreshTopics">
          <Refresh />
          새로고침
        </el-button>
        <el-button type="primary" @click="showCreateDialog = true">
          <Plus />
          새 토픽
        </el-button>
      </div>
    </div>

    <div v-if="currentConnection" class="current-connection">
      <el-tag type="info">
        현재 연결: {{ currentConnection.name }}
      </el-tag>
    </div>

    <div v-else class="no-connection">
      <el-empty description="연결을 선택해주세요">
        <el-button type="primary" @click="$router.push('/connections')">
          연결 관리로 이동
        </el-button>
      </el-empty>
    </div>

    <div v-if="currentConnection">

      <div v-if="loading" class="loading-container">
        <LoadingSpinner />
      </div>

      <div v-else-if="error" class="error-container">
        <ErrorMessage :message="error" @retry="refreshTopics" />
      </div>

      <div v-else-if="topics.length === 0" class="empty-container">
        <el-empty description="토픽이 없습니다">
          <el-button type="primary" @click="showCreateDialog = true">
            첫 번째 토픽 만들기
          </el-button>
        </el-empty>
      </div>

      <div v-else class="topics-grid">
        <TopicCard
          v-for="topic in topics"
          :key="topic.name"
          :topic="topic"
          @delete="handleDelete"
          @select="handleSelect"
        />
      </div>
    </div>

    <TopicForm
      v-model="showCreateDialog"
      :connection-id="currentConnection?.id"
      @created="handleCreated"
    />

    <ConfirmDialog
      v-model="showDeleteDialog"
      title="토픽 삭제"
      :message="`'${deletingTopicName}' 토픽을 삭제하시겠습니까?`"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useTopicStore } from '@/stores/topic'
import { useConnectionStore } from '@/stores/connection'
import type { TopicDto, CreateTopicRequest } from '@/types/topic'
import TopicCard from './TopicCard.vue'
import TopicForm from './TopicForm.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const topicStore = useTopicStore()
const connectionStore = useConnectionStore()

const showCreateDialog = ref(false)
const showDeleteDialog = ref(false)
const deletingTopicName = ref('')

const { topics, loading, error } = topicStore
const currentConnection = computed(() => connectionStore.currentConnection)

const refreshTopics = async () => {
  if (currentConnection.value) {
    await topicStore.fetchTopics(currentConnection.value.id)
  }
}

const handleDelete = (topicName: string) => {
  deletingTopicName.value = topicName
  showDeleteDialog.value = true
}

const handleSelect = (topic: TopicDto) => {
  const topicDetail = {
    name: topic.name,
    partitionCount: topic.partitionCount,
    replicationFactor: topic.replicationFactor,
    messageCount: topic.messageCount,
    isInternal: topic.isInternal,
    isHealthy: topic.isHealthy,
    config: {},
    partitions: [],
    createdAt: topic.createdAt,
    updatedAt: topic.updatedAt
  }
  topicStore.setCurrentTopic(topicDetail)
  ElMessage.success(`'${topic.name}' 토픽이 선택되었습니다.`)
}

const handleCreated = async (data: CreateTopicRequest) => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    await topicStore.createTopic(currentConnection.value.id, data)
    ElMessage.success('토픽이 생성되었습니다.')
  } catch (error) {
    console.error('토픽 생성 실패:', error)
  }
}

const confirmDelete = async () => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    await topicStore.deleteTopic(currentConnection.value.id, deletingTopicName.value)
    ElMessage.success('토픽이 삭제되었습니다.')
    showDeleteDialog.value = false
  } catch (error) {
    console.error('토픽 삭제 실패:', error)
  }
}

const clearError = () => {
  topicStore.clearError()
}

watch(() => currentConnection.value?.id, (newConnectionId) => {
  if (newConnectionId) {
    topicStore.fetchTopics(newConnectionId)
  } else {
    topicStore.topics.length = 0
  }
})

onMounted(() => {
  if (currentConnection.value) {
    topicStore.fetchTopics(currentConnection.value.id)
  }
})
</script>

<style scoped>
.topic-list {
  padding: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.list-header h3 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.current-connection {
  margin-bottom: 16px;
}

.no-connection,
.loading-container,
.error-container,
.empty-container {
  padding: 40px;
  text-align: center;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}
</style>