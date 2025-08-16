<template>
  <div class="real-time-notification">
    <div class="notification-icon" @click="showNotificationPanel = !showNotificationPanel">
      <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification-badge">
        <el-icon class="notification-icon-svg">
          <Bell />
        </el-icon>
      </el-badge>
    </div>

    <div v-if="showNotificationPanel" class="notification-panel">
      <div class="notification-header">
        <h3>실시간 알림</h3>
        <div class="notification-actions">
          <el-button 
            type="text" 
            size="small" 
            @click="markAllAsRead"
            :disabled="unreadCount === 0"
          >
            모두 읽음
          </el-button>
          <el-button 
            type="text" 
            size="small" 
            @click="clearAllNotifications"
            :disabled="notifications.length === 0"
          >
            모두 삭제
          </el-button>
        </div>
      </div>

      <div class="notification-list">
        <div v-if="notifications.length === 0" class="empty-notifications">
          <el-empty description="현재 알림이 없습니다" :image-size="60">
            <template #image>
              <el-icon class="empty-icon">
                <Bell />
              </el-icon>
            </template>
          </el-empty>
        </div>

        <div 
          v-for="notification in notifications" 
          :key="notification.id"
          class="notification-item"
          :class="{ 
            'unread': !notification.isRead,
            [`severity-${notification.severity.toLowerCase()}`]: true
          }"
          @click="markAsRead(notification.id)"
        >
          <div class="notification-content">
            <div class="notification-header-row">
              <span class="notification-title">{{ notification.title }}</span>
              <span class="notification-time">{{ formatTime(notification.timestamp) }}</span>
            </div>
            <p class="notification-message">{{ notification.message }}</p>
            <div v-if="notification.topicName" class="notification-topic">
              토픽: {{ notification.topicName }}
            </div>
          </div>
          <div class="notification-actions">
            <el-button 
              type="text" 
              size="small" 
              @click.stop="deleteNotification(notification.id)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { Bell, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { apiService } from '@/services/api'

interface Notification {
  id: string
  type: string
  title: string
  message: string
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  topicName?: string
  connectionId?: string
  timestamp: string
  isRead: boolean
  details: Record<string, any>
}

const notifications = ref<Notification[]>([])
const showNotificationPanel = ref(false)
const loading = ref(false)

const unreadCount = computed(() => 
  notifications.value.filter(n => !n.isRead).length
)

const fetchNotifications = async () => {
  try {
    loading.value = true
    const response = await apiService.get<Notification[]>('/notifications')
    notifications.value = response
  } catch (error) {
    console.error('알림 조회 실패:', error)
    ElMessage.error('알림을 불러오는데 실패했습니다.')
  } finally {
    loading.value = false
  }
}

const markAsRead = async (notificationId: string) => {
  try {
    await apiService.post(`/notifications/${notificationId}/read`)
    const notification = notifications.value.find(n => n.id === notificationId)
    if (notification) {
      notification.isRead = true
    }
  } catch (error) {
    console.error('알림 읽음 처리 실패:', error)
  }
}

const markAllAsRead = async () => {
  try {
    await apiService.post('/notifications/read-all')
    notifications.value.forEach(n => n.isRead = true)
    ElMessage.success('모든 알림을 읽음 처리했습니다.')
  } catch (error) {
    console.error('모든 알림 읽음 처리 실패:', error)
    ElMessage.error('알림 읽음 처리에 실패했습니다.')
  }
}

const deleteNotification = async (notificationId: string) => {
  try {
    await apiService.delete(`/notifications/${notificationId}`)
    notifications.value = notifications.value.filter(n => n.id !== notificationId)
    ElMessage.success('알림을 삭제했습니다.')
  } catch (error) {
    console.error('알림 삭제 실패:', error)
    ElMessage.error('알림 삭제에 실패했습니다.')
  }
}

const clearAllNotifications = async () => {
  try {
    await apiService.delete('/notifications')
    notifications.value = []
    ElMessage.success('모든 알림을 삭제했습니다.')
  } catch (error) {
    console.error('모든 알림 삭제 실패:', error)
    ElMessage.error('알림 삭제에 실패했습니다.')
  }
}

const formatTime = (timestamp: string) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  
  if (diff < 60000) {
    return '방금 전'
  } else if (diff < 3600000) { 
    return `${Math.floor(diff / 60000)}분 전`
  } else if (diff < 86400000) { 
    return `${Math.floor(diff / 3600000)}시간 전`
  } else {
    return date.toLocaleDateString('ko-KR')
  }
}

let refreshInterval: NodeJS.Timeout | null = null

onMounted(() => {
  fetchNotifications()
  refreshInterval = setInterval(fetchNotifications, 10000) 
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped>
.real-time-notification {
  position: relative;
}

.notification-icon {
  cursor: pointer;
  padding: 8px;
  border-radius: 50%;
  transition: background-color 0.3s;
}

.notification-icon:hover {
  background-color: rgba(0, 0, 0, 0.1);
}

.notification-icon-svg {
  font-size: 20px;
  color: #606266;
}

.notification-badge {
  margin-right: 0;
}

.notification-panel {
  position: absolute;
  top: 100%;
  right: 0;
  width: 400px;
  max-height: 500px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  background: #fafafa;
}

.notification-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.notification-actions {
  display: flex;
  gap: 8px;
}

.notification-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-notifications {
  padding: 40px 20px;
  text-align: center;
}

.empty-icon {
  font-size: 60px;
  color: #c0c4cc;
}

.notification-item {
  display: flex;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.notification-item:hover {
  background-color: #f5f7fa;
}

.notification-item.unread {
  background-color: #f0f9ff;
  border-left: 4px solid #409eff;
}

.notification-item.severity-critical {
  border-left: 4px solid #f56c6c;
}

.notification-item.severity-high {
  border-left: 4px solid #e6a23c;
}

.notification-item.severity-medium {
  border-left: 4px solid #409eff;
}

.notification-item.severity-low {
  border-left: 4px solid #67c23a;
}

.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 4px;
}

.notification-title {
  font-weight: 600;
  color: #303133;
  font-size: 14px;
  line-height: 1.4;
}

.notification-time {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  margin-left: 8px;
}

.notification-message {
  margin: 4px 0;
  font-size: 13px;
  color: #606266;
  line-height: 1.4;
  word-break: break-word;
}

.notification-topic {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.notification-actions {
  display: flex;
  align-items: center;
  margin-left: 8px;
}

.notification-list::-webkit-scrollbar {
  width: 6px;
}

.notification-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.notification-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.notification-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}
</style>

