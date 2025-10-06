<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '토픽 수정' : '토픽 생성'"
    width="500px"
    :before-close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="120px"
      @submit.prevent="handleSubmit"
    >
      <el-form-item label="토픽 이름" prop="name">
        <el-input
          v-model="form.name"
          placeholder="토픽 이름을 입력하세요"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="파티션 수" prop="partitions">
        <el-input-number
          v-model="form.partitions"
          :min="1"
          :max="100"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="복제 팩터" prop="replicationFactor">
        <el-input-number
          v-model="form.replicationFactor"
          :min="1"
          :max="10"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="설정">
        <el-input
          v-model="configText"
          type="textarea"
          :rows="6"
          placeholder="토픽 설정 (JSON 형식)"
          :disabled="loading"
          @input="handleConfigInput"
        />
        <div class="form-help">
          <small>
            <strong>올바른 JSON 형식 예시:</strong><br>
            <code>{"cleanup.policy": "delete", "retention.ms": "604800000"}</code><br>
            <code>{}</code> (빈 설정)
          </small>
        </div>
        <div v-if="configError" class="config-error">
          <el-alert
            :title="configError"
            type="error"
            :closable="false"
            show-icon
            size="small"
          />
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel" :disabled="loading">
          취소
        </el-button>
        <el-button
          type="primary"
          @click="handleSubmit"
          :loading="loading"
          :disabled="!!configError"
        >
          {{ isEdit ? '수정' : '생성' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { TopicDto, CreateTopicRequest } from '@/types/topic'

interface Props {
  modelValue: boolean
  topic?: TopicDto
  connectionId?: string
}

const props = withDefaults(defineProps<Props>(), {
  topic: undefined,
  connectionId: ''
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [data: CreateTopicRequest]
}>()

const visible = ref(props.modelValue)
const loading = ref(false)
const formRef = ref<FormInstance>()
const configError = ref<string>('')

const isEdit = computed(() => !!props.topic)

const form = ref<CreateTopicRequest>({
  name: '',
  partitions: 3,
  replicationFactor: 1,
  config: {}
})

const configText = ref('{}')

const rules: FormRules = {
  name: [
    { required: true, message: '토픽 이름을 입력하세요', trigger: 'blur' },
    { min: 1, max: 50, message: '토픽 이름은 1-50자 사이여야 합니다', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._-]+$/, message: '토픽 이름은 영문자, 숫자, 점, 언더스코어, 하이픈만 사용 가능합니다', trigger: 'blur' }
  ],
  partitions: [
    { required: true, message: '파티션 수를 입력하세요', trigger: 'blur' },
    { type: 'number', min: 1, max: 100, message: '파티션 수는 1-100 사이여야 합니다', trigger: 'blur' }
  ],
  replicationFactor: [
    { required: true, message: '복제 팩터를 입력하세요', trigger: 'blur' },
    { type: 'number', min: 1, max: 10, message: '복제 팩터는 1-10 사이여야 합니다', trigger: 'blur' }
  ]
}

const handleConfigInput = () => {
  configError.value = ''
  
  if (!configText.value.trim()) {
    form.value.config = {}
    return
  }
  
  try {
    const parsed = JSON.parse(configText.value)
    
    if (typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)) {
      form.value.config = parsed
      console.log('JSON 설정 파싱 성공:', parsed)
    } else {
      configError.value = '설정은 객체 형태여야 합니다'
      form.value.config = {}
    }
  } catch (e: any) {
    configError.value = `JSON 형식 오류: ${e.message}`
    form.value.config = {}
    console.error('JSON 파싱 실패:', e.message)
  }
}

const updateConfig = () => {
  handleConfigInput()
}

const resetForm = () => {
  form.value = {
    name: '',
    partitions: 3,
    replicationFactor: 1,
    config: {}
  }
  configText.value = '{}'
  configError.value = ''
  formRef.value?.clearValidate()
}

watch(() => props.modelValue, (newValue) => {
  visible.value = newValue
})

watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

watch(() => props.topic, (topic) => {
  if (topic) {
    form.value = {
      name: topic.name,
      partitions: topic.partitionCount,
      replicationFactor: topic.replicationFactor,
      config: {}
    }
    configText.value = '{}'
    configError.value = ''
  } else {
    resetForm()
  }
}, { immediate: true })

const handleSubmit = async () => {
  if (!formRef.value) return

  if (!props.connectionId) {
    ElMessage.error('연결을 선택해주세요')
    return
  }

  if (configError.value) {
    ElMessage.error('설정 필드의 JSON 형식을 수정해주세요')
    return
  }

  try {
    await formRef.value.validate()
    loading.value = true

    updateConfig()

    if (!form.value.name.trim()) {
      ElMessage.error('토픽 이름을 입력해주세요')
      return
    }

    console.log('토픽 생성 요청:', {
      connectionId: props.connectionId,
      topicData: form.value
    })

    emit('submit', { ...form.value })
    visible.value = false
    resetForm()
  } catch (error) {
    console.error('폼 검증 실패:', error)
    ElMessage.error('폼 검증에 실패했습니다')
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  visible.value = false
  resetForm()
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

.form-help {
  margin-top: 8px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.form-help small {
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
}

.form-help code {
  background-color: #f0f0f0;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 11px;
}

.config-error {
  margin-top: 8px;
}

:deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}
</style>