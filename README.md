# Project AntiGravity: 초고가용성 하이브리드 주식 트레이딩 시스템

**AntiGravity**는 유니버스 기반의 하이브리드 아키텍처를 적용한 알고리즘 트레이딩 시스템 프로토타입입니다. Spring Boot와 React를 사용하여 프로덕션 레벨의 안정성을 확보하고, Global Kill Switch 등 강력한 리스크 관리 기능을 탑재했습니다.

## 아키텍처 개요

1.  **Phase 1: 유니버스 스크리닝 (야간 배치)**
    -   매일 밤 거래량과 시가총액 상위 종목을 스캔하여 다음 날 거래할 핵심 타겟 50개를 선정합니다.
2.  **Phase 2: 데이터 아카이빙 (야간 배치)**
    -   선정된 타겟의 과거 1분봉 데이터를 수집하고, 20일/60일 이동평균선(MA)을 미리 계산하여 DB에 저장합니다.
3.  **Phase 3: 실시간 트레이딩 (장중)**
    -   WebSocket(향후 연동 예정)을 통해 실시간 시세를 수신하고, 사전 계산된 지표와 비교하여 매매를 수행합니다.
4.  **안전 장치 (Safety First)**:
    -   **Global Kill Switch**: Redis 플래그를 통해 즉시 시스템의 모든 매매를 중단시킬 수 있습니다.
    -   **Daily Loss Limit**: 당일 손실이 -5%를 초과하면 자동으로 Kill Switch가 발동되어 시스템이 정지됩니다.

## 기술 스택 (Tech Stack)
-   **Backend**: Java 17, Spring Boot 3.2, Spring Data JPA, Redis
-   **Frontend**: React, TypeScript, Vite, Tailwind CSS, Lightweight Charts
-   **Infrastructure**: Docker, Docker Compose, PostgreSQL, Redis

## 시작하기 (Getting Started)

### 필수 요구사항
-   Docker & Docker Compose
-   (로컬 개발 시) Java 17+, Node.js 20+

### 빠른 실행 (권장)
프로젝트 루트에서 다음 명령어 하나로 전체 시스템을 실행할 수 있습니다.
```bash
docker-compose up --build
```
실행 후 **http://localhost:5173**에 접속하여 대시보드를 확인하세요.

### 수동 설치 및 실행

#### Backend
```bash
cd antigravity/backend
./gradlew build
java -jar build/libs/antigravity-backend-0.0.1-SNAPSHOT.jar
```
*참고: 로컬에 Postgres와 Redis가 실행 중이어야 합니다.*

#### Frontend
```bash
cd antigravity/frontend
npm install
npm run dev
```

## 대시보드 주요 기능
-   **Kill Switch 제어**: 시스템 전체의 매매 기능을 ON/OFF 할 수 있습니다.
-   **백테스트 시뮬레이터**: 과거 데이터 기반으로 전략을 테스트하고 수익률을 확인할 수 있습니다. (예: 종목코드 005930, 기간 2023-01-01 ~ 2023-12-31)
-   **인터랙티브 차트**: 선정된 종목의 시세 차트와 보조 지표를 시각적으로 확인합니다.
-   **실시간 상태 모니터링**: 현재 활성 타겟 목록과 당일 손익(P/L)을 실시간으로 보여줍니다.

## API 레퍼런스
-   `POST /api/backtest`: 백테스트 시뮬레이션 실행
-   `GET /api/candles`: 과거 캔들 데이터 조회
-   `POST /api/test/screen`: Phase 1 (스크리닝) 강제 트리거
-   `POST /api/system/kill-switch`: Kill Switch 상태 변경
