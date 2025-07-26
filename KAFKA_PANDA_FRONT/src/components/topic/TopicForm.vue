<template>
  <el-dialog
    v-model="visible"
    title="새 토픽 생성"
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
      <el-form-item label="토픽 이름" prop="name">
        <el-input v-model="form.name" placeholder="토픽 이름을 입력하세요" />
      </el-form-item>

      <el-form-item label="파티션 수" prop="partitions">
        <el-input-number
          v-model="form.partitions"
          :min="1"
          :max="1000"
          placeholder="1"
        />
      </el-form-item>

      <el-form-item label="복제 팩터" prop="replicationFactor">
        <el-input-number
          v-model="form.replicationFactor"
          :min="1"
          :max="10"
          placeholder="1"
        />
      </el-form-item>

      <el-form-item label="설정">
        <div class="config-section">
          <div
            v-for="(config, index) in form.configs"
            :key="index"
            class="config-row"
          >
            <el-input
              v-model="config.key"
              placeholder="키"
              style="width: 40%"
            />
            <el-input
              v-model="config.value"
              placeholder="값"
              style="width: 40%"
            />
            <el-button
              type="danger"
              size="small"
              @click="removeConfig(index)"
            >
              삭제
            </el-button>
          </div>
          <el-button
            type="primary"
            size="small"
            @click="addConfig"
          >
            설정 추가
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleCancel">취소</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="loading">
          생성
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { useTopicStore } from '@/stores/topic'
import { validateTopicConfig } from '@/utils/validators'
import type { CreateTopicRequest } from '@/types/topic'

interface Props {
  modelValue: boolean
  connectionId: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  created: [topic: any]
}>()

const topicStore = useTopicStore()
const formRef = ref()
const visible = ref(props.modelValue)
const loading = ref(false)

const form = ref<CreateTopicRequest & { configs: Array<{ key: string; value: string }> }>({
  name: '',
  partitions: 1,
  replicationFactor: 1,
  config: {},
  configs: []
})

const rules = {
  name: [
    { required: true, message: '토픽 이름을 입력해주세요', trigger: 'blur' },
    { min: 1, max: 249, message: '토픽 이름은 1-249자 사이여야 합니다', trigger: 'blur' }
  ],
  partitions: [
    { required: true, message: '파티션 수를 입력해주세요', trigger: 'blur' },
    { type: 'number', min: 1, max: 1000, message: '파티션 수는 1-1000 사이여야 합니다', trigger: 'blur' }
  ],
  replicationFactor: [
    { required: true, message: '복제 팩터를 입력해주세요', trigger: 'blur' },
    { type: 'number', min: 1, max: 10, message: '복제 팩터는 1-10 사이여야 합니다', trigger: 'blur' }
  ]
}

watch(() => props.modelValue, (newValue) => {
  visible.value = newValue
})

watch(visible, (newValue) => {
  emit('update:modelValue', newValue)
})

const addConfig = () => {
  form.value.configs.push({ key: '', value: '' })
}

const removeConfig = (index: number) => {
  form.value.configs.splice(index, 1)
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    
    const validation = validateTopicConfig({
      name: form.value.name,
      partitions: form.value.partitions,
      replicationFactor: form.value.replicationFactor
    })
    
    if (!validation.isValid) {
      ElMessage.error(validation.errors[0])
      return
    }

    loading.value = true

    // configs를 config 객체로 변환
    const config: Record<string, string> = {}
    form.value.configs.forEach(item => {
      if (item.key && item.value) {
        config[item.key] = item.value
      }
    })

    const createRequest: CreateTopicRequest = {
      name: form.value.name,
      partitions: form.value.partitions,
      replicationFactor: form.value.replicationFactor,
      config
    }

    const newTopic = await topicStore.createTopic(props.connectionId, createRequest)
    emit('created', newTopic)

    visible.value = false
    resetForm()
  } catch (error) {
    // 에러는 store에서 처리됨
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

const resetForm = () => {
  form.value = {
    name: '',
    partitions: 1,
    replicationFactor: 1,
    config: {},
    configs: []
  }
  formRef.value?.clearValidate()
}
</script>

<style scoped>
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.config-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>