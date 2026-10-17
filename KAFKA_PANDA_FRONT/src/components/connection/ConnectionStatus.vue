<template>
  <el-tag
    :type="statusType"
    size="small"
    :class="{ 'status-tag': true }"
  >
    {{ statusText }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ConnectionDto } from '@/types/connection'

interface Props {
  connection: ConnectionDto
}

const props = defineProps<Props>()

const isUp = computed(() => {
  const last = props.connection.lastConnected
  if (last && last !== 'Invalid Date') return true
  return Boolean(props.connection.host && props.connection.port)
})

const statusType = computed(() => (isUp.value ? 'success' : 'danger'))

const statusText = computed(() => (isUp.value ? '연결됨' : '연결 안됨'))
</script>

<style scoped>
.status-tag {
  font-weight: 500;
}
</style>