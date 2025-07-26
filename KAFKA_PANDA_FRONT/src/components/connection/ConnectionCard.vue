<template>
  <el-card class="connection-card" :class="{ active: isActive }" @click="handleClick">
    <template #header>
      <div class="card-header">
        <span class="connection-name">{{ connection.name }}</span>
        <ConnectionStatus :connection="connection" />
      </div>
    </template>

    <div class="card-content">
      <div class="connection-info">
        <p><strong>호스트:</strong> {{ connection.host }}:{{ connection.port }}</p>
        <p><strong>SSL:</strong> {{ connection.sslEnabled ? '활성화' : '비활성화' }}</p>
        <p><strong>SASL:</strong> {{ connection.saslEnabled ? '활성화' : '비활성화' }}</p>
        <p v-if="connection.username"><strong>사용자:</strong> {{ connection.username }}</p>
        <p><strong>생성일:</strong> {{ formatDate(connection.createdAt) }}</p>
        <p v-if="connection.lastConnected">
          <strong>마지막 연결:</strong> {{ formatDate(connection.lastConnected) }}
        </p>
      </div>

      <div class="card-actions">
        <el-button size="small" @click.stop="handleEdit">
          <el-icon><Edit /></el-icon>
          수정
        </el-button>
        <el-button size="small" type="danger" @click.stop="handleDelete">
          <el-icon><Delete /></el-icon>
          삭제
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import { Edit, Delete } from '@element-plus/icons-vue'
import type { ConnectionDto } from '@/types/connection'
import ConnectionStatus from './ConnectionStatus.vue'
import { formatDate } from '@/utils/formatters'

interface Props {
  connection: ConnectionDto
}

const props = defineProps<Props>()

const emit = defineEmits<{
  edit: [connection: ConnectionDto]
  delete: [connectionId: string]
  select: [connection: ConnectionDto]
}>()

const connectionStore = useConnectionStore()

const isActive = computed(() => 
  connectionStore.currentConnection?.id === props.connection.id
)

const handleClick = () => {
  emit('select', props.connection)
}

const handleEdit = () => {
  emit('edit', props.connection)
}

const handleDelete = () => {
  emit('delete', props.connection.id)
}
</script>

<style scoped>
.connection-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.connection-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.connection-card.active {
  border-color: #409EFF;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.connection-name {
  font-weight: bold;
  color: #303133;
}

.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.connection-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #606266;
}

.card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}
</style>