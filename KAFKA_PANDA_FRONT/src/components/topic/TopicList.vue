<template>
  <div class="topic-list">
    <div class="header">
      <h2>토픽 목록</h2>
      <div class="header-actions">
        
        <div class="topic-filters">
          <el-switch
            v-model="showInternalTopics"
            active-text="내부 토픽 표시"
            inactive-text="내부 토픽 숨김"
            @change="handleInternalTopicsToggle"
          />
        </div>
        <el-button @click="refreshTopics">
          <Refresh />
          새로고침
        </el-button>
        <el-button type="primary" @click="showCreateForm = true">
          <Plus />
          토픽 생성
        </el-button>
      </div>
    </div>

   
    <div v-if="showInternalTopics" class="internal-topics-section">
      <el-card class="internal-topics-card">
        <template #header>
          <div class="card-header">
            <span>내부 토픽 관리</span>
            <el-button size="small" @click="addCommonInternalTopics">
              일반 내부 토픽 추가
            </el-button>
          </div>
        </template>
        <div class="internal-topics-content">
          <p>Kafka 클러스터의 내부 토픽들을 수동으로 추가할 수 있습니다.</p>
          <el-button-group>
            <el-button size="small" @click="addInternalTopic('__consumer_offsets')">
              __consumer_offsets
            </el-button>
            <el-button size="small" @click="addInternalTopic('__transaction_state')">
              __transaction_state
            </el-button>
            <el-button size="small" @click="addInternalTopic('__schema_registry')">
              __schema_registry
            </el-button>
          </el-button-group>
        </div>
      </el-card>
    </div>

    <div v-if="loading" class="loading-container">
      <LoadingSpinner />
    </div>
    
    <div v-else-if="error" class="error-container">
      <ErrorMessage :message="error" />
    </div>
    
    <div v-else-if="filteredTopics.length === 0" class="empty-state">
      <p>토픽이 없습니다.</p>
    </div>
    
    <div v-else class="topics-grid">
      <TopicCard
        v-for="topic in filteredTopics"
        :key="topic.name"
        :topic="topic"
        @delete="handleDeleteTopic"
      />
    </div>
    <TopicForm
      v-model="showCreateForm"
      @created="handleTopicCreated"
    />
    
    <ConfirmDialog
      v-model="showDeleteDialog"
      title="토픽 삭제"
      message="정말로 이 토픽을 삭제하시겠습니까?"
      @confirm="confirmDeleteTopic"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
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
import { storeToRefs } from 'pinia'

const topicStore = useTopicStore()
const connectionStore = useConnectionStore()

const showCreateForm = ref(false)
const showDeleteDialog = ref(false)
const deletingTopicName = ref('')
const showInternalTopics = ref(true) 

const { topics, loading, error, currentConnection } = storeToRefs(topicStore)
const { connections } = storeToRefs(connectionStore)


const filteredTopics = computed(() => {
  if (showInternalTopics.value) {
    return topics.value
  }
  return topics.value.filter(topic => !topic.isInternal)
})

const handleInternalTopicsToggle = (value: boolean) => {
  showInternalTopics.value = value
  ElMessage.success(value ? '내부 토픽이 표시됩니다' : '내부 토픽이 숨겨집니다')
}


const addInternalTopic = async (topicName: string) => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    const request: CreateTopicRequest = {
      name: topicName,
      partitions: 1,
      replicationFactor: 1,
      config: {
        'cleanup.policy': 'delete',
        'retention.ms': '604800000'
      }
    }
    
    await topicStore.createTopic(currentConnection.value.id, request)
    ElMessage.success(`내부 토픽 '${topicName}'이 추가되었습니다`)
  } catch (err) {
    ElMessage.error(`내부 토픽 추가 실패: ${err instanceof Error ? err.message : 'Unknown error'}`)
  }
}

const addCommonInternalTopics = async () => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  const commonInternalTopics = [
    '__consumer_offsets',
    '__transaction_state',
    '__schema_registry'
  ]

  try {
    for (const topicName of commonInternalTopics) {
      const request: CreateTopicRequest = {
        name: topicName,
        partitions: 1,
        replicationFactor: 1,
        config: {
          'cleanup.policy': 'delete',
          'retention.ms': '604800000'
        }
      }
      
      await topicStore.createTopic(currentConnection.value.id, request)
    }
    
    ElMessage.success('일반 내부 토픽들이 추가되었습니다')
  } catch (err) {
    ElMessage.error(`내부 토픽 일괄 추가 실패: ${err instanceof Error ? err.message : 'Unknown error'}`)
  }
}


const refreshTopics = async () => {
  if (currentConnection.value) {
    await topicStore.fetchTopics(currentConnection.value.id, showInternalTopics.value)
  }
}

const handleDeleteTopic = (topicName: string) => {
  deletingTopicName.value = topicName
  showDeleteDialog.value = true
}

const handleTopicCreated = async (data: CreateTopicRequest) => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    await topicStore.createTopic(currentConnection.value.id, data)
    showCreateForm.value = false
    ElMessage.success('토픽이 생성되었습니다')
  } catch (err) {
    ElMessage.error(`토픽 생성 실패: ${err instanceof Error ? err.message : 'Unknown error'}`)
  }
}

const confirmDeleteTopic = async () => {
  if (!currentConnection.value || !deletingTopicName.value) {
    ElMessage.error('삭제할 토픽을 찾을 수 없습니다')
    return
  }

  try {
    await topicStore.deleteTopic(currentConnection.value.id, deletingTopicName.value)
    showDeleteDialog.value = false
    deletingTopicName.value = ''
    ElMessage.success('토픽이 삭제되었습니다')
  } catch (err) {
    ElMessage.error(`토픽 삭제 실패: ${err instanceof Error ? err.message : 'Unknown error'}`)
  }
}

onMounted(async () => {
  await connectionStore.fetchConnections()
  if (connections.value.length > 0) {
    await topicStore.fetchTopics(connections.value[0].id, showInternalTopics.value)
  }
})

watch(currentConnection, async (newConnection) => {
  if (newConnection) {
    await topicStore.fetchTopics(newConnection.id, showInternalTopics.value)
  }
})
</script>

<style scoped>
.topic-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header h2 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.topic-filters {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.internal-topics-section {
  margin-bottom: 24px;
}

.internal-topics-card {
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header span {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.internal-topics-content {
  padding: 15px 20px;
}

.internal-topics-content p {
  margin-bottom: 10px;
  color: #606266;
}

.internal-topics-content .el-button-group {
  display: flex;
  gap: 8px;
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