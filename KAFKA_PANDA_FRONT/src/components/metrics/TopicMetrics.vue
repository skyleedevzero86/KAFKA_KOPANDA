<template>
  <div class="topic-metrics">
    <el-card>
      <template #header>
        <span>토픽 메트릭</span>
      </template>

      <el-table :data="topics" style="width: 100%">
        <el-table-column prop="name" label="토픽명" />
        <el-table-column prop="partitionCount" label="파티션" width="100" />
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
        <el-table-column label="차트" width="200">
          <template #default="{ row }">
            <div class="mini-chart">
              <BarChart
                  :data="{
                  labels: ['파티션'],
                  datasets: [{
                    label: '메시지',
                    data: [row.messageCount],
                    backgroundColor: ['#409EFF']
                  }]
                }"
                  :options="{ responsive: true, maintainAspectRatio: false }"
              />
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { TopicDto } from '@/types/topic'
import { formatNumber } from '@/utils/formatters'
import BarChart from '@/components/charts/BarChart.vue'

interface Props {
  topics: TopicDto[]
}

defineProps<Props>()
</script>

<style scoped>
.topic-metrics {
  margin-bottom: 20px;
}

.mini-chart {
  height: 50px;
}
</style>