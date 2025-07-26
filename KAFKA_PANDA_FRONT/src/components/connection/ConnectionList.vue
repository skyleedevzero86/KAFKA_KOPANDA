<template>
  <div class="connection-list">
    <div class="connection-header">
      <h3>연결 목록</h3>
      <el-button type="primary" @click="showCreateDialog = true">
        <el-icon><Plus /></el-icon>
        새 연결
      </el-button>
    </div>

    <el-row :gutter="16">
      <el-col
        v-for="connection in connections"
        :key="connection.id"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <ConnectionCard
          :connection="connection"
          @edit="handleEdit"
          @delete="handleDelete"
          @select="handleSelect"
        />
      </el-col>
    </el-row>

    <ConnectionForm
      v-model="showCreateDialog"
      @created="handleCreated"
    />

    <ConnectionForm
      v-if="editingConnection"
      v-model="showEditDialog"
      :connection="editingConnection"
      @updated="handleUpdated"
    />

    <ConfirmDialog
      v-model="showDeleteDialog"
      title="연결 삭제"
      message="정말로 이 연결을 삭제하시겠습니까?"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import type { ConnectionDto } from '@/types/connection'
import ConnectionCard from './ConnectionCard.vue'
import ConnectionForm from './ConnectionForm.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const connectionStore = useConnectionStore()

const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const showDeleteDialog = ref(false)
const editingConnection = ref<ConnectionDto | null>(null)
const deletingConnectionId = ref<string | null>(null)

const { connections, loading, error } = connectionStore

onMounted(() => {
  connectionStore.fetchConnections()
})

const handleEdit = (connection: ConnectionDto) => {
  editingConnection.value = connection
  showEditDialog.value = true
}

const handleDelete = (connectionId: string) => {
  deletingConnectionId.value = connectionId
  showDeleteDialog.value = true
}

const handleSelect = (connection: ConnectionDto) => {
  connectionStore.setCurrentConnection(connection)
}

const handleCreated = (connection: ConnectionDto) => {
  ElMessage.success('연결이 성공적으로 생성되었습니다.')
  showCreateDialog.value = false
}

const handleUpdated = (connection: ConnectionDto) => {
  ElMessage.success('연결이 성공적으로 수정되었습니다.')
  showEditDialog.value = false
  editingConnection.value = null
}

const confirmDelete = async () => {
  if (!deletingConnectionId.value) return

  try {
    await connectionStore.deleteConnection(deletingConnectionId.value)
    ElMessage.success('연결이 성공적으로 삭제되었습니다.')
    showDeleteDialog.value = false
    deletingConnectionId.value = null
  } catch (error) {
    // 에러는 store에서 처리예정
  }
}
</script>

<style scoped>
.connection-list {
  padding: 20px;
}

.connection-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.connection-header h3 {
  margin: 0;
  color: #303133;
}
</style>