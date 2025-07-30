<template>
  <div class="topics-view">
    <TopicList />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import TopicList from '@/components/topic/TopicList.vue'

onMounted(() => {
  console.log('Topics 컴포넌트 마운트됨')
})
</script>

<style scoped>
.topics-view {
  height: 100%;
  padding: 20px;
}
</style>