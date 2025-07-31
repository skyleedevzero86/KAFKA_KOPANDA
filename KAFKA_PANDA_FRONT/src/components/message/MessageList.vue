<template>
  <div class="message-list">
    <div class="message-header">
      <h3>메시지 관리</h3>
      <div class="header-actions">
        <el-button @click="showSearchDialog = true">
          <Search />
          검색
        </el-button>
        <el-button type="primary" @click="showSendDialog = true">
          <Plus />
          메시지 전송
        </el-button>
      </div>
    </div>
    
    <div class="message-filters">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="연결">
            <el-select 
              v-model="selectedConnectionId" 
              placeholder="연결을 선택하세요"
              @change="handleConnectionChange"
              style="width: 100%"
            >
              <el-option
                v-for="connection in connections"
                :key="connection.id"
                :label="connection.name"
                :value="connection.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="토픽">
            <el-select 
              v-model="selectedTopic" 
              placeholder="토픽을 선택하세요"
              @change="handleTopicChange"
              style="width: 100%"
            >
              <el-option
                v-for="topic in topics"
                :key="topic.name"
                :label="topic.name"
                :value="topic.name"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="파티션">
            <el-select 
              v-model="selectedPartition" 
              placeholder="파티션을 선택하세요"
              @change="loadMessages"
              style="width: 100%"
            >
              <el-option label="자동 선택" :value="null" />
              <el-option
                v-for="partition in availablePartitions"
                :key="partition"
                :label="`파티션 ${partition}`"
                :value="partition"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      
      <el-row :gutter="20" style="margin-top: 12px;">
        <el-col :span="8">
          <el-form-item label="조회 방식">
            <el-select 
              v-model="selectedOffsetType" 
              @change="loadMessages"
              style="width: 100%"
            >
              <el-option label="최신 메시지" value="LATEST" />
              <el-option label="모든 메시지" value="EARLIEST" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="조회 개수">
            <el-input-number 
              v-model="messageLimit" 
              :min="1" 
              :max="100"
              @change="loadMessages"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-button @click="loadMessages" type="primary" style="margin-top: 32px;">
            <Refresh />
            새로고침
          </el-button>
        </el-col>
      </el-row>
    </div>

    <div class="message-content">
      <LoadingSpinner v-if="messageStore.loading" />
      <ErrorMessage v-else-if="messageStore.error" :message="messageStore.error" />
      <div v-else-if="messageStore.messages.length === 0" class="empty-state">
        <el-empty description="메시지가 없습니다." />
      </div>
      <div v-else class="message-items">
        <MessageCard
          v-for="message in messageStore.messages"
          :key="`${message.offset}-${message.partition}`"
          :message="message"
        />
      </div>
    </div>

    <MessageForm
      v-model="showSendDialog"
      :connection-id="selectedConnectionId"
      :topic-name="selectedTopic"
      @sent="handleMessageSent"
    />

    <MessageSearch
      v-model="showSearchDialog"
      :connection-id="selectedConnectionId"
      @search="handleSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useConnectionStore } from '@/stores/connection'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import type { MessageDto } from '@/types/message'
import { OffsetType } from '@/types/message'
import MessageCard from './MessageCard.vue'
import MessageForm from './MessageForm.vue'
import MessageSearch from './MessageSearch.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

const messageStore = useMessageStore()
const connectionStore = useConnectionStore()
const topicStore = useTopicStore()


const showSendDialog = ref(false)
const showSearchDialog = ref(false)
const selectedConnectionId = ref('')
const selectedTopic = ref('')
const selectedPartition = ref<number | null>(null)
const selectedOffsetType = ref<OffsetType>(OffsetType.EARLIEST)
const messageLimit = ref(20)


const connections = computed(() => connectionStore.connections)
const topics = computed(() => topicStore.topics)
const currentTopicDetails = computed(() => topicStore.currentTopic)

const availablePartitions = computed(() => {
  if (!currentTopicDetails.value) return []
  return Array.from({ length: currentTopicDetails.value.partitionCount }, (_, i) => i)
})

const loadMessages = async () => {
  if (selectedConnectionId.value && selectedTopic.value) {
    await messageStore.getMessages(
      selectedConnectionId.value,
      selectedTopic.value,
      selectedPartition.value || 0,
      null,
      selectedOffsetType.value,
      messageLimit.value
    )
  }
}

const handleConnectionChange = async () => {
  selectedTopic.value = ''
  selectedPartition.value = null
  if (selectedConnectionId.value) {
    await topicStore.fetchTopics(selectedConnectionId.value)
  }
}

const handleTopicChange = async () => {
  if (selectedConnectionId.value && selectedTopic.value) {
    await topicStore.getTopicDetails(selectedConnectionId.value, selectedTopic.value)
    selectedPartition.value = null
    await loadMessages()
  }
}

onMounted(async () => {
  await connectionStore.fetchConnections()
  if (connections.value.length > 0) {
    selectedConnectionId.value = connections.value[0].id
  }
})

const handleMessageSent = () => {
  ElMessage.success('메시지가 성공적으로 전송되었습니다.')
  showSendDialog.value = false
 
  setTimeout(() => {
    loadMessages()
  }, 1000)
}

const handleSearch = (searchResults: MessageDto[]) => {
  ElMessage.success(`${searchResults.length}개의 메시지를 찾았습니다.`)
  showSearchDialog.value = false
}
</script>

<style scoped>
.message-list {
  padding: 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.message-header h3 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.message-filters {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.message-content {
  min-height: 400px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 300px;
}

.message-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
</style>