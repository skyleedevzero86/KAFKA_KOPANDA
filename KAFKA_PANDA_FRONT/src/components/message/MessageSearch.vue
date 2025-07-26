<template>
  <el-card class="message-search">
    <template #header>
      <div class="search-header">
        <h3>메시지 검색</h3>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      label-width="100px"
      @submit.prevent="handleSearch"
    >
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="토픽" prop="topic">
            <el-select
              v-model="form.topic"
              placeholder="토픽을 선택하세요"
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

        <el-col :span="12">
          <el-form-item label="파티션">
            <el-input-number
              v-model="form.partition"
              :min="0"
              :max="999"
              placeholder="모든 파티션"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="키">
            <el-input
              v-model="form.key"
              placeholder="메시지 키로 검색"
            />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="값">
            <el-input
              v-model="form.value"
              placeholder="메시지 값으로 검색"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="시작 오프셋">
            <el-input-number
              v-model="form.startOffset"
              :min="0"
              placeholder="시작 오프셋"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="끝 오프셋">
            <el-input-number
              v-model="form.endOffset"
              :min="0"
              placeholder="끝 오프셋"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="시작 시간">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              placeholder="시작 시간 선택"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>

        <el-col :span="12">
          <el-form-item label="끝 시간">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              placeholder="끝 시간 선택"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="결과 개수">
        <el-input-number
          v-model="form.limit"
          :min="1"
          :max="1000"
          :default-value="100"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch" :loading="loading">
          검색
        </el-button>
        <el-button @click="handleReset">초기화</el-button>
      </el-form-item>
    </el-form>

    <div v-if="searchResults.length > 0" class="search-results">
      <h4>검색 결과 ({{ searchResults.length }}개)</h4>
      <div class="results-container">
        <MessageCard
          v-for="message in searchResults"
          :key="`${message.partition}-${message.offset}`"
          :message="message"
        />
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useMessageStore } from '@/stores/message'
import { useTopicStore } from '@/stores/topic'
import type { MessageSearchCriteria } from '@/types/message'
import MessageCard from './MessageCard.vue'

interface Props {
  connectionId: string
}

const props = defineProps<Props>()

const messageStore = useMessageStore()
const topicStore = useTopicStore()
const formRef = ref()
const loading = ref(false)
const searchResults = ref([])

const form = ref<MessageSearchCriteria>({
  topic: '',
  partition: undefined,
  key: '',
  value: '',
  startOffset: undefined,
  endOffset: undefined,
  startTime: undefined,
  endTime: undefined,
  limit: 100
})

const topics = computed(() => topicStore.topics)

onMounted(async () => {
  try {
    await topicStore.fetchTopics(props.connectionId)
  } catch (error) {
    ElMessage.error('토픽 목록을 불러오는데 실패했습니다.')
  }
})

const handleSearch = async () => {
  if (!form.value.topic) {
    ElMessage.warning('토픽을 선택해주세요.')
    return
  }

  try {
    loading.value = true
    
    const criteria: MessageSearchCriteria = {
      topic: form.value.topic,
      partition: form.value.partition,
      key: form.value.key || undefined,
      value: form.value.value || undefined,
      startOffset: form.value.startOffset,
      endOffset: form.value.endOffset,
      startTime: form.value.startTime ? new Date(form.value.startTime).getTime() : undefined,
      endTime: form.value.endTime ? new Date(form.value.endTime).getTime() : undefined,
      limit: form.value.limit
    }

    const results = await messageStore.searchMessages(props.connectionId, criteria)
    searchResults.value = results
    
    ElMessage.success(`${results.length}개의 메시지를 찾았습니다.`)
  } catch (error) {
    ElMessage.error('메시지 검색에 실패했습니다.')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  form.value = {
    topic: '',
    partition: undefined,
    key: '',
    value: '',
    startOffset: undefined,
    endOffset: undefined,
    startTime: undefined,
    endTime: undefined,
    limit: 100
  }
  searchResults.value = []
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.message-search {
  margin-bottom: 20px;
}

.search-header h3 {
  margin: 0;
  color: #303133;
}

.search-results {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e6e6e6;
}

.search-results h4 {
  margin: 0 0 16px 0;
  color: #303133;
}

.results-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>