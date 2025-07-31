![image](https://github.com/user-attachments/assets/e0e2539b-d74a-4e42-a149-b447b5092178)

# Kafka Kopanda - Kafka 관리 및 모니터링 시스템

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blue.svg)
![Vue.js](https://img.shields.io/badge/Vue.js-3.5.17-green.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-5.5.4-blue.svg)
![Kafka](https://img.shields.io/badge/Kafka-3.9.0-orange.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

## 📋 프로젝트 개요

**Kafka Kopanda**는 Apache Kafka 클러스터를 효율적으로 관리하고 모니터링하기 위한 웹 기반 관리 시스템입니다. Spring Boot 백엔드와 Vue.js 프론트엔드로 구성된 풀스택 애플리케이션으로, 다중 Kafka 클러스터 연결, 실시간 모니터링, 메시지 관리 등의 기능을 제공합니다.

## ✨ 주요 기능

### 🔗 **연결 관리**
- **다중 Kafka 클러스터 연결** - 여러 Kafka 서버에 동시 연결
- **연결 상태 모니터링** - 실시간 연결 상태 및 헬스체크
- **SSL/SASL 인증 지원** - 보안 연결 설정
- **연결 테스트** - 연결 가능성 사전 검증

### 📊 **토픽 관리**
- **토픽 생성/삭제** - 새로운 토픽 생성 및 기존 토픽 삭제
- **토픽 목록 조회** - 클러스터 내 모든 토픽 정보
- **토픽 상세 정보** - 파티션, 복제 팩터, 설정 정보
- **토픽 상태 모니터링** - 토픽 헬스 상태 및 메트릭

### �� **메시지 관리**
- **메시지 전송** - 특정 토픽에 메시지 발행
- **메시지 조회** - 파티션별 메시지 읽기
- **메시지 검색** - 키/값 기반 메시지 검색
- **오프셋 관리** - 파티션별 오프셋 조회 및 설정

### 📈 **모니터링 & 메트릭**
- **실시간 메트릭** - 브로커, 토픽, 파티션 메트릭
- **컨슈머 그룹 모니터링** - 컨슈머 그룹 상태 및 오프셋
- **WebSocket 실시간 업데이트** - 실시간 상태 변경 알림
- **성능 지표** - 처리량, 지연시간, 연결 수

### 🔧 **개발자 도구**
- **RESTful API** - 완전한 REST API 제공
- **H2 콘솔** - 내장 데이터베이스 관리
- **테스트 컨트롤러** - 개발 및 테스트용 엔드포인트
- **로깅 시스템** - 상세한 로그 및 디버깅 정보

## 🏗️ 아키텍처

### **Backend (Spring Boot + Kotlin)**
```
src/main/kotlin/com/sleekydz86/kopanda/
├── application/           # 애플리케이션 레이어
│   ├── dto/              # 데이터 전송 객체
│   ├── ports/            # 포트 인터페이스
│   │   ├── in/           # 인바운드 포트
│   │   └── out/          # 아웃바운드 포트
│   └── services/         # 애플리케이션 서비스
├── domain/               # 도메인 레이어
│   ├── entities/         # 도메인 엔티티
│   ├── events/           # 도메인 이벤트
│   └── valueobjects/     # 값 객체
├── infrastructure/       # 인프라스트럭처 레이어
│   ├── adapters/         # 어댑터
│   │   ├── in/           # 인바운드 어댑터 (컨트롤러)
│   │   └── out/          # 아웃바운드 어댑터 (리포지토리)
│   └── persistence/      # 영속성
└── shared/               # 공유 도메인
    └── domain/           # 공유 도메인 객체
```

### **Frontend (Vue.js + TypeScript)**
```
src/
├── components/           # Vue 컴포넌트
│   ├── common/          # 공통 컴포넌트
│   ├── connection/      # 연결 관리 컴포넌트
│   ├── message/         # 메시지 관리 컴포넌트
│   ├── metrics/         # 메트릭 컴포넌트
│   └── topic/           # 토픽 관리 컴포넌트
├── views/               # 페이지 컴포넌트
├── stores/              # Pinia 상태 관리
├── services/            # API 서비스
├── types/               # TypeScript 타입 정의
├── utils/               # 유틸리티 함수
└── router/              # Vue Router 설정
```

## �� 빠른 시작

### 📋 **사전 요구사항**

- **Java 19+**
- **Kotlin 1.9.25+**
- **Node.js 18+**
- **Kafka 서버** (로컬 또는 원격)
- **Gradle 8.0+**
- **pnpm** (패키지 매니저)

### ⚡ **빠른 설치**

1. **저장소 클론**
```bash
git clone https://github.com/your-username/kafka-kopanda.git
cd kafka-kopanda
```

2. **백엔드 실행**
```bash
cd KAFKA_KOPANDA
./gradlew bootRun
```

3. **프론트엔드 실행**
```bash
cd KAFKA_PANDA_FRONT
pnpm install
pnpm dev
```

4. **애플리케이션 접속**
```
Backend: http://localhost:8080/api
Frontend: http://localhost:5173
H2 Console: http://localhost:8080/api/h2-console
Swagger UI: http://localhost:8080/api/swagger-ui.html
```

### 🔧 **설정**

#### **Backend 설정 (application.yml)**
```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:h2:file:./data/kopanda
    driver-class-name: org.h2.Driver
    username: sa
    password:

kafka:
  client:
    bootstrap-servers: localhost:9092
    default-timeout-ms: 5000
```

#### **Frontend 설정 (.env)**
```env
VITE_API_BASE_URL=http://localhost:8080/api
```

## 📄 API 문서

### **연결 관리 API**

| Method | Endpoint                           | Description    |
| ------ | ---------------------------------- | -------------- |
| `GET`  | `/api/connections`                 | 모든 연결 조회 |
| `POST` | `/api/connections`                 | 새 연결 생성   |
| `POST` | `/api/connections/test`            | 연결 테스트    |
| `GET`  | `/api/connections/{id}/status`     | 연결 상태 확인 |

### **토픽 관리 API**

| Method | Endpoint                              | Description      |
| ------ | ------------------------------------- | ---------------- |
| `GET`  | `/api/connections/{id}/topics`        | 토픽 목록 조회   |
| `POST` | `/api/connections/{id}/topics`        | 토픽 생성        |
| `DELETE` | `/api/connections/{id}/topics/{name}` | 토픽 삭제        |
| `GET`  | `/api/connections/{id}/topics/{name}` | 토픽 상세 정보   |

### **메시지 관리 API**

| Method | Endpoint                                                              | Description |
| ------ | --------------------------------------------------------------------- | ----------- |
| `POST` | `/api/connections/{id}/topics/{name}/messages`                        | 메시지 전송 |
| `GET`  | `/api/connections/{id}/topics/{name}/partitions/{partition}/messages` | 메시지 조회 |
| `POST` | `/api/connections/{id}/topics/{name}/messages/search`                 | 메시지 검색 |

### **모니터링 API**

| Method | Endpoint                                | Description  |
| ------ | --------------------------------------- | ------------ |
| `GET`  | `/api/connections/{id}/metrics`         | Kafka 메트릭 |
| `GET`  | `/api/connections/{id}/consumer-groups` | 컨슈머 그룹  |
| `GET`  | `/api/connections/{id}/topic-metrics`   | 토픽 메트릭  |

## ��️ 기술 스택

### **Backend**
- **Framework**: Spring Boot 3.5.3
- **Language**: Kotlin 1.9.25
- **Database**: H2 (개발), PostgreSQL (운영)
- **Message Queue**: Apache Kafka 3.9.0
- **Security**: Spring Security
- **Real-time**: WebSocket
- **Monitoring**: Micrometer, Prometheus
- **Testing**: JUnit 5, TestContainers
- **Documentation**: Swagger/OpenAPI 3

### **Frontend**
- **Framework**: Vue.js 3.5.17
- **Language**: TypeScript 5.5.4
- **State Management**: Pinia 3.0.3
- **UI Library**: Element Plus 2.4.0
- **Routing**: Vue Router 4.2.5
- **HTTP Client**: Axios 1.6.0
- **Charts**: Chart.js 4.4.0
- **Build Tool**: Vite 7.0.0
- **Package Manager**: pnpm

### **DevOps & Tools**
- **Build Tool**: Gradle 8.0+
- **Package Manager**: pnpm
- **Linting**: ESLint, Prettier
- **Testing**: JUnit 5, TestContainers
- **Documentation**: Swagger UI

## �� 테스트

### **Backend 테스트**
```bash
cd KAFKA_KOPANDA
./gradlew test
```

### **Frontend 테스트**
```bash
cd KAFKA_PANDA_FRONT
pnpm test
```

### **전체 테스트**
```bash
# Backend
./gradlew check

# Frontend
pnpm run type-check
pnpm run lint
```

## �� 배포

### **Docker 배포**
```bash
# Backend Docker 이미지 빌드
cd KAFKA_KOPANDA
docker build -t kafka-kopanda-backend .

# Frontend Docker 이미지 빌드
cd KAFKA_PANDA_FRONT
docker build -t kafka-kopanda-frontend .

# Docker Compose 실행
docker-compose up -d
```

### **개발 환경 실행**
```bash
# Backend
./gradlew bootRun

# Frontend
pnpm dev
```

## �� 모니터링

### **애플리케이션 메트릭**
- **Actuator**: `/api/actuator`
- **Health Check**: `/api/actuator/health`
- **Metrics**: `/api/actuator/metrics`
- **Prometheus**: `/api/actuator/prometheus`

### **데이터베이스 관리**
- **H2 Console**: `http://localhost:8080/api/h2-console`
- **Connection**: `jdbc:h2:file:./data/kopanda`
- **Username**: `sa`
- **Password**: (비어있음)

## �� 개발 가이드

### **코드 스타일**
- **Backend**: Kotlin 코딩 컨벤션 준수
- **Frontend**: ESLint + Prettier 설정
- **TypeScript**: 엄격한 타입 체크

### **아키텍처 패턴**
- **Backend**: Hexagonal Architecture (Clean Architecture)
- **Frontend**: Composition API + Pinia
- **API**: RESTful API 설계

### **에러 처리**
- **Backend**: GlobalExceptionHandler
- **Frontend**: Axios Interceptors
- **로깅**: 구조화된 로깅

## 📸 인터페이스

### 🖥️ **메인 대시보드**

<img width="1146" height="650" alt="image" src="https://github.com/user-attachments/assets/a3da3208-3236-411d-b516-bd8957e4365c" />

### 🔗 **연결 관리**

<img width="1903" height="402" alt="image" src="https://github.com/user-attachments/assets/416089f8-2259-48eb-9aa2-040b377851c0" />


### 📊 **토픽 관리**

<img width="944" height="446" alt="image" src="https://github.com/user-attachments/assets/afae13ee-ce2a-4503-bc22-0fa935f6b262" />

### 📊 **토픽 모니터링**

<img width="1243" height="905" alt="image" src="https://github.com/user-attachments/assets/70a103d2-24ec-4d2f-aaf9-f87e84ac36d3" />
<img width="1722" height="902" alt="image" src="https://github.com/user-attachments/assets/75307170-7418-49be-ab58-987caa7ac635" />
<img width="1244" height="909" alt="image" src="https://github.com/user-attachments/assets/16093bda-73bd-4440-bb34-31bf954840a9" />



### 💬 **메시지 관리**

<img width="1132" height="543" alt="image" src="https://github.com/user-attachments/assets/21d26beb-49ac-423d-bb95-72dfc37b603c" />
<img width="1109" height="646" alt="image" src="https://github.com/user-attachments/assets/dc14552a-6f32-4a4c-a684-4ffa8c391a79" />


### 📈 **메트릭 대시보드**

<img width="1911" height="407" alt="image" src="https://github.com/user-attachments/assets/8cd11546-f935-4513-9692-66ff24a4ca17" />


## 📝 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 📞 지원

- **이슈 리포트**: [GitHub Issues](https://github.com/your-username/kafka-kopanda/issues)
- **문서**: [Wiki](https://github.com/your-username/kafka-kopanda/wiki)
- **이메일**: sleekydz86@naver.com

## 🙏 감사의 말

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Apache Kafka](https://kafka.apache.org/)
- [Element Plus](https://element-plus.org/)
- [Chart.js](https://www.chartjs.org/)

---

**Kafka Kopanda** - Kafka 관리의 새로운 패러다임 🚀



[![GitHub forks](https://img.shields.io/github/forks/skyleedevzero86/kafka-kopanda?style=social)](https://github.com/skyleedevzero86/kafka-kopanda/network/members)
[![GitHub issues](https://img.shields.io/github/issues/skyleedevzero86/kafka-kopanda)](https://github.com/skyleedevzero86/kafka-kopanda/issues)

</div>
