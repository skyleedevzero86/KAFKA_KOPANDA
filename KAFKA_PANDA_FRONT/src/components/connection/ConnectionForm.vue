<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '연결 수정' : '연결 생성'"
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
        <el-input
          v-model="form.name"
          placeholder="연결 이름을 입력하세요"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="호스트" prop="host">
        <el-input
          v-model="form.host"
          placeholder="호스트를 입력하세요 (예: localhost)"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="포트" prop="port">
        <el-input-number
          v-model="form.port"
          :min="1"
          :max="65535"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item label="SSL 사용">
        <el-switch
          v-model="form.sslEnabled"
          :disabled="loading"
        />
        <div class="form-help">
          <small>Kafka 서버가 SSL을 사용하는 경우에만 활성화하세요</small>
        </div>
      </el-form-item>

      <el-form-item label="SASL 사용">
        <el-switch
          v-model="form.saslEnabled"
          :disabled="loading"
        />
        <div class="form-help">
          <small>Kafka 서버가 인증을 사용하는 경우에만 활성화하세요</small>
        </div>
      </el-form-item>

      <el-form-item
        v-if="form.saslEnabled"
        label="사용자명"
        prop="username"
      >
        <el-input
          v-model="form.username"
          placeholder="사용자명을 입력하세요"
          :disabled="loading"
        />
      </el-form-item>

      <el-form-item
        v-if="form.saslEnabled"
        label="비밀번호"
        prop="password"
      >
        <el-input
          v-model="form.password"
          type="password"
          placeholder="비밀번호를 입력하세요"
          :disabled="loading"
          show-password
        />
      </el-form-item>

      <el-form-item>
        <el-alert
          title="연결 정보"
          type="info"
          :closable="false"
          show-icon
        >
          <template #default>
            <p><strong>연결 문자열:</strong> {{ form.host }}:{{ form.port }}</p>
            <p><strong>보안:</strong> {{ getSecurityInfo() }}</p>
          </template>
        </el-alert>
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
import type { ConnectionDto, CreateConnectionRequest, UpdateConnectionRequest } from '@/types/connection'

interface Props {
  modelValue: boolean
  connection?: ConnectionDto
}

const props = withDefaults(defineProps<Props>(), {
  connection: undefined
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [data: CreateConnectionRequest | UpdateConnectionRequest]
}>()

const visible = ref(props.modelValue)
const loading = ref(false)
const formRef = ref<FormInstance>()

const isEdit = computed(() => !!props.connection)

const form = ref<CreateConnectionRequest>({
  name: '',
  host: 'localhost',
  port: 9092, 
  sslEnabled: false,
  saslEnabled: false,
  username: undefined,
  password: undefined
})

const rules: FormRules = {
  name: [
    { required: true, message: '연결 이름을 입력하세요', trigger: 'blur' },
    { min: 1, max: 50, message: '연결 이름은 1-50자 사이여야 합니다', trigger: 'blur' }
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

const getSecurityInfo = () => {
  if (form.value.sslEnabled && form.value.saslEnabled) {
    return 'SSL + SASL'
  } else if (form.value.sslEnabled) {
    return 'SSL'
  } else if (form.value.saslEnabled) {
    return 'SASL'
  } else {
    return '없음 (PLAINTEXT)'
  }
}

const resetForm = () => {
  form.value = {
    name: '',
    host: 'localhost',
    port: 9092,
    sslEnabled: false,
    saslEnabled: false,
    username: undefined,
    password: undefined
  }
  formRef.value?.clearValidate()
}

watch(() => props.modelValue, (newValue) => {
  visible.value = newValue
})

watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

watch(() => props.connection, (connection) => {
  if (connection) {
    form.value = {
      name: connection.name,
      host: connection.host,
      port: connection.port,
      sslEnabled: connection.sslEnabled,
      saslEnabled: connection.saslEnabled,
      username: connection.username,
      password: undefined
    }
  } else {
    resetForm()
  }
}, { immediate: true })

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    loading.value = true

    const submitData = isEdit.value
      ? { ...form.value, id: props.connection!.id } as UpdateConnectionRequest
      : form.value as CreateConnectionRequest

    emit('submit', submitData)
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

.form-help {
  margin-top: 4px;
}

.form-help small {
  color: #909399;
  font-size: 12px;
}
</style>