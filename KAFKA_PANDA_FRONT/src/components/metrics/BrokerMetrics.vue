<template>
  <div class="broker-metrics">
    <el-card>
      <template #header>
        <span>브로커 메트릭</span>
      </template>

      <el-row :gutter="20">
        <el-col :span="8">
          <GaugeChart
              :value="metrics?.brokerCount || 0"
              :max="10"
              label="브로커"
              color="#409EFF"
          />
        </el-col>

        <el-col :span="8">
          <GaugeChart
              :value="metrics?.topicCount || 0"
              :max="100"
              label="토픽"
              color="#67C23A"
          />
        </el-col>

        <el-col :span="8">
          <GaugeChart
              :value="metrics?.totalPartitions || 0"
              :max="1000"
              label="파티션"
              color="#E6A23C"
          />
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import type { KafkaMetricsDto } from '@/types/metrics'
import GaugeChart from '@/components/charts/GaugeChart.vue'

interface Props {
  metrics: KafkaMetricsDto | null
}

defineProps<Props>()
</script>

<style scoped>
.broker-metrics {
  margin-bottom: 20px;
}
</style>