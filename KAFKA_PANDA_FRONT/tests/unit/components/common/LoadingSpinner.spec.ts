import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'

describe('LoadingSpinner', () => {
  it('로딩 스피너를 렌더링할 수 있다', () => {
    const wrapper = mount(LoadingSpinner)
    
    expect(wrapper.find('.loading-spinner').exists()).toBe(true)
  })

  it('로딩 텍스트를 표시한다', () => {
    const wrapper = mount(LoadingSpinner)
    
    expect(wrapper.text()).toContain('로딩 중...')
  })

  it('기본 로딩 메시지를 표시한다', () => {
    const wrapper = mount(LoadingSpinner)
    
    expect(wrapper.text()).toContain('로딩 중...')
  })

  it('커스텀 로딩 메시지를 표시한다', () => {
    const customMessage = '데이터를 불러오는 중입니다...'
    const wrapper = mount(LoadingSpinner, {
      props: {
        message: customMessage
      }
    })
    
    expect(wrapper.text()).toContain(customMessage)
  })

  it('빈 메시지를 처리할 수 있다', () => {
    const wrapper = mount(LoadingSpinner, {
      props: {
        message: ''
      }
    })
    
    expect(wrapper.text()).toBe('')
  })

  it('긴 로딩 메시지를 처리할 수 있다', () => {
    const longMessage = '매우 긴 로딩 메시지입니다. 이것은 테스트를 위한 긴 로딩 메시지입니다.'
    const wrapper = mount(LoadingSpinner, {
      props: {
        message: longMessage
      }
    })
    
    expect(wrapper.text()).toContain(longMessage)
  })

  it('한글이 포함된 메시지를 처리할 수 있다', () => {
    const koreanMessage = '한글 로딩 메시지입니다. 로딩 중입니다.'
    const wrapper = mount(LoadingSpinner, {
      props: {
        message: koreanMessage
      }
    })
    
    expect(wrapper.text()).toContain(koreanMessage)
  })

  it('숫자가 포함된 메시지를 처리할 수 있다', () => {
    const numericMessage = '로딩 중... (1/5)'
    const wrapper = mount(LoadingSpinner, {
      props: {
        message: numericMessage
      }
    })
    
    expect(wrapper.text()).toContain(numericMessage)
  })

  it('특수 문자가 포함된 메시지를 처리할 수 있다', () => {
    const specialMessage = '로딩 중... <progress>50%</progress>'
    const wrapper = mount(LoadingSpinner, {
      props: {
        message: specialMessage
      }
    })
    
    expect(wrapper.text()).toContain(specialMessage)
  })

  it('props가 변경되면 메시지가 업데이트된다', async () => {
    const wrapper = mount(LoadingSpinner, {
      props: {
        message: '첫 번째 메시지'
      }
    })
    
    expect(wrapper.text()).toContain('첫 번째 메시지')
    
    await wrapper.setProps({ message: '두 번째 메시지' })
    expect(wrapper.text()).toContain('두 번째 메시지')
  })

  it('컴포넌트가 올바르게 마운트된다', () => {
    const wrapper = mount(LoadingSpinner)
    
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.isVisible()).toBe(true)
  })

  it('컴포넌트가 올바르게 언마운트된다', () => {
    const wrapper = mount(LoadingSpinner)
    
    wrapper.unmount()
    expect(wrapper.exists()).toBe(false)
  })

  it('로딩 스피너 클래스가 올바르게 적용된다', () => {
    const wrapper = mount(LoadingSpinner)
    
    const spinner = wrapper.find('.loading-spinner')
    expect(spinner.exists()).toBe(true)
  })

  it('기본 props 없이도 렌더링된다', () => {
    const wrapper = mount(LoadingSpinner)
    
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.text()).toContain('로딩 중...')
  })

  it('여러 인스턴스를 동시에 렌더링할 수 있다', () => {
    const wrapper1 = mount(LoadingSpinner, {
      props: { message: '첫 번째' }
    })
    const wrapper2 = mount(LoadingSpinner, {
      props: { message: '두 번째' }
    })
    
    expect(wrapper1.text()).toContain('첫 번째')
    expect(wrapper2.text()).toContain('두 번째')
    
    wrapper1.unmount()
    wrapper2.unmount()
  })
})
