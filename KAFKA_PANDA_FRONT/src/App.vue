<script setup lang="ts">
import { ref } from 'vue';

const message = ref<string>('안녕하세요, Vue 3 + TypeScript!');
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-gray-100 p-4">
    <h1 class="text-4xl font-bold text-blue-600 mb-6">{{ message }}</h1>
    <p class="text-lg text-gray-700 text-center">
      이것은 TypeScript로 작성된 Vue 3 프로젝트의 예시입니다.
    </p>
    <p class="text-lg text-gray-700 text-center mt-2">
      `package.json`에 필요한 의존성이 추가되었으며,
      `tsconfig.json` 파일들이 설정되었습니다.
    </p>
    <button
      @click="message = '메시지가 변경되었습니다!'"
      class="mt-8 px-6 py-3 bg-blue-500 text-white rounded-lg shadow-md hover:bg-blue-600 transition duration-300 ease-in-out"
    >
      메시지 변경
    </button>
  </div>
</template>
<style scoped></style>