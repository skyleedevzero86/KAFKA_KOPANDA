<template>
  <div class="loading-spinner">
    <el-icon class="is-loading">
      <Loading />
    </el-icon>
    <p>{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
import { Loading } from '@element-plus/icons-vue'

interface Props {
  message?: string
}

withDefaults(defineProps<Props>(), {
  message: '로딩 중...'
})
</script>

<style scoped>
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
}

.loading-spinner p {
  margin-top: 16px;
  color: #606266;
}
</style>