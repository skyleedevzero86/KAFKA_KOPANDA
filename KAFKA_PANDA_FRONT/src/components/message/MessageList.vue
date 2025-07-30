<template>
  <div class="message-list">
    <div class="message-header">
      <h3>메시지 목록</h3>
      <div class="header-actions">
        <el-button @click="showSearchDialog = true">
          <el-icon><Search /></el-icon>
          검색
        </el-button>
        <el-button type="primary" @click="showSendDialog = true" :disabled="!currentTopic">
          <el-icon><Plus /></el-icon>
          메시지 전송
        </el-button>
      </div>
    </div>

    <div v-if="!currentConnection || !currentTopic" class="no-selection">
      <el-empty description="메시지를 보려면 연결과 토픽을 선택하세요" />
    </div>

    <div v-else>
      <div class="message-controls">
        <el-form :model="controls" inline>
          <el-form-item label="파티션">
            <el-select v-model="controls.partition" @change="loadMessages">
              <el-option
                v-for="partition in partitions"
                :key="partition.partitionNumber"
                :label="`파티션 ${partition.partitionNumber}`"
                :value="partition.partitionNumber"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="오프셋 타입">
            <el-select v-model="controls.offsetType" @change="loadMessages">
              <el-option label="가장 최근" value="LATEST" />
              <el-option label="가장 오래된" value="EARLIEST" />
              <el-option label="특정 오프셋" value="SPECIFIC" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="controls.offsetType === 'SPECIFIC'" label="오프셋">
            <el-input-number v-model="controls.offset" @change="loadMessages" />
          </el-form-item>
          <el-form-item label="개수">
            <el-input-number v-model="controls.limit" :min="1" :max="1000" @change="loadMessages" />
          </el-form-item>
        </el-form>
      </div>

      <div class="message-table">
        <el-table :data="messages" style="width: 100%" v-loading="loading">
          <el-table-column prop="offset" label="오프셋" width="100" />
          <el-table-column prop="partition" label="파티션" width="80" />
          <el-table-column prop="key" label="키" width="200" />
          <el-table-column prop="value" label="값" min-width="300" show-overflow-tooltip />
          <el-table-column prop="formattedTime" label="시간" width="180" />
          <el-table-column label="헤더" width="100">
            <template #default="{ row }">
              <el-button v-if="Object.keys(row.headers).length > 0" size="small" @click="showHeaders(row)">
                헤더 보기
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div v-if="pagination" class="message-pagination">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <MessageForm
      v-if="currentConnection && currentTopic"
      v-model="showSendDialog"
      :connection-id="currentConnection.id"
      :topic-name="currentTopic.name"
      @sent="handleSent"
    />

    <MessageSearch
      v-if="currentConnection"
      v-model="showSearchDialog"
      :connection-id="currentConnection.id"
      @search="handleSearch"
    />

    <el-dialog v-model="showHeadersDialog" title="메시지 헤더" width="400px">
      <el-descriptions :column="1" border>
        <el-descriptions-item
          v-for="(value, key) in selectedMessageHeaders"
          :key="key"
          :label="key"
        >
          {{ value }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useConnectionStore } from '@/stores/connection'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import type { MessageDto, OffsetType } from '@/types/message'
import type { PartitionDto } from '@/types/topic'
import MessageForm from './MessageForm.vue'
import MessageSearch from './MessageSearch.vue'

const messageStore = useMessageStore()
const connectionStore = useConnectionStore()
const topicStore = useTopicStore()

const showSendDialog = ref(false)
const showSearchDialog = ref(false)
const showHeadersDialog = ref(false)
const selectedMessageHeaders = ref<Record<string, string>>({})

const currentConnection = computed(() => connectionStore.currentConnection)
const currentTopic = computed(() => topicStore.currentTopic)

const controls = ref({
  partition: 0,
  offsetType: 'LATEST' as OffsetType,
  offset: null as number | null,
  limit: 100
})

const { messages, pagination, loading, error } = messageStore

const partitions = computed(() => {
  if (!currentTopic.value) return []
  return currentTopic.value.partitions
})

watch([currentConnection, currentTopic], ([connection, topic]) => {
  if (connection && topic) {
    controls.value.partition = topic.partitions[0]?.partitionNumber || 0
    loadMessages()
  }
}, { immediate: true })

const loadMessages = async () => {
  if (!currentConnection.value || !currentTopic.value) return

  try {
    await messageStore.getMessages(
      currentConnection.value.id,
      currentTopic.value.name,
      controls.value.partition,
      controls.value.offset,
      controls.value.offsetType,
      controls.value.limit
    )
  } catch (error) {
    // 에러는 store에서 처리됨
  }
}

const handleSent = () => {
  ElMessage.success('메시지가 성공적으로 전송되었습니다.')
  showSendDialog.value = false
  loadMessages()
}

const handleSearch = (searchMessages: MessageDto[]) => {
  messageStore.messages = searchMessages
  showSearchDialog.value = false
}

const showHeaders = (message: MessageDto) => {
  selectedMessageHeaders.value = message.headers
  showHeadersDialog.value = true
}

const handleSizeChange = (size: number) => {
  if (pagination.value) {
    pagination.value.pageSize = size
    loadMessages()
  }
}

const handleCurrentChange = (page: number) => {
  if (pagination.value) {
    pagination.value.page = page
    loadMessages()
  }
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

.no-selection {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.message-controls {
  margin-bottom: 20px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.message-table {
  margin-bottom: 20px;
}

.message-pagination {
  display: flex;
  justify-content: center;
}
</style>