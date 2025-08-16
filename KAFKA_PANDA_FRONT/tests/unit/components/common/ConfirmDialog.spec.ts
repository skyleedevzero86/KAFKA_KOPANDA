import { mount } from '@vue/test-utils'
import { describe, it, expect, beforeEach, vi } from 'vitest'
import ConfirmDialog from '@/components/common/ConfirmDialog.vue'

describe('ConfirmDialog', () => {
  const defaultProps = {
    modelValue: true,
    title: '확인',
    message: '정말로 진행하시겠습니까?',
    confirmText: '확인',
    cancelText: '취소',
    loading: false
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('확인 다이얼로그를 렌더링할 수 있다', () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    expect(wrapper.find('.el-dialog').exists()).toBe(true)
    expect(wrapper.find('.dialog-footer').exists()).toBe(true)
  })

  it('기본 props를 올바르게 표시한다', () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    expect(wrapper.text()).toContain('확인')
    expect(wrapper.text()).toContain('정말로 진행하시겠습니까?')
    expect(wrapper.text()).toContain('확인')
    expect(wrapper.text()).toContain('취소')
  })

  it('커스텀 props를 올바르게 표시한다', () => {
    const customProps = {
      modelValue: true,
      title: '삭제 확인',
      message: '이 항목을 삭제하시겠습니까?',
      confirmText: '삭제',
      cancelText: '아니오',
      loading: false
    }

    const wrapper = mount(ConfirmDialog, {
      props: customProps
    })

    expect(wrapper.text()).toContain('삭제 확인')
    expect(wrapper.text()).toContain('이 항목을 삭제하시겠습니까?')
    expect(wrapper.text()).toContain('삭제')
    expect(wrapper.text()).toContain('아니오')
  })

  it('modelValue가 false일 때 다이얼로그가 숨겨진다', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        modelValue: false
      }
    })

    expect(wrapper.vm.visible).toBe(false)
  })

  it('modelValue가 true일 때 다이얼로그가 표시된다', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        modelValue: true
      }
    })

    expect(wrapper.vm.visible).toBe(true)
  })

  it('확인 버튼을 클릭하면 confirm 이벤트를 발생시킨다', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    const confirmButton = wrapper.find('button[type="primary"]')
    await confirmButton.trigger('click')

    expect(wrapper.emitted('confirm')).toBeTruthy()
  })

  it('취소 버튼을 클릭하면 cancel 이벤트를 발생시키고 다이얼로그를 닫는다', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    const cancelButton = wrapper.find('button:not([type="primary"])')
    await cancelButton.trigger('click')

    expect(wrapper.emitted('cancel')).toBeTruthy()
    expect(wrapper.vm.visible).toBe(false)
  })

  it('다이얼로그를 닫으면 update:modelValue 이벤트를 발생시킨다', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    wrapper.vm.visible = false
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([false])
  })

  it('로딩 상태일 때 확인 버튼에 로딩 표시가 나타난다', () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        loading: true
      }
    })

    const confirmButton = wrapper.find('button[type="primary"]')
    expect(confirmButton.attributes('loading')).toBe('true')
  })

  it('다이얼로그 너비를 올바르게 설정한다', () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    const dialog = wrapper.find('.el-dialog')
    expect(dialog.attributes('width')).toBe('400px')
  })

  it('beforeClose 핸들러가 올바르게 동작한다', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    await wrapper.vm.handleClose()

    expect(wrapper.vm.visible).toBe(false)
  })

  it('props 변경 시 visible 상태가 올바르게 업데이트된다', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: {
        ...defaultProps,
        modelValue: false
      }
    })

    expect(wrapper.vm.visible).toBe(false)

    await wrapper.setProps({ modelValue: true })
    expect(wrapper.vm.visible).toBe(true)

    await wrapper.setProps({ modelValue: false })
    expect(wrapper.vm.visible).toBe(false)
  })

  it('visible 상태 변경 시 update:modelValue 이벤트를 발생시킨다', async () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    wrapper.vm.visible = false
    await wrapper.vm.$nextTick()

    expect(wrapper.emitted('update:modelValue')).toBeTruthy()
    expect(wrapper.emitted('update:modelValue')[0]).toEqual([false])
  })

  it('다이얼로그 푸터 스타일이 올바르게 적용된다', () => {
    const wrapper = mount(ConfirmDialog, {
      props: defaultProps
    })

    const footer = wrapper.find('.dialog-footer')
    expect(footer.exists()).toBe(true)
  })

  it('빈 제목과 메시지를 처리할 수 있다', () => {
    const emptyProps = {
      modelValue: true,
      title: '',
      message: '',
      confirmText: '확인',
      cancelText: '취소',
      loading: false
    }

    const wrapper = mount(ConfirmDialog, {
      props: emptyProps
    })

    expect(wrapper.find('.el-dialog').exists()).toBe(true)
  })

  it('긴 제목과 메시지를 처리할 수 있다', () => {
    const longProps = {
      modelValue: true,
      title: '매우 긴 제목입니다. 이것은 테스트를 위한 긴 제목입니다.',
      message: '매우 긴 메시지입니다. 이것은 테스트를 위한 긴 메시지입니다. 여러 줄에 걸쳐 표시될 수 있습니다.',
      confirmText: '확인',
      cancelText: '취소',
      loading: false
    }

    const wrapper = mount(ConfirmDialog, {
      props: longProps
    })

    expect(wrapper.text()).toContain('매우 긴 제목입니다')
    expect(wrapper.text()).toContain('매우 긴 메시지입니다')
  })
})
