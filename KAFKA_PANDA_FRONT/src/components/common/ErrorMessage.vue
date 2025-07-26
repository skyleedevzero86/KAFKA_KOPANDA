<template>
  <div v-if="error" class="error-message">
    <el-alert
      :title="title"
      :description="error"
      type="error"
      show-icon
      :closable="closable"
      @close="$emit('close')"
    />
  </div>
</template>

<script setup lang="ts">
interface Props {
  error: string | null
  title?: string
  closable?: boolean
}

withDefaults(defineProps<Props>(), {
  title: '오류',
  closable: true
})

defineEmits<{
  close: []
}>()
</script>

<style scoped>
.error-message {
  margin-bottom: 16px;
}
</style>