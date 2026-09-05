# 이커머스 MSA 프로젝트 (ecommerce-msa)

스파르타 MSA 과정 개별 프로젝트. Spring Cloud Gateway + 3개의 도메인 서비스(회원/상품/주문)로 구성된
마이크로서비스 아키텍처 기반 이커머스 백엔드입니다.

## 아키텍처

```
                        ┌──────────────────┐
                        │  gateway-service  │  :8080
                        │  (Spring Cloud    │
                        │   Gateway + JWT)  │
                        └─────────┬─────────┘
                                  │ X-User-Id 헤더 주입
            ┌─────────────────────┼─────────────────────┐
            ▼                     ▼                     ▼
   ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐
   │  user-service   │  │ product-service   │  │  order-service    │
   │     :8081       │  │      :8082        │  │      :8083        │
   └────────┬────────┘  └─────────┬─────────┘  └─────────┬─────────┘
            │                     │                       │ (FeignClient)
            ▼                     ▼                       ▼
     PostgreSQL(user_db)   PostgreSQL(product_db)  PostgreSQL(order_db)
            │                                              │
            ▼                                              ▼
          Redis                                          Redis
     (JWT 리프레시 등)                            (Redisson 분산 락)
```

- **gateway-service**: 모든 요청의 진입점. `AuthFilter`(GatewayFilterFactory)가 JWT를 검증하고,
  파싱한 사용자 ID를 `X-User-Id` 헤더로 변환해 하위 서비스에 전달합니다. 회원가입/로그인/상품 조회 등은
  인증 예외 경로(`excludePaths`)로 처리됩니다.
- **user-service**: 회원가입, 로그인(JWT 발급), 내 정보 조회/수정/탈퇴 담당.
- **product-service**: 상품 등록/조회/수정/삭제, 카테고리/키워드 검색, 재고 조회 및 차감 API 제공.
- **order-service**: 장바구니, 주문, 쿠폰, 결제(토스페이먼츠 연동)를 담당하며 `ProductClient`(OpenFeign)로
  product-service의 재고 API를 호출합니다.

## 주요 기능

### 회원 (user-service)
- 회원가입 / 로그인 (JWT Access/Refresh 발급)
- 내 정보 조회 / 수정 / 탈퇴 (`X-User-Id` 헤더 기반)

### 상품 (product-service)
- 상품 CRUD, 카테고리별 조회, 키워드 검색
- 재고 조회(`GET /{id}/stock`) 및 차감(`PUT /{id}/stock`) API

### 주문 (order-service)
- 장바구니 담기/조회/수량 변경/삭제
- 장바구니 → 주문 생성 (재고 사전 검증 후 차감, 쿠폰 할인 적용)
- 주문 목록/상세 조회, 주문 취소, 주문 상태 변경(관리자)
- 쿠폰 발급 / 내 쿠폰 조회 / 쿠폰 적용(할인 금액 계산)
- 결제 승인(토스페이먼츠 API 연동) / 환불

### 동시성 제어
- 주문 생성 시 여러 사용자가 동시에 같은 상품을 주문해 재고가 마이너스로 빠지는 문제를 막기 위해
  `StockLockService`에서 **Redisson 분산 락**(`tryLock(3s, 5s)`)으로 상품별 재고 차감 구간을 직렬화합니다.
- 락 획득 실패 시 "잠시 후 다시 시도해주세요" 예외를 반환하고, 재고 차감 전 별도로 재고 수량을 검증해
  품절 상품 주문을 사전에 막습니다.

## 기술 스택

- Java 17, Spring Boot 3.5
- Spring Cloud Gateway, Spring Cloud OpenFeign
- Spring Data JPA, PostgreSQL 16
- Spring Security, JJWT (JWT 발급/검증)
- Redis, Redisson (분산 락)
- Toss Payments API 연동
- Docker / Docker Compose

## 실행 방법

```bash
# 인프라 + 전체 서비스 빌드/기동
docker-compose up --build

# 서비스 포트
# gateway-service : 8080
# user-service    : 8081
# product-service : 8082
# order-service   : 8083
# postgres        : 5433
# redis           : 6379
```

모든 API 요청은 gateway-service(`:8080`)를 통해 호출합니다. 로그인 응답으로 받은 JWT를
`Authorization: Bearer {token}` 헤더에 담아 요청하면, 게이트웨이가 이를 검증한 뒤 `X-User-Id` 헤더를
붙여 각 서비스로 라우팅합니다.

---

## 📅 제출 방법

### 1️⃣ Branch 생성 및 작업

제공된 주차별 Git repository에서 **신규 Branch**를 생성한 뒤 작업합니다.

> **Branch 이름 형식**
> ```
> work/{팀번호}-{영문 이름}
> ```
> 예: `work/1-john-doe`


### 2️⃣ Commit 및 Push

작업 내용을 **작업용 브랜치**에 Commit하고 Push합니다.


### 3️⃣ PR 요청

작업이 완료되면 **작업용 브랜치**에서 **제출용 브랜치**로 PR(Pull Request)을 생성합니다.

> **제출용 브랜치 이름 형식**
> ```
> project/{팀번호}-{영문 이름}
> ```
> 예: `project/1-john-doe`


### 4️⃣ PR 리뷰 및 병합

리뷰가 완료되면 **제출용 브랜치**에 PR을 병합합니다.


### 🔄 전체 흐름 요약

```
신규 Branch 생성 (work/...)
        ↓
     작업 진행
        ↓
  Commit & Push
        ↓
PR 생성 (work/... → project/...)
        ↓
    리뷰 완료
        ↓
  PR 병합 ✅
```
