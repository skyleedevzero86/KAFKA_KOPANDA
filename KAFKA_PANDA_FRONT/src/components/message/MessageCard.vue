<template>
  <el-card class="message-card">
    <template #header>
      <div class="message-header">
        <span class="message-offset">Offset: {{ formatOffset(message.offset) }}</span>
        <span class="message-partition">Partition {{ message.partition }}</span>
        <span class="message-time">{{ formatDate(message.timestamp) }}</span>
      </div>
    </template>

    <div class="message-content">
      <div v-if="message.key" class="message-key">
        <strong>Key:</strong>
        <el-input
          :value="message.key"
          readonly
          size="small"
          class="key-input"
        />
      </div>

      <div class="message-value">
        <strong>Value:</strong>
        <el-input
          :value="message.value"
          type="textarea"
          :rows="4"
          readonly
          class="value-input"
        />
      </div>

      <div v-if="Object.keys(message.headers).length > 0" class="message-headers">
        <strong>Headers:</strong>
        <div class="headers-list">
          <div
            v-for="(value, key) in message.headers"
            :key="key"
            class="header-item"
          >
            <span class="header-key">{{ key }}:</span>
            <span class="header-value">{{ value }}</span>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="message-footer">
        <el-tag
          :type="message.consumed ? 'success' : 'warning'"
          size="small"
        >
          {{ message.consumed ? '소비됨' : '미소비' }}
        </el-tag>
      </div>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import type { MessageDto } from '@/types/message'
import { formatOffset, formatDate } from '@/utils/formatters'

interface Props {
  message: MessageDto
}

defineProps<Props>()
</script>

<style scoped>
.message-card {
  margin-bottom: 16px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #606266;
}

.message-offset {
  font-weight: bold;
  color: #409EFF;
}

.message-partition {
  color: #909399;
}

.message-time {
  color: #909399;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.message-key,
.message-value {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.key-input,
.value-input {
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

.headers-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
}

.header-item {
  display: flex;
  gap: 8px;
  font-size: 12px;
  font-family: 'Courier New', monospace;
}

.header-key {
  color: #409EFF;
  font-weight: bold;
  min-width: 80px;
}

.header-value {
  color: #606266;
}

.message-footer {
  display: flex;
  justify-content: flex-end;
}
</style>
