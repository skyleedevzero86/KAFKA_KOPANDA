<template>
  <div class="topics-view">
    <TopicList />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import TopicList from '@/components/topic/TopicList.vue'

onMounted(() => {
  console.log('토픽 화면이 마운트되었습니다')
})
</script>

<style scoped>
.topics-view {
  height: 100%;
  padding: 20px;
}
</style>