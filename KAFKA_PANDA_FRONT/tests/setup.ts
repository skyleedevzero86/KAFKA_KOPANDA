import '@testing-library/jest-dom'

// Mock Chart.js
jest.mock('chart.js', () => ({
  Chart: {
    register: jest.fn(),
    registerables: []
  }
}))

// Mock Element Plus
jest.mock('element-plus', () => ({
  ElButton: 'el-button',
  ElDialog: 'el-dialog',
  ElForm: 'el-form',
  ElFormItem: 'el-form-item',
  ElInput: 'el-input',
  ElSelect: 'el-select',
  ElOption: 'el-option',
  ElTable: 'el-table',
  ElTableColumn: 'el-table-column',
  ElPagination: 'el-pagination',
  ElCard: 'el-card',
  ElTag: 'el-tag',
  ElIcon: 'el-icon',
  ElMenu: 'el-menu',
  ElMenuItem: 'el-menu-item',
  ElSubmenu: 'el-submenu',
  ElDropdown: 'el-dropdown',
  ElDropdownMenu: 'el-dropdown-menu',
  ElDropdownItem: 'el-dropdown-item',
  ElTooltip: 'el-tooltip',
  ElPopconfirm: 'el-popconfirm',
  ElMessage: {
    success: jest.fn(),
    error: jest.fn(),
    warning: jest.fn(),
    info: jest.fn()
  },
  ElMessageBox: {
    confirm: jest.fn(),
    alert: jest.fn()
  }
}))

// Mock Pinia
jest.mock('pinia', () => ({
  createPinia: jest.fn(() => ({})),
  defineStore: jest.fn(() => jest.fn())
}))

// Mock Vue Router
jest.mock('vue-router', () => ({
  createRouter: jest.fn(),
  createWebHistory: jest.fn(),
  useRouter: jest.fn(() => ({
    push: jest.fn(),
    replace: jest.fn(),
    go: jest.fn(),
    back: jest.fn()
  })),
  useRoute: jest.fn(() => ({
    params: {},
    query: {},
    path: '/'
  }))
}))

// Global test utilities
global.ResizeObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn()
}))

global.IntersectionObserver = jest.fn().mockImplementation(() => ({
  observe: jest.fn(),
  unobserve: jest.fn(),
  disconnect: jest.fn()
}))

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: jest.fn().mockImplementation(query => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: jest.fn(),
    removeListener: jest.fn(),
    addEventListener: jest.fn(),
    removeEventListener: jest.fn(),
    dispatchEvent: jest.fn()
  }))
})
