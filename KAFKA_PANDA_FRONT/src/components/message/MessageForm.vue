<template>
  <el-card class="message-form">
    <template #header>
      <div class="form-header">
        <h3>메시지 전송</h3>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
      @submit.prevent="handleSubmit"
    >
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

      <el-form-item label="키 (선택)">
        <el-input
          v-model="form.key"
          placeholder="메시지 키를 입력하세요"
        />
      </el-form-item>

      <el-form-item label="값" prop="value">
        <el-input
          v-model="form.value"
          type="textarea"
          :rows="6"
          placeholder="메시지 값을 입력하세요"
        />
      </el-form-item>

      <el-form-item label="파티션 (선택)">
        <el-input-number
          v-model="form.partition"
          :min="0"
          :max="999"
          placeholder="자동 할당"
        />
      </el-form-item>

      <el-form-item label="헤더">
        <div class="headers-section">
          <div
            v-for="(header, index) in form.headers"
            :key="index"
            class="header-row"
          >
            <el-input
              v-model="header.key"
              placeholder="키"
              style="width: 40%"
            />
            <el-input
              v-model="header.value"
              placeholder="값"
              style="width: 40%"
            />
            <el-button
              type="danger"
              size="small"
              @click="removeHeader(index)"
            >
              삭제
            </el-button>
          </div>
          <el-button
            type="primary"
            size="small"
            @click="addHeader"
          >
            헤더 추가
          </el-button>
        </div>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          메시지 전송
        </el-button>
        <el-button @click="handleReset">초기화</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useMessageStore } from '@/stores/message'
import { useTopicStore } from '@/stores/topic'
import { validateMessageConfig } from '@/utils/validators'
import type { SendMessageRequest } from '@/types/message'
import type { TopicDto } from '@/types/topic'

interface Props {
  connectionId: string
}

const props = defineProps<Props>()

const messageStore = useMessageStore()
const topicStore = useTopicStore()
const formRef = ref()
const loading = ref(false)

const form = ref<SendMessageRequest & { topic: string }>({
  topic: '',
  key: '',
  value: '',
  partition: undefined,
  headers: {}
})

const rules = {
  topic: [
    { required: true, message: '토픽을 선택해주세요', trigger: 'change' }
  ],
  value: [
    { required: true, message: '메시지 값을 입력해주세요', trigger: 'blur' }
  ]
}

const topics = computed(() => topicStore.topics)

const addHeader = () => {
  form.value.headers = {
    ...form.value.headers,
    [`header_${Object.keys(form.value.headers).length}`]: ''
  }
}

const removeHeader = (key: string) => {
  const newHeaders = { ...form.value.headers }
  delete newHeaders[key]
  form.value.headers = newHeaders
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    const validation = validateMessageConfig({
      topic: form.value.topic,
      value: form.value.value,
      key: form.value.key,
      partition: form.value.partition
    })
    
    if (!validation.isValid) {
      ElMessage.error(validation.errors[0])
      return
    }

    loading.value = true

    await messageStore.sendMessage(
      props.connectionId,
      form.value.topic,
      {
        key: form.value.key || undefined,
        value: form.value.value,
        partition: form.value.partition,
        headers: form.value.headers
      }
    )

    ElMessage.success('메시지가 성공적으로 전송되었습니다.')
    handleReset()
  } catch (error) {
    // 에러는 store에서 처리예정
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  form.value = {
    topic: '',
    key: '',
    value: '',
    partition: undefined,
    headers: {}
  }
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.message-form {
  margin-bottom: 20px;
}

.form-header h3 {
  margin: 0;
  color: #303133;
}

.headers-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>