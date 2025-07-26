<template>
  <div class="connection-status">
    <el-tag
      :type="statusType"
      :icon="statusIcon"
      size="small"
    >
      {{ statusText }}
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Connection, Warning, CircleClose, Loading } from '@element-plus/icons-vue'
import type { ConnectionDto } from '@/types/connection'

interface Props {
  connection: ConnectionDto
}

const props = defineProps<Props>()

const statusType = computed(() => {
  if (props.connection.lastConnected) {
    return 'success'
  }
  return 'info'
})

const statusIcon = computed(() => {
  if (props.connection.lastConnected) {
    return Connection
  }
  return CircleClose
})

const statusText = computed(() => {
  if (props.connection.lastConnected) {
    return '연결됨'
  }
  return '연결 안됨'
})
</script>

<style scoped>
.connection-status {
  display: flex;
  align-items: center;
}
</style>
