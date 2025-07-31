# KAFKA_KOPANDA

![image](https://github.com/user-attachments/assets/e0e2539b-d74a-4e42-a149-b447b5092178)

**Kafka 관리 및 모니터링을 위한 Spring Boot 기반 웹 애플리케이션**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg)](https://spring.io/projects/spring-boot)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.25-blue.svg)](https://kotlinlang.org/)
[![Kafka](https://img.shields.io/badge/Kafka-3.9.0-orange.svg)](https://kafka.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

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

### 💬 **메시지 관리**

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

## 🚀 빠른 시작 가이드

### 📋 **사전 요구사항**

- **Java 19+**
- **Kotlin 1.9.25+**
- **Kafka 서버** (로컬 또는 원격)
- **Gradle 8.0+**

### ⚡ **빠른 설치**

1. **저장소 클론**

```bash
git clone https://github.com/your-username/kafka-kopanda.git
cd kafka-kopanda
```

2. **애플리케이션 실행**

```bash
./gradlew bootRun
```

3. **애플리케이션 접속**

```
http://localhost:8080/api/test/status
```

### 🔧 **설정**

#### **application.yml 설정**

```yaml
spring:
  application:
    name: kafka-kopanda

  datasource:
    url: jdbc:h2:mem:kopanda
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console
```

#### **Kafka 연결 설정**

```yaml
kafka:
  bootstrap-servers: localhost:9092
  security:
    protocol: PLAINTEXT
```

### 🧪 **API 테스트**

#### **1. 애플리케이션 상태 확인**

```bash
curl -X GET http://localhost:8080/api/test/status
```

#### **2. Kafka 연결 테스트**

```bash
curl -X POST http://localhost:8080/api/test/kafka-connection
```

#### **3. 연결 생성**

```bash
curl -X POST http://localhost:8080/api/test/create-test-connection
```

#### **4. 토픽 생성**

```bash
curl -X POST http://localhost:8080/api/test/create-test-topic/{connectionId}
```

#### **5. 토픽 목록 조회**

```bash
curl -X GET http://localhost:8080/api/connections/{connectionId}/topics
```

## 📸 인터페이스

### 🖥️ **메인 대시보드**

<img width="1146" height="650" alt="image" src="https://github.com/user-attachments/assets/a3da3208-3236-411d-b516-bd8957e4365c" />

### 🔗 **연결 관리**

<img width="1903" height="402" alt="image" src="https://github.com/user-attachments/assets/416089f8-2259-48eb-9aa2-040b377851c0" />


### 📊 **토픽 관리**

<img width="944" height="446" alt="image" src="https://github.com/user-attachments/assets/afae13ee-ce2a-4503-bc22-0fa935f6b262" />

### 📊 **토픽 모니터링**

<img width="1014" height="828" alt="image" src="https://github.com/user-attachments/assets/bec4abce-a8a4-4856-a84c-2232d617a0eb" />
<img width="1722" height="902" alt="image" src="https://github.com/user-attachments/assets/75307170-7418-49be-ab58-987caa7ac635" />


### 💬 **메시지 관리**

![메시지 관리](docs/images/messages.png)

### 📈 **메트릭 대시보드**

<img width="1911" height="407" alt="image" src="https://github.com/user-attachments/assets/8cd11546-f935-4513-9692-66ff24a4ca17" />


## 🏗️ **아키텍처**

### **Hexagonal Architecture (Clean Architecture)**

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

### **기술 스택**

- **Backend**: Spring Boot 3.5.3, Kotlin 1.9.25
- **Database**: H2 (개발), PostgreSQL (운영)
- **Message Queue**: Apache Kafka 3.9.0
- **Security**: Spring Security
- **Real-time**: WebSocket
- **Monitoring**: Micrometer, Prometheus
- **Testing**: JUnit 5, TestContainers

## 📄 **API 문서**

### **연결 관리 API**

| Method | Endpoint                           | Description    |
| ------ | ---------------------------------- | -------------- |
| `GET`  | `/api/connections`                 | 모든 연결 조회 |
| `POST` | `/api/connections`                 | 새 연결 생성   |
| `POST` | `/api/connections/test`            | 연결 테스트    |
| `GET`  | `/api/test/connection-status/{id}` | 연결 상태 확인 |

### **토픽 관리 API**

| Method | Endpoint                              | Description      |
| ------ | ------------------------------------- | ---------------- |
| `GET`  | `/api/connections/{id}/topics`        | 토픽 목록 조회   |
| `POST` | `/api/test/create-test-topic/{id}`    | 테스트 토픽 생성 |
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

## 🧪 **테스트**

### **단위 테스트 실행**

```bash
./gradlew test
```

### **통합 테스트 실행**

```bash
./gradlew integrationTest
```

### **전체 테스트 실행**

```bash
./gradlew check
```

## 🚀 **배포**

### **Docker 배포**

```bash
# Docker 이미지 빌드
docker build -t kafka-kopanda .

# Docker 컨테이너 실행
docker run -p 8080:8080 kafka-kopanda
```

### **Kubernetes 배포**

```bash
# Kubernetes 매니페스트 적용
kubectl apply -f k8s/

# 서비스 확인
kubectl get pods -l app=kafka-kopanda
```

## 📄 **License**

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 [LICENSE](LICENSE) 파일을 참조하세요.

```
MIT License

Copyright (c) 2025 Kafka Kopanda

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## 📧 **Contact**

### **프로젝트 관리자**

- **이름**: lee,KyeongYong
- **이메일**: sleekydz86@naver.com
- **GitHub**: [@skyleedevzero86]([https://github.com/skyleedevzero86](https://velog.io/@sleekydevzero86/posts))

---

<div align="center">

**⭐ 이 프로젝트가 도움이 되었다면 스타를 눌러주세요! ⭐**

[![GitHub stars](https://img.shields.io/github/stars/skyleedevzero86/kafka-kopanda?style=social)](https://github.com/skyleedevzero86/kafka-kopanda/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/skyleedevzero86/kafka-kopanda?style=social)](https://github.com/skyleedevzero86/kafka-kopanda/network/members)
[![GitHub issues](https://img.shields.io/github/issues/skyleedevzero86/kafka-kopanda)](https://github.com/skyleedevzero86/kafka-kopanda/issues)

</div>
