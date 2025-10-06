<template>
  <div class="alert-list">
    <div class="alert-header">
      <h4>모니터링 알림</h4>
      <div class="header-actions">
        <el-button 
          size="small" 
          @click="createTestAlert" 
          :loading="loading"
          type="primary"
        >
          <el-icon><Plus /></el-icon>
          테스트 알림
        </el-button>
        <el-button 
          size="small" 
          @click="clearAllAlerts" 
          :disabled="alerts.length === 0"
          type="danger"
        >
          <el-icon><Delete /></el-icon>
          모든 알림 지우기
        </el-button>
      </div>
    </div>
    
    <div v-if="loading" class="loading-container">
      <LoadingSpinner message="알림을 불러오는 중..." />
    </div>
    
    <div v-else-if="error" class="error-container">
      <ErrorMessage :message="error" @close="clearError" />
    </div>
    
    <div v-else-if="alerts.length === 0" class="no-alerts">
      <el-empty description="현재 알림이 없습니다" :image-size="60">
        <el-button @click="createTestAlert" type="primary">
          테스트 알림 생성
        </el-button>
      </el-empty>
    </div>
    
    <div v-else class="alerts-container">
      <div 
        v-for="alert in alerts" 
        :key="alert.id"
        class="alert-item"
        :class="[
          `alert-${alert.severity.toLowerCase()}`,
          { 'alert-acknowledged': alert.isAcknowledged }
        ]"
      >
        <div class="alert-content">
          <div class="alert-header-info">
            <div class="alert-title-row">
              <span class="alert-title">{{ alert.title }}</span>
              <el-tag 
                :type="getSeverityType(alert.severity)" 
                size="small"
                class="severity-tag"
              >
                {{ getSeverityLabel(alert.severity) }}
              </el-tag>
            </div>
            <div class="alert-meta">
              <span class="alert-time">{{ formatTime(alert.timestamp) }}</span>
              <span v-if="alert.connectionId" class="alert-connection">
                연결: {{ alert.connectionId.substring(0, 8) }}...
              </span>
              <span v-if="alert.topicName" class="alert-topic">
                토픽: {{ alert.topicName }}
              </span>
            </div>
          </div>
          
          <p class="alert-message">{{ alert.message }}</p>
          
          <div v-if="alert.isAcknowledged" class="alert-acknowledged-info">
            <el-icon><Check /></el-icon>
            <span>확인됨 - {{ alert.acknowledgedBy }} ({{ formatTime(alert.acknowledgedAt || '') }})</span>
          </div>
        </div>
        
        <div class="alert-actions">
          <el-button 
            v-if="!alert.isAcknowledged"
            size="small" 
            type="success"
            @click="acknowledgeAlert(alert.id)"
            :loading="acknowledgingIds.has(alert.id)"
          >
            <el-icon><Check /></el-icon>
            확인
          </el-button>
          <el-button 
            size="small" 
            type="danger"
            @click="removeAlert(alert.id)"
          >
            <el-icon><Delete /></el-icon>
            삭제
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useAlertStore } from '@/stores/alert'
import { useConnectionStore } from '@/stores/connection'
import { ElMessage } from 'element-plus'
import { Plus, Delete, Check } from '@element-plus/icons-vue'
import LoadingSpinner from './LoadingSpinner.vue'
import ErrorMessage from './ErrorMessage.vue'
import { IssueSeverity } from '@/types/alert'

const alertStore = useAlertStore()
const connectionStore = useConnectionStore()

const acknowledgingIds = ref<Set<string>>(new Set())

const alerts = computed(() => alertStore.activeAlerts.value)
const loading = computed(() => alertStore.loading.value)
const error = computed(() => alertStore.error.value)

const currentConnection = computed(() => connectionStore.currentConnection)

onMounted(() => {
  if (currentConnection.value) {
    fetchAlerts()
  }
})

const fetchAlerts = async () => {
  if (currentConnection.value) {
    await alertStore.fetchAlerts(currentConnection.value.id)
  }
}

const createTestAlert = async () => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }
  
  try {
    await alertStore.createTestAlert(currentConnection.value.id)
    ElMessage.success('테스트 알림이 생성되었습니다')
  } catch (err: any) {
    ElMessage.error(err.message || '테스트 알림 생성에 실패했습니다')
  }
}

const acknowledgeAlert = async (alertId: string) => {
  try {
    acknowledgingIds.value.add(alertId)
    await alertStore.acknowledgeAlert(alertId, '사용자')
    ElMessage.success('알림이 확인되었습니다')
  } catch (err: any) {
    ElMessage.error(err.message || '알림 확인에 실패했습니다')
  } finally {
    acknowledgingIds.value.delete(alertId)
  }
}

const removeAlert = (alertId: string) => {
  alertStore.removeAlert(alertId)
  ElMessage.info('알림이 삭제되었습니다')
}

const clearAllAlerts = async () => {
  if (!currentConnection.value) {
    ElMessage.error('연결을 선택해주세요')
    return
  }
  
  try {
    await alertStore.clearAllAlerts(currentConnection.value.id)
    ElMessage.success('모든 알림이 삭제되었습니다')
  } catch (err: any) {
    ElMessage.error(err.message || '알림 삭제에 실패했습니다')
  }
}

const clearError = () => {
  alertStore.clearError()
}

const getSeverityType = (severity: IssueSeverity) => {
  switch (severity) {
    case IssueSeverity.CRITICAL:
      return 'danger'
    case IssueSeverity.HIGH:
      return 'warning'
    case IssueSeverity.MEDIUM:
      return 'info'
    case IssueSeverity.LOW:
      return 'success'
    default:
      return 'info'
  }
}

const getSeverityLabel = (severity: IssueSeverity) => {
  switch (severity) {
    case IssueSeverity.CRITICAL:
      return '치명적'
    case IssueSeverity.HIGH:
      return '높음'
    case IssueSeverity.MEDIUM:
      return '보통'
    case IssueSeverity.LOW:
      return '낮음'
    default:
      return '알 수 없음'
  }
}

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  return date.toLocaleString('ko-KR')
}
</script>

<style scoped>
.alert-list {
  width: 100%;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e4e7ed;
}

.alert-header h4 {
  margin: 0;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.loading-container,
.error-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 200px;
}

.no-alerts {
  padding: 40px 20px;
  text-align: center;
}

.alerts-container {
  max-height: 500px;
  overflow-y: auto;
}

.alert-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px;
  margin-bottom: 12px;
  border-radius: 8px;
  border-left: 4px solid;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.alert-item:hover {
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.alert-item.alert-critical {
  border-left-color: #f56c6c;
  background-color: #fef0f0;
}

.alert-item.alert-high {
  border-left-color: #e6a23c;
  background-color: #fdf6ec;
}

.alert-item.alert-medium {
  border-left-color: #409eff;
  background-color: #f0f9ff;
}

.alert-item.alert-low {
  border-left-color: #67c23a;
  background-color: #f0f9ff;
}

.alert-item.alert-acknowledged {
  opacity: 0.6;
  background-color: #f5f7fa;
}

.alert-content {
  flex: 1;
  margin-right: 12px;
}

.alert-header-info {
  margin-bottom: 8px;
}

.alert-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.alert-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
}

.severity-tag {
  font-size: 10px;
  height: 20px;
  line-height: 18px;
}

.alert-meta {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #909399;
}

.alert-message {
  margin: 0 0 8px 0;
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.alert-acknowledged-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}

.alert-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.alert-actions .el-button {
  min-width: 60px;
}
</style>
