import { mount } from '@vue/test-utils'
import { describe, it, expect } from 'vitest'
import ErrorMessage from '@/components/common/ErrorMessage.vue'

describe('ErrorMessage', () => {
  it('에러 메시지를 렌더링할 수 있다', () => {
    const errorMessage = '오류가 발생했습니다.'
    const wrapper = mount(ErrorMessage, {
      props: {
        message: errorMessage
      }
    })

    expect(wrapper.text()).toContain(errorMessage)
  })

  it('기본 에러 메시지를 표시한다', () => {
    const wrapper = mount(ErrorMessage, {
      props: {
        message: '알 수 없는 오류가 발생했습니다.'
      }
    })

    expect(wrapper.text()).toContain('알 수 없는 오류가 발생했습니다.')
  })

  it('긴 에러 메시지를 처리할 수 있다', () => {
    const longMessage = '매우 긴 에러 메시지입니다. 이것은 테스트를 위한 긴 에러 메시지입니다. 여러 줄에 걸쳐 표시될 수 있습니다.'
    const wrapper = mount(ErrorMessage, {
      props: {
        message: longMessage
      }
    })

    expect(wrapper.text()).toContain(longMessage)
  })

  it('빈 메시지를 처리할 수 있다', () => {
    const wrapper = mount(ErrorMessage, {
      props: {
        message: ''
      }
    })

    expect(wrapper.text()).toBe('')
  })

  it('특수 문자가 포함된 메시지를 처리할 수 있다', () => {
    const specialMessage = '오류 코드: 500, 상세: <script>alert("test")</script>'
    const wrapper = mount(ErrorMessage, {
      props: {
        message: specialMessage
      }
    })

    expect(wrapper.text()).toContain('오류 코드: 500, 상세: <script>alert("test")</script>')
  })

  it('한글이 포함된 메시지를 처리할 수 있다', () => {
    const koreanMessage = '한글 에러 메시지입니다. 오류가 발생했습니다.'
    const wrapper = mount(ErrorMessage, {
      props: {
        message: koreanMessage
      }
    })

    expect(wrapper.text()).toContain(koreanMessage)
  })

  it('숫자가 포함된 메시지를 처리할 수 있다', () => {
    const numericMessage = '오류 코드: 404, 상태: Not Found'
    const wrapper = mount(ErrorMessage, {
      props: {
        message: numericMessage
      }
    })

    expect(wrapper.text()).toContain(numericMessage)
  })

  it('HTML 태그가 포함된 메시지를 처리할 수 있다', () => {
    const htmlMessage = '<strong>중요한</strong> 오류가 발생했습니다.'
    const wrapper = mount(ErrorMessage, {
      props: {
        message: htmlMessage
      }
    })

    expect(wrapper.text()).toContain(htmlMessage)
  })

  it('여러 줄 메시지를 처리할 수 있다', () => {
    const multilineMessage = '첫 번째 줄\n두 번째 줄\n세 번째 줄'
    const wrapper = mount(ErrorMessage, {
      props: {
        message: multilineMessage
      }
    })

    expect(wrapper.text()).toContain(multilineMessage)
  })

  it('props가 변경되면 메시지가 업데이트된다', async () => {
    const wrapper = mount(ErrorMessage, {
      props: {
        message: '첫 번째 메시지'
      }
    })

    expect(wrapper.text()).toContain('첫 번째 메시지')

    await wrapper.setProps({ message: '두 번째 메시지' })
    expect(wrapper.text()).toContain('두 번째 메시지')
  })

  it('컴포넌트가 올바르게 마운트된다', () => {
    const wrapper = mount(ErrorMessage, {
      props: {
        message: '테스트 메시지'
      }
    })

    expect(wrapper.exists()).toBe(true)
    expect(wrapper.isVisible()).toBe(true)
  })

  it('컴포넌트가 올바르게 언마운트된다', () => {
    const wrapper = mount(ErrorMessage, {
      props: {
        message: '테스트 메시지'
      }
    })

    wrapper.unmount()
    expect(wrapper.exists()).toBe(false)
  })
})
