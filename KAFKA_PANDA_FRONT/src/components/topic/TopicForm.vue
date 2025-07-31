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
          :rows="4"
          placeholder="토픽 설정 (JSON 형식)"
          :disabled="loading"
          @input="updateConfig"
        />
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

const updateConfig = () => {
  try {
    form.value.config = JSON.parse(configText.value)
  } catch (e) {
    console.error('Invalid JSON format:', e)
  }
}

const resetForm = () => {
  form.value = {
    name: '',
    partitions: 3,
    replicationFactor: 1,
    config: {}
  }
  configText.value = '{}'
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

  try {
    await formRef.value.validate()
    loading.value = true

    updateConfig() 

    emit('submit', { ...form.value })
    visible.value = false
  } catch (error) {
    console.error('폼 검증 실패:', error)
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  visible.value = false
}

const handleClose = () => {
  visible.value = false
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>