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
    <div v-if="activeConnection" class="current-connection">
      <el-alert
        :title="`현재 연결: ${activeConnection.name} (${activeConnection.host}:${activeConnection.port})`"
        type="info"
        :closable="false"
        show-icon
      />
    </div>
    <div v-if="!activeConnection" class="no-connection">
      <el-alert
        title="연결이 선택되지 않았습니다"
        description="토픽을 생성하려면 먼저 연결을 선택해주세요"
        type="warning"
        :closable="false"
        show-icon
      />
    </div>
    <div v-if="showInternalTopics" class="internal-topics-section">
      <el-card class="internal-topics-card">
        <template #header>
          <div class="card-header">
            <span>내부 토픽 관리</span>
            <el-button size="small" @click="checkForInternalTopics" :loading="loading">
              내부 토픽 확인
            </el-button>
          </div>
        </template>
        <div class="internal-topics-content">
          <p>Kafka 클러스터의 내부 토픽들은 자동으로 생성됩니다. 새로고침하여 확인해보세요.</p>
          <div v-if="internalTopicsStatus.length > 0" class="internal-topics-status">
            <h4>발견된 내부 토픽:</h4>
            <ul>
              <li v-for="topic in internalTopicsStatus" :key="topic" class="internal-topic-item">
                <el-tag type="info" size="small">{{ topic }}</el-tag>
              </li>
            </ul>
          </div>
          <div v-else class="no-internal-topics">
            <p>내부 토픽이 발견되지 않았습니다. Kafka 클러스터가 아직 활동하지 않았을 수 있습니다.</p>
          </div>
        </div>
      </el-card>
    </div>
    <div v-if="loading" class="loading-container">
      <LoadingSpinner />
    </div>
    <div v-else-if="error" class="error-container">
      <ErrorMessage :message="error" @retry="refreshTopics" />
    </div>
    <div v-else-if="filteredTopics.length === 0" class="empty-state">
      <el-empty description="토픽이 없습니다">
        <el-button type="primary" @click="showCreateForm = true" :disabled="!activeConnection">
          첫 번째 토픽 만들기
        </el-button>
      </el-empty>
    </div>
    <div v-else class="topics-grid">
      <TopicCard
        v-for="topic in filteredTopics"
        :key="topic.name"
        :topic="topic"
        @delete="handleDeleteTopic"
        @select="handleTopicSelect"
      />
    </div>
    <TopicForm
      v-model="showCreateForm"
      :connection-id="activeConnection?.id"
      @submit="handleTopicCreated"
    />
    <ConfirmDialog
      v-model="showDeleteDialog"
      title="토픽 삭제"
      :message="`'${deletingTopicName}' 토픽을 삭제하시겠습니까?`"
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
const internalTopicsStatus = ref<string[]>([])

const { topics, loading, error } = storeToRefs(topicStore)
const { connections, currentConnection } = storeToRefs(connectionStore)

const activeConnection = computed(() => {
  const connection = currentConnection?.value || connections.value?.[0] || null
  console.log('Active connection:', connection)
  return connection
})

const filteredTopics = computed(() => {
  if (showInternalTopics.value) {
    return topics.value
  }
  return topics.value.filter(topic => !topic.isInternal)
})

const handleInternalTopicsToggle = async (value: boolean) => {
  showInternalTopics.value = value
  ElMessage.success(value ? '내부 토픽이 표시됩니다' : '내부 토픽이 숨겨집니다')
  await refreshTopics()
}

const checkForInternalTopics = async () => {
  const connection = activeConnection.value
  
  if (!connection) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    const status = await topicStore.getInternalTopicsStatus(connection.id)
    internalTopicsStatus.value = status.found
    
    if (status.found.length > 0) {
      ElMessage.success(`${status.found.length}개의 내부 토픽을 발견했습니다`)
    } else {
      ElMessage.info('내부 토픽이 발견되지 않았습니다.')
    }
  } catch (err: any) {
    console.error('내부 토픽 확인 실패:', err)
    ElMessage.error(`내부 토픽 확인 실패: ${err.message || 'Unknown error'}`)
  }
}

const refreshTopics = async () => {
  const connection = activeConnection.value
  if (connection) {
    try {
      await topicStore.fetchTopics(connection.id, showInternalTopics.value)
      await checkForInternalTopics()
      ElMessage.success('토픽 목록이 새로고침되었습니다')
    } catch (error) {
      console.error('토픽 새로고침 실패:', error)
      ElMessage.error('토픽 목록 새로고침에 실패했습니다')
    }
  }
}

const handleTopicSelect = (topic: TopicDto) => {
  console.log('토픽 선택됨:', topic)
}

const handleDeleteTopic = (topicName: string) => {
  deletingTopicName.value = topicName
  showDeleteDialog.value = true
}

const handleTopicCreated = async (data: CreateTopicRequest) => {
  const connection = activeConnection.value
  
  if (!connection) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    console.log('토픽 생성 시작:', { connectionId: connection.id, topicData: data })
    
    await topicStore.createTopic(connection.id, data)
    
    ElMessage.success('토픽이 성공적으로 생성되었습니다')
    showCreateForm.value = false
    
    await refreshTopics()
    
  } catch (err: any) {
    console.error('토픽 생성 실패:', err)
    ElMessage.error(`토픽 생성에 실패했습니다: ${err.message || 'Unknown error'}`)
  }
}

const confirmDeleteTopic = async () => {
  const connection = activeConnection.value
  
  if (!connection || !deletingTopicName.value) {
    ElMessage.error('삭제할 토픽을 찾을 수 없습니다')
    return
  }

  try {
    await topicStore.deleteTopic(connection.id, deletingTopicName.value)
    showDeleteDialog.value = false
    deletingTopicName.value = ''
    ElMessage.success('토픽이 삭제되었습니다')
    
    await refreshTopics()
    
  } catch (err: any) {
    console.error('토픽 삭제 실패:', err)
    ElMessage.error(`토픽 삭제에 실패했습니다: ${err.message || 'Unknown error'}`)
  }
}

onMounted(async () => {
  console.log('TopicList 컴포넌트 마운트됨')
  await connectionStore.fetchConnections()
  const connection = activeConnection.value
  if (connection) {
    await topicStore.fetchTopics(connection.id, showInternalTopics.value)
    await checkForInternalTopics()
  }
})

watch(activeConnection, async (newConnection) => {
  if (newConnection) {
    console.log('연결 변경됨:', newConnection.name)
    await topicStore.fetchTopics(newConnection.id, showInternalTopics.value)
    await checkForInternalTopics()
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

.internal-topics-status {
  margin: 16px 0;
}

.internal-topics-status h4 {
  margin: 0 0 8px 0;
  color: #303133;
  font-size: 14px;
}

.internal-topics-status ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.internal-topic-item {
  display: inline-block;
  margin: 4px 8px 4px 0;
}

.no-internal-topics {
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.no-internal-topics p {
  margin: 0;
  color: #6c757d;
  font-style: italic;
}

.topics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}
</style>