<template>
  <div class="message-list">
    <div class="message-controls">
      <el-form :inline="true" class="message-filters">
        <el-form-item label="토픽">
          <el-select
            v-model="selectedTopic"
            placeholder="토픽을 선택하세요"
            @change="handleTopicChange"
          >
            <el-option
              v-for="topic in topics"
              :key="topic.name"
              :label="topic.name"
              :value="topic.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="파티션">
          <el-select
            v-model="selectedPartition"
            placeholder="파티션을 선택하세요"
            @change="loadMessages"
          >
            <el-option
              v-for="partition in availablePartitions"
              :key="partition"
              :label="`Partition ${partition}`"
              :value="partition"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="오프셋 타입">
          <el-select v-model="offsetType" @change="loadMessages">
            <el-option label="최신" value="LATEST" />
            <el-option label="최초" value="EARLIEST" />
            <el-option label="특정" value="SPECIFIC" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="offsetType === 'SPECIFIC'" label="오프셋">
          <el-input-number
            v-model="specificOffset"
            :min="0"
            placeholder="오프셋 입력"
            @change="loadMessages"
          />
        </el-form-item>

        <el-form-item label="개수">
          <el-input-number
            v-model="messageLimit"
            :min="1"
            :max="1000"
            @change="loadMessages"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="loadMessages" :loading="loading">
            메시지 로드
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="loading" class="loading-container">
      <LoadingSpinner message="메시지를 불러오는 중..." />
    </div>

    <div v-else-if="messages.length === 0" class="empty-state">
      <el-empty description="메시지가 없습니다" />
    </div>

    <div v-else class="messages-container">
      <MessageCard
        v-for="message in messages"
        :key="`${message.partition}-${message.offset}`"
        :message="message"
      />
    </div>

    <div v-if="pagination" class="pagination-container">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next"
        @size-change="handlePageSizeChange"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import type { OffsetType } from '@/types/message'
import MessageCard from './MessageCard.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

interface Props {
  connectionId: string
}

const props = defineProps<Props>()

const messageStore = useMessageStore()
const topicStore = useTopicStore()

const selectedTopic = ref('')
const selectedPartition = ref(0)
const offsetType = ref<OffsetType>('LATEST')
const specificOffset = ref<number | null>(null)
const messageLimit = ref(50)
const currentPage = ref(1)
const pageSize = ref(20)

const { messages, pagination, loading, error } = messageStore

const topics = computed(() => topicStore.topics)

const availablePartitions = computed(() => {
  if (!selectedTopic.value) return []
  const topic = topics.value.find(t => t.name === selectedTopic.value)
  if (!topic) return []
  
  const partitions = []
  for (let i = 0; i < topic.partitionCount; i++) {
    partitions.push(i)
  }
  return partitions
})

onMounted(async () => {
  try {
    await topicStore.fetchTopics(props.connectionId)
  } catch (error) {
    ElMessage.error('토픽 목록을 불러오는데 실패했습니다.')
  }
})

const handleTopicChange = () => {
  selectedPartition.value = 0
  loadMessages()
}

const loadMessages = async () => {
  if (!selectedTopic.value || selectedPartition.value === undefined) return

  try {
    const offset = offsetType.value === 'SPECIFIC' ? specificOffset.value : null
    
    await messageStore.getMessages(
      props.connectionId,
      selectedTopic.value,
      selectedPartition.value,
      offset,
      offsetType.value,
      messageLimit.value
    )
  } catch (error) {
    ElMessage.error('메시지를 불러오는데 실패했습니다.')
  }
}

const handlePageSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadMessages()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadMessages()
}
</script>

<style scoped>
.message-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-controls {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.message-filters {
  margin: 0;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding: 40px;
}

.messages-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>