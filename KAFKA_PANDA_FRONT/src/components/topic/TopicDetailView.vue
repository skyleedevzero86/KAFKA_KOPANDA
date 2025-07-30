<template>
  <div class="topic-detail-view">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="토픽명">{{ topic.name }}</el-descriptions-item>
      <el-descriptions-item label="파티션 수">{{ topic.partitionCount }}</el-descriptions-item>
      <el-descriptions-item label="복제 팩터">{{ topic.replicationFactor }}</el-descriptions-item>
      <el-descriptions-item label="메시지 수">{{ formatNumber(topic.messageCount) }}</el-descriptions-item>
      <el-descriptions-item label="내부 토픽">{{ topic.isInternal ? '예' : '아니오' }}</el-descriptions-item>
      <el-descriptions-item label="상태">
        <el-tag :type="topic.isHealthy ? 'success' : 'danger'">
          {{ topic.isHealthy ? '정상' : '오류' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="생성일">{{ formatDate(topic.createdAt) }}</el-descriptions-item>
      <el-descriptions-item label="수정일">{{ formatDate(topic.updatedAt) }}</el-descriptions-item>
    </el-descriptions>

    <div style="margin-top: 20px;">
      <h4>파티션 상세 정보</h4>
      <el-table :data="topic.partitions" style="width: 100%">
        <el-table-column prop="partitionNumber" label="파티션 번호" width="120" />
        <el-table-column prop="leader" label="리더" width="80" />
        <el-table-column prop="replicas" label="복제본" width="120">
          <template #default="{ row }">
            {{ row.replicas.join(', ') }}
          </template>
        </el-table-column>
        <el-table-column prop="inSyncReplicas" label="동기화된 복제본" width="150">
          <template #default="{ row }">
            {{ row.inSyncReplicas.join(', ') }}
          </template>
        </el-table-column>
        <el-table-column prop="earliestOffset" label="최초 오프셋" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.earliestOffset) }}
          </template>
        </el-table-column>
        <el-table-column prop="latestOffset" label="최신 오프셋" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.latestOffset) }}
          </template>
        </el-table-column>
        <el-table-column prop="messageCount" label="메시지 수" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.messageCount) }}
          </template>
        </el-table-column>
        <el-table-column prop="isHealthy" label="상태" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isHealthy ? 'success' : 'danger'">
              {{ row.isHealthy ? '정상' : '오류' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isUnderReplicated" label="복제 상태" width="120">
          <template #default="{ row }">
            <el-tag :type="row.isUnderReplicated ? 'warning' : 'success'">
              {{ row.isUnderReplicated ? '복제 부족' : '정상' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div style="margin-top: 20px;">
      <h4>토픽 설정</h4>
      <el-table :data="topicConfigs" style="width: 100%">
        <el-table-column prop="key" label="설정 키" />
        <el-table-column prop="value" label="설정 값" />
        <el-table-column prop="description" label="설명" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { TopicDetailDto } from '@/types/topic'
import { formatNumber, formatDate } from '@/utils/formatters'

interface Props {
  topic: TopicDetailDto
}

const props = defineProps<Props>()

const topicConfigs = [
  { key: 'retention.ms', value: '604800000', description: '메시지 보관 기간 (밀리초)' },
  { key: 'cleanup.policy', value: 'delete', description: '정리 정책' },
  { key: 'max.message.bytes', value: '1048588', description: '최대 메시지 크기 (바이트)' },
  { key: 'compression.type', value: 'producer', description: '압축 타입' },
  { key: 'min.insync.replicas', value: '1', description: '최소 동기화 복제본 수' }
]
</script>

<style scoped>
.topic-detail-view {
  padding: 20px;
}

h4 {
  margin: 0 0 16px 0;
  color: #303133;
}
</style>