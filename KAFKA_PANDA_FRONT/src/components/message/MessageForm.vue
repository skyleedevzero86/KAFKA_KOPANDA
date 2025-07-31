<template>
  <el-dialog
    v-model="visible"
    title="메시지 전송"
    width="700px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="연결">
        <el-input :value="connectionName" disabled />
      </el-form-item>
      
      <el-form-item label="토픽">
        <el-input :value="topicName" disabled />
      </el-form-item>

      <el-form-item label="값" prop="value" required>
        <el-input
          v-model="form.value"
          type="textarea"
          :rows="4"
          placeholder="메시지 내용을 입력하세요"
        />
      </el-form-item>

      <el-form-item label="키">
        <el-input
          v-model="form.key"
          placeholder="메시지 키 (선택사항)"
        />
      </el-form-item>

      <el-form-item label="파티션">
        <el-select v-model="form.partition" placeholder="자동 선택">
          <el-option label="자동 선택" :value="undefined" />
          <el-option
            v-for="partition in availablePartitions"
            :key="partition"
            :label="`파티션 ${partition}`"
            :value="partition"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="헤더">
        <div class="headers-container">
          <div
            v-for="(value, key) in form.headers"
            :key="key"
            class="header-item"
          >
            <el-input v-model="headerKeys[key]" placeholder="키" style="width: 150px;" />
            <el-input v-model="form.headers[key]" placeholder="값" style="flex: 1;" />
            <el-button @click="removeHeader(key)" type="danger" size="small">
              삭제
            </el-button>
          </div>
          <el-button @click="addHeader" type="primary" size="small">
            헤더 추가
          </el-button>
        </div>
      </el-form-item>

      <el-form-item label="미리보기">
        <div class="message-preview">
          <div class="preview-item">
            <strong>값:</strong> {{ form.value || '입력된 값이 없습니다' }}
          </div>
          <div v-if="form.key" class="preview-item">
            <strong>키:</strong> {{ form.key }}
          </div>
          <div v-if="Object.keys(form.headers).length > 0" class="preview-item">
            <strong>헤더:</strong>
            <div class="headers-preview">
              <div
                v-for="(value, key) in form.headers"
                :key="key"
                class="header-preview"
              >
                {{ key }}: {{ value }}
              </div>
            </div>
          </div>
        </div>
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
import { ref, computed, watch } from 'vue'
import { useMessageStore } from '@/stores/message'
import { useConnectionStore } from '@/stores/connection'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import type { SendMessageRequest } from '@/types/message'

interface Props {
  modelValue: boolean
  connectionId?: string
  topicName?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  sent: []
}>()

const messageStore = useMessageStore()
const connectionStore = useConnectionStore()
const topicStore = useTopicStore()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const loading = ref(false)
const formRef = ref()

const form = ref<SendMessageRequest>({
  key: '',
  value: '',
  partition: undefined,
  headers: {}
})

const headerKeys = ref<Record<string, string>>({})

const connectionName = computed(() => {
  if (!props.connectionId) return ''
  const connection = connectionStore.connections.find(c => c.id === props.connectionId)
  return connection?.name || ''
})

const topicName = computed(() => props.topicName || '')

const currentTopicDetails = computed(() => topicStore.currentTopic)

const availablePartitions = computed(() => {
  if (!currentTopicDetails.value) return []
  return Array.from({ length: currentTopicDetails.value.partitionCount }, (_, i) => i)
})

const rules = {
  value: [
    { required: true, message: '메시지 값은 필수입니다', trigger: 'blur' }
  ]
}

const addHeader = () => {
  const key = `header_${Object.keys(form.value.headers).length}`
  form.value.headers[key] = ''
  headerKeys.value[key] = ''
}

const removeHeader = (key: string) => {
  delete form.value.headers[key]
  delete headerKeys.value[key]
}

watch(headerKeys, (newKeys) => {
  Object.keys(newKeys).forEach(key => {
    if (newKeys[key] && newKeys[key] !== key) {
      const value = form.value.headers[key]
      delete form.value.headers[key]
      form.value.headers[newKeys[key]] = value
      headerKeys.value[newKeys[key]] = newKeys[key]
      delete headerKeys.value[key]
    }
  })
}, { deep: true })

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate()
  if (!valid) return

  if (!props.connectionId || !props.topicName) {
    ElMessage.error('연결과 토픽을 선택해주세요')
    return
  }

  loading.value = true
  try {
    await messageStore.sendMessage(props.connectionId, props.topicName, form.value)
    ElMessage.success('메시지가 성공적으로 전송되었습니다.')
    emit('sent')
    visible.value = false
    resetForm()
  } catch (error) {
    ElMessage.error('메시지 전송에 실패했습니다.')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.value = {
    key: '',
    value: '',
    partition: undefined,
    headers: {}
  }
  headerKeys.value = {}
  formRef.value?.clearValidate()
}

const handleClose = () => {
  visible.value = false
  resetForm()
}

watch(visible, (newVal) => {
  if (newVal && props.connectionId && props.topicName) {
  
    topicStore.getTopicDetails(props.connectionId, props.topicName)
  }
})
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

.message-preview {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  margin-top: 8px;
}

.preview-item {
  margin-bottom: 8px;
}

.preview-item:last-child {
  margin-bottom: 0;
}

.headers-preview {
  margin-top: 4px;
  padding-left: 12px;
}

.header-preview {
  font-size: 12px;
  color: #606266;
  margin-bottom: 2px;
}

.headers-container {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
}
</style>