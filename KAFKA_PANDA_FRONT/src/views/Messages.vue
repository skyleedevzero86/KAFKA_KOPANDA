<template>
  <div class="messages-view">
    <MessageList />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import MessageList from '@/components/message/MessageList.vue'

onMounted(() => {
  console.log('Messages 컴포넌트 마운트됨')
})
</script>

<style scoped>
.messages-view {
  height: 100%;
  padding: 20px;
}
</style>