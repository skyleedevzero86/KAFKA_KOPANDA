<template>
  <el-dialog
    v-model="visible"
    title="메시지 검색"
    width="600px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="토픽" prop="topic">
        <el-select v-model="form.topic" placeholder="토픽을 선택하세요">
          <el-option
            v-for="topic in availableTopics"
            :key="topic.name"
            :label="topic.name"
            :value="topic.name"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="파티션">
        <el-select v-model="form.partition" placeholder="모든 파티션">
          <el-option label="모든 파티션" :value="undefined" />
          <el-option
            v-for="partition in availablePartitions"
            :key="partition"
            :label="`파티션 ${partition}`"
            :value="partition"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="키">
        <el-input v-model="form.key" placeholder="메시지 키로 검색" />
      </el-form-item>

      <el-form-item label="값">
        <el-input v-model="form.value" placeholder="메시지 값으로 검색" />
      </el-form-item>

      <el-form-item label="오프셋 범위">
        <el-input-number v-model="form.startOffset" placeholder="시작 오프셋" />
        <span style="margin: 0 8px;">~</span>
        <el-input-number v-model="form.endOffset" placeholder="끝 오프셋" />
      </el-form-item>

      <el-form-item label="시간 범위">
        <el-date-picker
          v-model="startTime"
          type="datetime"
          placeholder="시작 시간"
          @change="updateStartTime"
        />
        <span style="margin: 0 8px;">~</span>
        <el-date-picker
          v-model="endTime"
          type="datetime"
          placeholder="끝 시간"
          @change="updateEndTime"
        />
      </el-form-item>

      <el-form-item label="결과 개수">
        <el-input-number v-model="form.limit" :min="1" :max="1000" />
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">취소</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          검색
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import type { MessageSearchCriteria } from '@/types/message'
import type { TopicDto } from '@/types/topic'

interface Props {
  modelValue: boolean
  connectionId: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  search: [messages: any[]]
}>()

const messageStore = useMessageStore()
const topicStore = useTopicStore()
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

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

const startTime = ref<Date | null>(null)
const endTime = ref<Date | null>(null)

const availableTopics = computed(() => {
  if (!props.connectionId) return []
  return topicStore.topics.filter(topic => {
   
    return true
  })
})

const availablePartitions = computed(() => {
  if (!form.value.topic) return []
  const topic = availableTopics.value.find(t => t.name === form.value.topic)
  if (!topic) return []
  return Array.from({ length: topic.partitionCount }, (_, i) => i)
})

const updateStartTime = (date: Date | null) => {
  form.value.startTime = date ? date.getTime() : undefined
}

const updateEndTime = (date: Date | null) => {
  form.value.endTime = date ? date.getTime() : undefined
}

const handleSubmit = async () => {
  if (!form.value.topic) {
    ElMessage.warning('토픽을 선택하세요')
    return
  }

  if (!props.connectionId) {
    ElMessage.warning('연결을 선택하세요')
    return
  }

  loading.value = true
  try {
    const messages = await messageStore.searchMessages(props.connectionId, form.value)
    emit('search', messages)
    visible.value = false
  } catch (error) {
    console.error('메시지 검색 실패:', error)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  visible.value = false
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
  startTime.value = null
  endTime.value = null
}

watch(visible, async (newVal) => {
  if (newVal && props.connectionId) {
    await topicStore.fetchTopics(props.connectionId)
  }
})
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>