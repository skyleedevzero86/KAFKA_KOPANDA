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

const statusType = computed(() => {
  if (props.connection.lastConnected && props.connection.lastConnected !== 'Invalid Date') {
    return 'success'
  }
  return 'danger'
})

const statusText = computed(() => {
  if (props.connection.lastConnected && props.connection.lastConnected !== 'Invalid Date') {
    return '연결됨'
  }
  return '연결 안됨'
})
</script>

<style scoped>
.status-tag {
  font-weight: 500;
}
</style>