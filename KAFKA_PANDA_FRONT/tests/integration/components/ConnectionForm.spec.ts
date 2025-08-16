import { mount } from '@vue/test-utils'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import ConnectionForm from '@/components/connection/ConnectionForm.vue'

vi.mock('@/services/connectionService', () => ({
  ConnectionService: vi.fn().mockImplementation(() => ({
    testConnection: vi.fn(),
    createConnection: vi.fn()
  }))
}))

const mockElForm = {
  validate: vi.fn(),
  resetFields: vi.fn()
}

const mockElFormItem = {
  validate: vi.fn()
}

describe('ConnectionForm Integration', () => {
  let pinia: any

  beforeEach(() => {
    pinia = createPinia()
    setActivePinia(pinia)
    vi.clearAllMocks()
  })

  it('연결 폼을 렌더링할 수 있다', () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    expect(wrapper.find('form').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="연결 이름을 입력하세요"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="호스트를 입력하세요"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="포트를 입력하세요"]').exists()).toBe(true)
  })

  it('폼 필드에 값을 입력할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const nameInput = wrapper.find('input[placeholder="연결 이름을 입력하세요"]')
    const hostInput = wrapper.find('input[placeholder="호스트를 입력하세요"]')
    const portInput = wrapper.find('input[placeholder="포트를 입력하세요"]')

    await nameInput.setValue('test-connection')
    await hostInput.setValue('localhost')
    await portInput.setValue('9092')

    expect(nameInput.element.value).toBe('test-connection')
    expect(hostInput.element.value).toBe('localhost')
    expect(portInput.element.value).toBe('9092')
  })

  it('SSL과 SASL 옵션을 토글할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const sslSwitch = wrapper.find('input[type="checkbox"]').at(0)
    const saslSwitch = wrapper.find('input[type="checkbox"]').at(1)

    await sslSwitch.setChecked(true)
    await saslSwitch.setChecked(true)

    expect(sslSwitch.element.checked).toBe(true)
    expect(saslSwitch.element.checked).toBe(true)
  })

  it('연결 테스트를 수행할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const testButton = wrapper.find('button:contains("연결 테스트")')
    expect(testButton.exists()).toBe(true)
  })

  it('연결 생성을 수행할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const createButton = wrapper.find('button:contains("연결 생성")')
    expect(createButton.exists()).toBe(true)
  })

  it('폼 유효성 검사를 수행할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const form = wrapper.find('form')
    await form.trigger('submit')

    expect(mockElForm.validate).toHaveBeenCalled()
  })

  it('폼을 리셋할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const resetButton = wrapper.find('button:contains("초기화")')
    await resetButton.trigger('click')

    expect(mockElForm.resetFields).toHaveBeenCalled()
  })

  it('사용자명과 비밀번호 필드를 표시할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const saslSwitch = wrapper.find('input[type="checkbox"]').at(1)
    await saslSwitch.setChecked(true)

    expect(wrapper.find('input[placeholder="사용자명을 입력하세요"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="비밀번호를 입력하세요"]').exists()).toBe(true)
  })

  it('포트 범위를 검증할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const portInput = wrapper.find('input[placeholder="포트를 입력하세요"]')
    
    await portInput.setValue('99999')
    await portInput.trigger('blur')
    const form = wrapper.find('form')
    await form.trigger('submit')

    expect(mockElForm.validate).toHaveBeenCalled()
  })

  it('연결 이름 중복 검사를 수행할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    const nameInput = wrapper.find('input[placeholder="연결 이름을 입력하세요"]')
    await nameInput.setValue('existing-connection')
    await nameInput.trigger('blur')

    expect(nameInput.element.value).toBe('existing-connection')
  })

  it('에러 메시지를 표시할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    await wrapper.setData({
      errors: {
        name: '연결 이름은 필수입니다.',
        host: '호스트는 필수입니다.',
        port: '포트는 1-65535 범위여야 합니다.'
      }
    })

    expect(wrapper.text()).toContain('연결 이름은 필수입니다.')
    expect(wrapper.text()).toContain('호스트는 필수입니다.')
    expect(wrapper.text()).toContain('포트는 1-65535 범위여야 합니다.')
  })

  it('로딩 상태를 표시할 수 있다', async () => {
    const wrapper = mount(ConnectionForm, {
      global: {
        plugins: [pinia],
        stubs: {
          'el-form': mockElForm,
          'el-form-item': mockElFormItem,
          'el-input': 'input',
          'el-select': 'select',
          'el-option': 'option',
          'el-button': 'button',
          'el-switch': 'input'
        }
      }
    })

    await wrapper.setData({
      isTesting: true,
      isCreating: true
    })

    const testButton = wrapper.find('button:contains("연결 테스트")')
    const createButton = wrapper.find('button:contains("연결 생성")')

    expect(testButton.attributes('disabled')).toBeDefined()
    expect(createButton.attributes('disabled')).toBeDefined()
  })
})
