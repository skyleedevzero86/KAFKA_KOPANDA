<template>
  <div class="messages-view">
    <div class="view-header">
      <h2>메시지 관리</h2>
      <p>Kafka 메시지를 관리합니다</p>
    </div>

    <div v-if="!currentConnection" class="no-connection">
      <el-empty description="연결된 클러스터가 없습니다">
        <el-button type="primary" @click="$router.push('/connections')">
          연결 관리로 이동
        </el-button>
      </el-empty>
    </div>

    <div v-else class="messages-content">
      <div class="messages-tabs">
        <el-tabs v-model="activeTab" type="card">
          <el-tab-pane label="메시지 전송" name="send">
            <MessageForm :connection-id="currentConnection.id" />
          </el-tab-pane>
          <el-tab-pane label="메시지 조회" name="list">
            <MessageList :connection-id="currentConnection.id" />
          </el-tab-pane>
          <el-tab-pane label="메시지 검색" name="search">
            <MessageSearch :connection-id="currentConnection.id" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useConnectionStore } from '@/stores/connection'
import MessageForm from '@/components/message/MessageForm.vue'
import MessageList from '@/components/message/MessageList.vue'
import MessageSearch from '@/components/message/MessageSearch.vue'

const connectionStore = useConnectionStore()
const activeTab = ref('send')

const currentConnection = computed(() => connectionStore.currentConnection)
</script>

<style scoped>
.messages-view {
  padding: 20px;
}

.view-header {
  margin-bottom: 30px;
}

.view-header h2 {
  margin: 0 0 8px 0;
  color: #303133;
}

.view-header p {
  margin: 0;
  color: #909399;
}

.no-connection {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.messages-content {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.messages-tabs {
  padding: 20px;
}
</style>