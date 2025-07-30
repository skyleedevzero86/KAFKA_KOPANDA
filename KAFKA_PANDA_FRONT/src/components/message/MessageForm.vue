<template>
  <el-dialog
    v-model="visible"
    title="메시지 전송"
    width="600px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="키">
        <el-input v-model="form.key" placeholder="메시지 키 (선택사항)" />
      </el-form-item>

      <el-form-item label="값" prop="value">
        <el-input
          v-model="form.value"
          type="textarea"
          :rows="6"
          placeholder="메시지 값을 입력하세요"
        />
      </el-form-item>

      <el-form-item label="파티션">
        <el-select v-model="form.partition" placeholder="자동 선택">
          <el-option label="자동 선택" :value="null" />
          <el-option
            v-for="partition in partitions"
            :key="partition.partitionNumber"
            :label="`파티션 ${partition.partitionNumber}`"
            :value="partition.partitionNumber"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="헤더">
        <div v-for="(value, key) in form.headers" :key="key" class="header-item">
          <el-input v-model="headerKeys[key]" placeholder="헤더 키" />
          <el-input v-model="form.headers[key]" placeholder="헤더 값" />
          <el-button type="danger" @click="removeHeader(key)">삭제</el-button>
        </div>
        <el-button @click="addHeader">헤더 추가</el-button>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">취소</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          전송
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { SendMessageRequest } from '@/types/message'
import type { PartitionDto } from '@/types/topic'

interface Props {
  modelValue: boolean
  connectionId: string
  topicName: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  sent: []
}>()

const messageStore = useMessageStore()
const topicStore = useTopicStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const form = ref<SendMessageRequest>({
  key: '',
  value: '',
  partition: null,
  headers: {}
})

const headerKeys = ref<Record<string, string>>({})

const rules: FormRules = {
  value: [
    { required: true, message: '메시지 값을 입력하세요', trigger: 'blur' }
  ]
}

const partitions = computed(() => {
  const topic = topicStore.currentTopic
  return topic?.partitions || []
})

const addHeader = () => {
  const key = `header_${Date.now()}`
  form.value.headers[key] = ''
  headerKeys.value[key] = ''
}

const removeHeader = (key: string) => {
  delete form.value.headers[key]
  delete headerKeys.value[key]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate()
  if (!valid) return

  loading.value = true
  try {
    // 헤더 키 매핑
    const mappedHeaders: Record<string, string> = {}
    Object.keys(form.value.headers).forEach(key => {
      const headerKey = headerKeys.value[key]
      if (headerKey) {
        mappedHeaders[headerKey] = form.value.headers[key]
      }
    })

    const request: SendMessageRequest = {
      key: form.value.key || undefined,
      value: form.value.value,
      partition: form.value.partition,
      headers: mappedHeaders
    }

    await messageStore.sendMessage(props.connectionId, props.topicName, request)
    emit('sent')
    visible.value = false
    resetForm()
  } catch (error) {
    // 에러는 store에서 처리됨
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    key: '',
    value: '',
    partition: null,
    headers: {}
  }
  headerKeys.value = {}
  formRef.value?.clearValidate()
}

const handleClose = () => {
  visible.value = false
  resetForm()
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.header-item {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
  align-items: center;
}

.header-item .el-input {
  flex: 1;
}
</style>