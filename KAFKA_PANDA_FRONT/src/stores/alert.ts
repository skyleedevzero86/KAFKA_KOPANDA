import { ref, computed } from 'vue'
import { alertService } from '@/services/alertService'
import type { AlertDto } from '@/types/alert'
import { IssueSeverity } from '@/types/alert'

const alerts = ref<AlertDto[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

export function useAlertStore() {
  async function fetchAlerts(connectionId?: string) {
    try {
      loading.value = true
      error.value = null
      alerts.value = await alertService.getActiveAlerts(connectionId)
    } catch (err: any) {
      error.value = err.message || '알림을 불러오는데 실패했습니다.'
      console.error('Failed to fetch alerts:', err)
    } finally {
      loading.value = false
    }
  }

  async function fetchAlertsBySeverity(severity: IssueSeverity) {
    try {
      loading.value = true
      error.value = null
      alerts.value = await alertService.getAlertsBySeverity(severity)
    } catch (err: any) {
      error.value = err.message || '알림을 불러오는데 실패했습니다.'
      console.error('Failed to fetch alerts by severity:', err)
    } finally {
      loading.value = false
    }
  }

  async function acknowledgeAlert(alertId: string, acknowledgedBy: string) {
    try {
      const updatedAlert = await alertService.acknowledgeAlert({
        alertId,
        acknowledgedBy
      })
      
      const index = alerts.value.findIndex(alert => alert.id === alertId)
      if (index !== -1) {
        alerts.value[index] = updatedAlert
      }
      
      return updatedAlert
    } catch (err: any) {
      error.value = err.message || '알림 확인 처리에 실패했습니다.'
      console.error('Failed to acknowledge alert:', err)
      throw err
    }
  }

  async function clearAllAlerts(connectionId?: string) {
    try {
      await alertService.clearAllAlerts({ connectionId })
      alerts.value = []
    } catch (err: any) {
      error.value = err.message || '알림 삭제에 실패했습니다.'
      console.error('Failed to clear alerts:', err)
      throw err
    }
  }

  async function checkAndCreateAlerts(connectionId: string) {
    try {
      await alertService.checkAndCreateAlerts(connectionId)
      await fetchAlerts(connectionId)
    } catch (err: any) {
      error.value = err.message || '알림 체크에 실패했습니다.'
      console.error('Failed to check and create alerts:', err)
      throw err
    }
  }

  async function createTestAlert(connectionId: string) {
    try {
      const testAlert = await alertService.createTestAlert(connectionId)
      alerts.value.unshift(testAlert)
      return testAlert
    } catch (err: any) {
      error.value = err.message || '테스트 알림 생성에 실패했습니다.'
      console.error('Failed to create test alert:', err)
      throw err
    }
  }

  function removeAlert(alertId: string) {
    alerts.value = alerts.value.filter(alert => alert.id !== alertId)
  }

  function clearError() {
    error.value = null
  }

  function clearAlerts() {
    alerts.value = []
  }

  const activeAlerts = computed(() => 
    alerts.value.filter(alert => !alert.isAcknowledged)
  )

  const acknowledgedAlerts = computed(() => 
    alerts.value.filter(alert => alert.isAcknowledged)
  )

  const alertsBySeverity = computed(() => {
    const grouped: Record<IssueSeverity, AlertDto[]> = {
      [IssueSeverity.LOW]: [],
      [IssueSeverity.MEDIUM]: [],
      [IssueSeverity.HIGH]: [],
      [IssueSeverity.CRITICAL]: []
    }
    
    alerts.value.forEach(alert => {
      grouped[alert.severity].push(alert)
    })
    
    return grouped
  })

  const criticalAlerts = computed(() => 
    alerts.value.filter(alert => alert.severity === IssueSeverity.CRITICAL && !alert.isAcknowledged)
  )

  const highPriorityAlerts = computed(() => 
    alerts.value.filter(alert => 
      (alert.severity === IssueSeverity.CRITICAL || alert.severity === IssueSeverity.HIGH) && 
      !alert.isAcknowledged
    )
  )

  const alertCount = computed(() => alerts.value.length)
  const activeAlertCount = computed(() => activeAlerts.value.length)

  return {
    alerts: computed(() => alerts.value),
    loading: computed(() => loading.value),
    error: computed(() => error.value),
    
    activeAlerts,
    acknowledgedAlerts,
    alertsBySeverity,
    criticalAlerts,
    highPriorityAlerts,
    alertCount,
    activeAlertCount,
    
    fetchAlerts,
    fetchAlertsBySeverity,
    acknowledgeAlert,
    clearAllAlerts,
    checkAndCreateAlerts,
    createTestAlert,
    removeAlert,
    clearError,
    clearAlerts
  }
}