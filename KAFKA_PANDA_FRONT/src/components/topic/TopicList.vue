<template>
  <div class="topic-list">
    <div class="topic-header">
      <h3>토픽 목록</h3>
      <div class="header-actions">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          새로고침
        </el-button>
        <el-button type="primary" @click="showCreateDialog = true" :disabled="!currentConnection">
          <el-icon><Plus /></el-icon>
          새 토픽
        </el-button>
      </div>
    </div>

    <div v-if="!currentConnection" class="no-connection">
      <el-empty description="토픽을 보려면 먼저 연결을 선택하세요" />
    </div>

    <div v-else>
      <el-tabs v-model="activeTab" @tab-click="handleTabClick">
        <el-tab-pane label="모든 토픽" name="all">
          <TopicGrid :topics="topics" @select="handleSelect" @delete="handleDelete" />
        </el-tab-pane>
        <el-tab-pane label="사용자 토픽" name="user">
          <TopicGrid :topics="userTopics" @select="handleSelect" @delete="handleDelete" />
        </el-tab-pane>
        <el-tab-pane label="시스템 토픽" name="system">
          <TopicGrid :topics="internalTopics" @select="handleSelect" @delete="handleDelete" />
        </el-tab-pane>
      </el-tabs>
    </div>

    <TopicForm
      v-if="currentConnection"
      v-model="showCreateDialog"
      :connection-id="currentConnection.id"
      @created="handleCreated"
    />

    <ConfirmDialog
      v-model="showDeleteDialog"
      title="토픽 삭제"
      message="정말로 이 토픽을 삭제하시겠습니까? 이 작업은 되돌릴 수 없습니다."
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useTopicStore } from '@/stores/topic'
import { useConnectionStore } from '@/stores/connection'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import type { TopicDto } from '@/types/topic'
import TopicGrid from './TopicGrid.vue'
import TopicForm from './TopicForm.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const topicStore = useTopicStore()
const connectionStore = useConnectionStore()

const activeTab = ref('all')
const showCreateDialog = ref(false)
const showDeleteDialog = ref(false)
const deletingTopicName = ref<string | null>(null)

const currentConnection = computed(() => connectionStore.currentConnection)
const { topics, userTopics, internalTopics, loading, error } = topicStore

watch(currentConnection, (connection) => {
  if (connection) {
    topicStore.fetchTopics(connection.id)
  }
}, { immediate: true })

const handleTabClick = () => {
  // 탭 변경 시 추가 로직이 필요한 경우 여기에 구현
}

const handleRefresh = () => {
  if (currentConnection.value) {
    topicStore.fetchTopics(currentConnection.value.id)
  }
}

const handleSelect = (topic: TopicDto) => {
  topicStore.setCurrentTopic(null) // 상세 정보는 별도 페이지에서 로드
}

const handleDelete = (topicName: string) => {
  deletingTopicName.value = topicName
  showDeleteDialog.value = true
}

const handleCreated = (topic: TopicDto) => {
  ElMessage.success('토픽이 성공적으로 생성되었습니다.')
  showCreateDialog.value = false
}

const confirmDelete = async () => {
  if (!deletingTopicName.value || !currentConnection.value) return

  try {
    await topicStore.deleteTopic(currentConnection.value.id, deletingTopicName.value)
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

.header-actions {
  display: flex;
  gap: 12px;
}

.no-connection {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}
</style>