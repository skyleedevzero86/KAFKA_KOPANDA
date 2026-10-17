<template>
  <div class="messages-view">
    <MessageList />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import MessageList from '@/components/message/MessageList.vue'

onMounted(() => {
  console.log('메시지 화면이 마운트되었습니다')
})
</script>

<style scoped>
.messages-view {
  height: 100%;
  padding: 20px;
}
</style>