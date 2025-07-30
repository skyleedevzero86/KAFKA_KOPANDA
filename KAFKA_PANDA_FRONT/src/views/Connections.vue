<template>
  <div class="connections-view">
    <ConnectionList />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onActivated } from 'vue'
import ConnectionList from '@/components/connection/ConnectionList.vue'

onMounted(() => {
  console.log('Connections 컴포넌트 마운트됨')
})

onActivated(() => {
  console.log('Connections 컴포넌트 활성화됨')
})
</script>

<style scoped>
.connections-view {
  height: 100%;
  padding: 20px;
}
</style>