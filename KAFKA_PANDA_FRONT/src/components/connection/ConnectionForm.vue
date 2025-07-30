<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '연결 수정' : '새 연결 생성'"
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
      <el-form-item label="연결 이름" prop="name">
        <el-input v-model="form.name" placeholder="연결 이름을 입력하세요" />
      </el-form-item>

      <el-form-item label="호스트" prop="host">
        <el-input v-model="form.host" placeholder="localhost" />
      </el-form-item>

      <el-form-item label="포트" prop="port">
        <el-input-number v-model="form.port" :min="1" :max="65535" />
      </el-form-item>

      <el-form-item label="SSL">
        <el-switch v-model="form.sslEnabled" />
      </el-form-item>

      <el-form-item label="SASL">
        <el-switch v-model="form.saslEnabled" />
      </el-form-item>

      <template v-if="form.saslEnabled">
        <el-form-item label="사용자명" prop="username">
          <el-input v-model="form.username" placeholder="사용자명을 입력하세요" />
        </el-form-item>

        <el-form-item label="비밀번호" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="비밀번호를 입력하세요"
            show-password
          />
        </el-form-item>
      </template>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">취소</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          {{ isEdit ? '수정' : '생성' }}
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { ConnectionDto, CreateConnectionRequest, UpdateConnectionRequest } from '@/types/connection'

interface Props {
  modelValue: boolean
  connection?: ConnectionDto | null
}

const props = withDefaults(defineProps<Props>(), {
  connection: null
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [connection: ConnectionDto]
  updated: [connection: ConnectionDto]
}>()

const connectionStore = useConnectionStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const isEdit = computed(() => !!props.connection)

const form = ref<CreateConnectionRequest>({
  name: '',
  host: 'localhost',
  port: 9092,
  sslEnabled: false,
  saslEnabled: false,
  username: '',
  password: ''
})

const rules: FormRules = {
  name: [
    { required: true, message: '연결 이름을 입력하세요', trigger: 'blur' },
    { min: 1, max: 100, message: '연결 이름은 1-100자 사이여야 합니다', trigger: 'blur' }
  ],
  host: [
    { required: true, message: '호스트를 입력하세요', trigger: 'blur' }
  ],
  port: [
    { required: true, message: '포트를 입력하세요', trigger: 'blur' },
    { type: 'number', min: 1, max: 65535, message: '포트는 1-65535 사이여야 합니다', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '사용자명을 입력하세요', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '비밀번호를 입력하세요', trigger: 'blur' }
  ]
}

watch(() => props.connection, (connection) => {
  if (connection) {
    form.value = {
      name: connection.name,
      host: connection.host,
      port: connection.port,
      sslEnabled: connection.sslEnabled,
      saslEnabled: connection.saslEnabled,
      username: connection.username || '',
      password: ''
    }
  } else {
    resetForm()
  }
}, { immediate: true })

const resetForm = () => {
  form.value = {
    name: '',
    host: 'localhost',
    port: 9092,
    sslEnabled: false,
    saslEnabled: false,
    username: '',
    password: ''
  }
  formRef.value?.clearValidate()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  const valid = await formRef.value.validate()
  if (!valid) return

  loading.value = true
  try {
    if (isEdit.value && props.connection) {
      const updateRequest: UpdateConnectionRequest = {
        name: form.value.name,
        host: form.value.host,
        port: form.value.port,
        sslEnabled: form.value.sslEnabled,
        saslEnabled: form.value.saslEnabled,
        username: form.value.username || undefined,
        password: form.value.password || undefined
      }
      const updatedConnection = await connectionStore.updateConnection(props.connection.id, updateRequest)
      emit('updated', updatedConnection)
    } else {
      const newConnection = await connectionStore.createConnection(form.value)
      emit('created', newConnection)
    }
    visible.value = false
  } catch (error) {
    // 에러는 store에서 처리됨
  } finally {
    loading.value = false
  }
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
</style>