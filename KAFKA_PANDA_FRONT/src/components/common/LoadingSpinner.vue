<template>
  <div class="loading-spinner">
    <el-loading-component />
    <p>{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
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
  color: #909399;
}
</style>