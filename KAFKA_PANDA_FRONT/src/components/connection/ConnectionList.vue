<template>
  <div class="connection-list">
    <div class="list-header">
      <h3>연결 관리</h3>
      <div class="header-actions">
        <el-button @click="refreshConnections">
          <Refresh />
          새로고침
        </el-button>
        <el-button type="primary" @click="showCreateDialog = true">
          <Plus />
          새 연결
        </el-button>
      </div>
    </div>

    <div v-if="loading" class="loading-container">
      <LoadingSpinner />
    </div>

    <div v-else-if="error" class="error-container">
      <ErrorMessage :message="error" @retry="refreshConnections" />
    </div>

    <div v-else-if="connections.length === 0" class="empty-container">
      <el-empty description="연결이 없습니다">
        <el-button type="primary" @click="showCreateDialog = true">
          첫 번째 연결 만들기
        </el-button>
      </el-empty>
    </div>

    <div v-else class="connections-grid">
      <ConnectionCard
        v-for="connection in connections"
        :key="connection.id"
        :connection="connection"
        @edit="handleEdit"
        @delete="handleDelete"
        @test="handleTest"
      />
    </div>

    <ConnectionForm
      v-model="showCreateDialog"
      :editing-connection="editingConnection || undefined"
      @created="handleCreated"
      @updated="handleUpdated"
    />

    <ConfirmDialog
      v-model="showDeleteDialog"
      title="연결 삭제"
      :message="`'${deletingConnectionName}' 연결을 삭제하시겠습니까?`"
      @confirm="confirmDelete"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useConnectionStore } from '@/stores/connection'
import type { ConnectionDto, CreateConnectionRequest, UpdateConnectionRequest } from '@/types/connection'
import ConnectionCard from './ConnectionCard.vue'
import ConnectionForm from './ConnectionForm.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

const connectionStore = useConnectionStore()

const showCreateDialog = ref(false)
const showDeleteDialog = ref(false)
const editingConnection = ref<ConnectionDto | null>(null)
const deletingConnectionName = ref('')
const isDeleting = ref(false)

const { connections, loading, error } = connectionStore

const refreshConnections = async () => {
  await connectionStore.fetchConnections()
}

const handleEdit = (connection: ConnectionDto) => {
  editingConnection.value = connection
  showCreateDialog.value = true
}

const handleDelete = (connectionName: string) => {
  deletingConnectionName.value = connectionName
  showDeleteDialog.value = true
}

const handleTest = async (connection: ConnectionDto) => {
  try {
    const result = await connectionStore.testConnection({
      name: connection.name,
      host: connection.host,
      port: connection.port,
      sslEnabled: connection.sslEnabled,
      saslEnabled: connection.saslEnabled,
      username: connection.username,
      password: ''
    })
    
    if (result.success) {
      ElMessage.success('연결 테스트 성공!')
    } else {
      ElMessage.error(`연결 테스트 실패: ${result.message}`)
    }
  } catch (error) {
    console.error('연결 테스트 실패:', error)
    ElMessage.error('연결 테스트 중 오류가 발생했습니다.')
  }
}

const handleCreated = async (data: CreateConnectionRequest) => {
  try {
    await connectionStore.createConnection(data)
    ElMessage.success('연결이 생성되었습니다.')
    showCreateDialog.value = false
  } catch (error) {
    console.error('연결 생성 실패:', error)
  }
}

const handleUpdated = async (data: UpdateConnectionRequest) => {
  if (!editingConnection.value) return

  try {
    await connectionStore.updateConnection(editingConnection.value.id, data)
    ElMessage.success('연결이 수정되었습니다.')
    showCreateDialog.value = false
    editingConnection.value = null
  } catch (error) {
    console.error('연결 수정 실패:', error)
  }
}

const confirmDelete = async () => {
  if (!deletingConnectionName.value) return

  isDeleting.value = true
  try {
    const connectionToDelete = connections.find(c => c.name === deletingConnectionName.value)
    if (connectionToDelete) {
      await connectionStore.deleteConnection(connectionToDelete.id)
      ElMessage.success('연결이 삭제되었습니다.')
    }
  } catch (error) {
    console.error('연결 삭제 실패:', error)
    ElMessage.error('연결 삭제에 실패했습니다.')
  } finally {
    isDeleting.value = false
  }
}

const clearError = () => {
  connectionStore.clearError()
}

watch(connections, (newConnections) => {
  console.log('연결 목록 변경됨:', newConnections.length)
  

  if (newConnections.length === 1) {
    connectionStore.setCurrentConnection(newConnections[0])
    console.log(' 자동으로 연결 선택됨:', newConnections[0])
    ElMessage.success(`'${newConnections[0].name}' 연결이 자동 선택되었습니다.`)
  }
 
  else if (newConnections.length > 1 && !connectionStore.currentConnection) {
    connectionStore.setCurrentConnection(newConnections[0])
    console.log(' 첫 번째 연결 선택됨:', newConnections[0])
    ElMessage.success(`'${newConnections[0].name}' 연결이 선택되었습니다.`)
  }
}, { immediate: true })

onMounted(async () => {
  console.log('연결 목록 로드 시작...')
  await connectionStore.fetchConnections()
  
  if (connectionStore.connections.length > 0 && !connectionStore.currentConnection) {
    const firstConnection = connectionStore.connections[0]
    connectionStore.setCurrentConnection(firstConnection)
    console.log(' 마운트 시 연결 자동 선택:', firstConnection)
    ElMessage.success(`'${firstConnection.name}' 연결이 자동 선택되었습니다.`)
  }
})
</script>

<style scoped>
.connection-list {
  padding: 20px;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.list-header h3 {
  margin: 0;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.loading-container,
.error-container,
.empty-container {
  padding: 40px;
  text-align: center;
}

.connections-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}
</style>