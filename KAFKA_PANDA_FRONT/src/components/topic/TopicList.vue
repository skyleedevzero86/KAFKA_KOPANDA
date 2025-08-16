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
          
          <el-collapse v-if="showDebugSection">
            <el-collapse-item title="고급 옵션 (개발용)" name="debug">
              <el-alert 
                title="주의" 
                type="warning" 
                :closable="false"
                description="내부 토픽은 일반적으로 Kafka에서 자동으로 관리됩니다. 수동 생성은 권장되지 않습니다."
                show-icon
              />
              <br>
              <el-button-group>
                <el-button size="small" @click="attemptCreateInternalTopic('__consumer_offsets')" :disabled="loading">
                  __consumer_offsets 생성 시도
                </el-button>
                <el-button size="small" @click="attemptCreateInternalTopic('__transaction_state')" :disabled="loading">
                  __transaction_state 생성 시도
                </el-button>
                <el-button size="small" @click="attemptCreateInternalTopic('__schema_registry')" :disabled="loading">
                  __schema_registry 생성 시도
                </el-button>
              </el-button-group>
            </el-collapse-item>
          </el-collapse>
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
import { ElMessage, ElMessageBox } from 'element-plus'
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
const showDebugSection = ref(false) 

const { topics, loading, error } = storeToRefs(topicStore)
const { connections, currentConnection } = storeToRefs(connectionStore)

const activeConnection = computed(() => {
  return currentConnection?.value || connections.value?.[0] || null
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
      
      if (status.missing.length > 0) {
        ElMessage.info(`누락된 내부 토픽: ${status.missing.join(', ')}`)
      }
    } else {
      ElMessage.info('내부 토픽이 발견되지 않았습니다. Kafka 클러스터가 아직 활동하지 않았을 수 있습니다.')
    }
  } catch (err: any) {
    console.error('내부 토픽 확인 실패:', err)
    
    if (err.message?.includes('DESCRIBE_TOPIC_PARTITIONS') || 
        err.message?.includes('UnsupportedVersionException')) {
      ElMessage.warning('Kafka 버전 호환성 문제로 일부 기능이 제한됩니다. Kafka 2.8.0 이상을 권장합니다.')
      internalTopicsStatus.value = []
    } else {
      ElMessage.error(`내부 토픽 확인 실패: ${err.message || 'Unknown error'}`)
    }
  }
}

const attemptCreateInternalTopic = async (topicName: string) => {
  const connection = activeConnection.value
  
  if (!connection) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  try {
    await ElMessageBox.confirm(
      `내부 토픽 '${topicName}'을 수동으로 생성하려고 합니다. 이는 권장되지 않으며 Kafka 클러스터에 문제를 일으킬 수 있습니다. 계속하시겠습니까?`,
      '경고',
      {
        confirmButtonText: '계속',
        cancelButtonText: '취소',
        type: 'warning',
      }
    )
  } catch {
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
    
    await topicStore.createTopic(connection.id, request)
    ElMessage.success(`내부 토픽 '${topicName}'이 생성되었습니다`)
    
    await checkForInternalTopics()
  } catch (err: any) {
    console.error('Internal topic creation failed:', err)
    
    if (err.message.includes('자동으로 관리됩니다')) {
      ElMessage.warning(err.message)
    } else {
      ElMessage.error(`내부 토픽 생성 실패: ${err.message}`)
    }
  }
}

const refreshTopics = async () => {
  const connection = activeConnection.value
  if (connection) {
    await topicStore.fetchTopics(connection.id, showInternalTopics.value)
    await checkForInternalTopics()
  }
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
    await topicStore.createTopic(connection.id, data)
    showCreateForm.value = false
    ElMessage.success('토픽이 생성되었습니다')
  } catch (err: any) {
    console.error('Topic creation failed:', err)
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
  } catch (err: any) {
    console.error('Topic deletion failed:', err)
  }
}

onMounted(async () => {
  await connectionStore.fetchConnections()
  const connection = activeConnection.value
  if (connection) {
    await topicStore.fetchTopics(connection.id, showInternalTopics.value)
    await checkForInternalTopics()
  }
})

watch(activeConnection, async (newConnection) => {
  if (newConnection) {
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

.internal-topics-content .el-button-group {
  display: flex;
  gap: 8px;
  margin-top: 12px;
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