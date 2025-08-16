/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

declare module 'chart.js/auto' {
  export * from 'chart.js'
}

declare module '@/stores/*' {
  const store: any
  export default store
}

declare module '@/types/*' {
  const types: any
  export default types
}

declare module '@/utils/*' {
  const utils: any
  export default utils
}

declare module '@/components/*' {
  const component: any
  export default component
}

declare module '@/services/*' {
  const service: any
  export default service
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
 