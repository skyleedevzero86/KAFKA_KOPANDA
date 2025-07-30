/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

// Node.js 타입 정의 추가
declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: 'development' | 'production' | 'test'
    VITE_API_BASE_URL: string
  }
  
  interface Timeout {
    ref(): Timeout
    unref(): Timeout
  }
}

// Chart.js 타입 정의
declare module 'chart.js' {
  export * from 'chart.js/auto'
}