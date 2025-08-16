# Kafka Panda Frontend 테스트 가이드

이 문서는 Kafka Panda Frontend 프로젝트의 테스트 구조와 실행 방법을 설명합니다.

## 테스트 구조

프로젝트는 다음과 같은 테스트 계층을 가지고 있습니다:

### 1. 단위테스트 (Unit Tests)

- **위치**: `tests/unit/`
- **목적**: 개별 컴포넌트와 함수의 동작을 격리된 환경에서 테스트
- **범위**: Vue 컴포넌트, 서비스, 유틸리티 함수 등

#### 주요 단위테스트

- `components/charts/` - 차트 컴포넌트 테스트
- `components/common/` - 공통 컴포넌트 테스트
- `services/` - API 서비스 테스트
- `utils/` - 유틸리티 함수 테스트

### 2. 통합테스트 (Integration Tests)

- **위치**: `tests/integration/`
- **목적**: 여러 컴포넌트 간의 상호작용을 테스트
- **범위**: 컴포넌트 간 통신, 상태 관리, 라우팅 등

#### 주요 통합테스트

- `components/` - 컴포넌트 통합 테스트
- `stores/` - Pinia 스토어 통합 테스트
- `router/` - 라우팅 통합 테스트

### 3. 인수테스트 (Acceptance Tests)

- **위치**: `tests/acceptance/`
- **목적**: 사용자 시나리오 기반의 전체 기능 테스트
- **범위**: 사용자 워크플로우, 비즈니스 로직 등

#### 주요 인수테스트

- `ConnectionManagement.spec.ts` - 연결 관리 사용자 시나리오
- `TopicManagement.spec.ts` - 토픽 관리 사용자 시나리오
- `MessageManagement.spec.ts` - 메시지 관리 사용자 시나리오

## 테스트 실행 방법

### 1. 모든 테스트 실행

```bash
npm run test
```

### 2. 테스트 감시 모드

```bash
npm run test:watch
```

### 3. 커버리지 리포트 생성

```bash
npm run test:coverage
```

### 4. 단위테스트만 실행

```bash
npm run test:unit
```

### 5. 통합테스트만 실행

```bash
npm run test:integration
```

### 6. 인수테스트만 실행

```bash
npm run test:acceptance
```

### 7. 특정 테스트 파일 실행

```bash
npm test -- tests/unit/components/charts/BarChart.spec.ts
```

### 8. 특정 테스트 패턴 실행

```bash
npm test -- --grep "차트를 렌더링할 수 있다"
```

## 테스트 환경 설정

### 1. 의존성 설치

```bash
npm install
```

### 2. Jest 설정

- `jest.config.js` - Jest 기본 설정
- `tests/setup.ts` - 테스트 환경 설정

### 3. Mock 설정

- **Chart.js**: 차트 라이브러리 모킹
- **Element Plus**: UI 컴포넌트 모킹
- **Pinia**: 상태 관리 모킹
- **Vue Router**: 라우팅 모킹

## 테스트 작성 가이드

### 1. 단위테스트 작성 원칙

```typescript
describe("ComponentName", () => {
  it("컴포넌트가 올바르게 렌더링된다", () => {
    // Given - 테스트 준비
    const wrapper = mount(ComponentName);

    // When - 테스트 실행
    // (필요한 경우)

    // Then - 결과 검증
    expect(wrapper.exists()).toBe(true);
  });
});
```

### 2. 테스트 네이밍 컨벤션

- **describe 블록**: `컴포넌트명` 또는 `기능 설명`
- **it 블록**: `한글로 동작을 설명`
- **변수명**: 영어 사용

### 3. 테스트 데이터 생성

```typescript
const mockData = {
  labels: ["Jan", "Feb", "Mar"],
  datasets: [
    {
      label: "Sales",
      data: [10, 20, 30],
    },
  ],
};
```

## 주요 테스트 라이브러리

### 1. Jest

- 테스트 프레임워크
- `describe`, `it`, `expect` 등

### 2. Vue Test Utils

- Vue 컴포넌트 테스트 지원
- `mount`, `shallowMount` 등

### 3. Testing Library

- 사용자 중심 테스트
- `findByText`, `click` 등

### 4. Vitest

- 빠른 테스트 실행
- Vite 기반 테스트 러너

## 컴포넌트별 테스트 가이드

### 1. 차트 컴포넌트 테스트

```typescript
// Chart.js 모킹
vi.mock("chart.js", () => ({
  Chart: vi.fn(() => mockChart),
  registerables: [],
}));

// 차트 생성 및 업데이트 테스트
it("데이터가 변경되면 차트를 업데이트한다", async () => {
  // 테스트 로직
});
```

### 2. 폼 컴포넌트 테스트

```typescript
// 폼 입력 테스트
it("폼 필드에 값을 입력할 수 있다", async () => {
  const input = wrapper.find('input[placeholder="..."]');
  await input.setValue("test-value");
  expect(input.element.value).toBe("test-value");
});
```

### 3. 서비스 테스트

```typescript
// API 호출 모킹
vi.mock("axios", () => ({
  default: {
    create: vi.fn(() => mockAxios),
  },
}));

// 서비스 메서드 테스트
it("GET 요청을 성공적으로 처리할 수 있다", async () => {
  // 테스트 로직
});
```

## 테스트 커버리지

### 1. 커버리지 리포트 생성

```bash
npm run test:coverage
```

### 2. 커버리지 리포트 위치

- `coverage/lcov-report/index.html`
- `coverage/coverage-summary.json`

### 3. 커버리지 기준

- **Statements**: 80% 이상
- **Branches**: 70% 이상
- **Functions**: 80% 이상
- **Lines**: 80% 이상

## 테스트 디버깅

### 1. 로그 확인

- 테스트 실행 시 상세 로그 출력
- 실패한 테스트의 상세 정보 확인

### 2. 테스트 실패 시

- 컴포넌트 마운트 상태 확인
- Mock 설정 확인
- 비동기 작업 처리 확인

## 주요 테스트 시나리오

### 1. 연결 관리

- 연결 생성/수정/삭제
- 연결 테스트
- 연결 상태 모니터링

### 2. 토픽 관리

- 토픽 생성/수정/삭제
- 파티션 관리
- 토픽 설정 관리

### 3. 메시지 관리

- 메시지 조회/전송
- 메시지 검색
- 오프셋 관리

### 4. 메트릭 모니터링

- 브로커 메트릭
- 토픽 메트릭
- 컨슈머 그룹 메트릭

## 테스트 실행 시 주의사항

### 1. 환경 요구사항

- Node.js 18 이상
- npm 9 이상
- Vue 3.x

### 2. 테스트 격리

- 각 테스트는 독립적으로 실행
- `beforeEach`에서 테스트 데이터 정리
- Mock 상태 초기화

### 3. 비동기 처리

- `async/await` 사용
- `wrapper.vm.$nextTick()` 사용
- Promise 처리 확인

## 문제 해결

### 1. 테스트 실패 시 체크리스트

- [ ] 의존성 설치 확인
- [ ] Mock 설정 확인
- [ ] 컴포넌트 마운트 확인
- [ ] 비동기 작업 처리 확인

### 2. 일반적인 문제들

- **컴포넌트 마운트 실패**: 전역 설정 확인
- **Mock 동작 안함**: vi.mock 설정 확인
- **비동기 테스트 실패**: await 사용 확인

## 추가 리소스

- [Vue Test Utils 공식 문서](https://test-utils.vuejs.org/)
- [Jest 공식 문서](https://jestjs.io/)
- [Vitest 공식 문서](https://vitest.dev/)
- [Testing Library 문서](https://testing-library.com/)
