<template>
  <el-dialog
    v-model="visible"
    title="새 토픽 생성"
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
        <el-input v-model="form.name" placeholder="토픽 이름을 입력하세요" />
      </el-form-item>

      <el-form-item label="파티션 수" prop="partitions">
        <el-input-number v-model="form.partitions" :min="1" :max="100" />
      </el-form-item>

      <el-form-item label="복제 팩터" prop="replicationFactor">
        <el-input-number v-model="form.replicationFactor" :min="1" :max="10" />
      </el-form-item>

      <el-form-item label="설정">
        <el-button @click="addConfig">설정 추가</el-button>
        <div v-for="(value, key) in form.config" :key="key" class="config-item">
          <el-input v-model="configKeys[key]" placeholder="키" />
          <el-input v-model="form.config[key]" placeholder="값" />
          <el-button type="danger" @click="removeConfig(key)">삭제</el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">취소</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          생성
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useTopicStore } from '@/stores/topic'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { TopicDto, CreateTopicRequest } from '@/types/topic'

interface Props {
  modelValue: boolean
  connectionId: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [topic: TopicDto]
}>()

const topicStore = useTopicStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const form = ref<CreateTopicRequest>({
  name: '',
  partitions: 3,
  replicationFactor: 1,
  config: {}
})

const configKeys = ref<Record<string, string>>({})

const rules: FormRules = {
  name: [
    { required: true, message: '토픽 이름을 입력하세요', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9._-]+$/, message: '토픽 이름은 영문, 숫자, 점, 언더스코어, 하이픈만 사용 가능합니다', trigger: 'blur' }
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

const addConfig = () => {
  const key = `config_${Date.now()}`
  form.value.config[key] = ''
  configKeys.value[key] = ''
}

const removeConfig = (key: string) => {
  delete form.value.config[key]
  delete configKeys.value[key]
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate()
  if (!valid) return

  loading.value = true
  try {
    const newTopic = await topicStore.createTopic(props.connectionId, form.value)
    emit('created', newTopic)
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
    name: '',
    partitions: 3,
    replicationFactor: 1,
    config: {}
  }
  configKeys.value = {}
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

.config-item {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: center;
}

.config-item .el-input {
  flex: 1;
}
</style>