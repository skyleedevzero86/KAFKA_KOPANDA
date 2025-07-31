/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

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

declare module 'chart.js' {
  export * from 'chart.js/auto'
}