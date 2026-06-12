# SURF_the_TAsk

<p align="center">
  <img src="images/Logo.png" alt="SURF the TAsk Logo" width="320">
</p>

## 서비스 소개

SURF the TAsk는 사용자의 개인 시간표를 분석하여 실제로 수행 가능한 시간에 맞춰 할 일을 관리하도록 돕는 웹 기반 지능형 To-do 플래너입니다.
Daily Goal과 Deadline Task를 구분해 관리하고, 집중 모드(On/Off), 가용 시간 기반 알림, 마감 알림, streak 및 우선순위 시각화를 통해 사용자가 계획을 꾸준히 실행할 수 있도록 지원합니다.

## 주요 기능

- 개인 시간표 등록 및 가용 시간 분석
- Daily Goal 및 Deadline Task 등록/수정/삭제
- 목표 완료 체크 및 streak 시각화
- 업무 우선순위 계산 및 진행 현황 확인
- Focus On/Off 기반 집중 모드
- 가용 시간, 마감 임박, 마감 초과 알림

## 서비스 화면 예상도

<p align="center">
  <img src="images/Proto%20Type.png" alt="SURF the TAsk Prototype" width="720">
</p>

## 데이터베이스 구조 (ERD)

ERD 이미지는 추후 `images/ERD.png` 경로에 추가할 예정입니다.

<p align="center">
  <em>ERD 이미지 추가 예정</em>
</p>

## 세팅 방법

프로젝트 구현이 정리되면 로컬 실행 환경, 환경 변수, 데이터베이스 초기화, 실행 명령어를 이곳에 작성할 예정입니다.

## 실행 방법

### 1. 로컬 Docker 실행

Docker Desktop 또는 Docker Engine과 Docker Compose 플러그인이 설치되어 있어야 합니다.

```powershell
cd implementation
Copy-Item .env.example .env
docker compose up --build
```

실행 후 브라우저에서 아래 주소로 접속합니다.

```text
http://localhost:8080
```

인증이 필요한 화면을 바로 확인하려면 로컬 개발용 로그인 경로를 사용할 수 있습니다.

```text
http://localhost:8080/dev-login
```

컨테이너를 중지하려면 아래 명령을 실행합니다.

```powershell
docker compose down
```

MySQL 데이터 볼륨까지 삭제하고 새로 시작하려면 아래 명령을 사용합니다.

```powershell
docker compose down -v
```

로컬 Docker 구성은 SMTP 환경 변수를 앱 컨테이너에 전달하지 않으므로 이메일 알림을 발송하지 않습니다.

### 2. AWS EC2 배포

이 프로젝트는 EC2에 Spring Boot 앱 컨테이너를 배포하고, 운영 데이터베이스는 Amazon RDS MySQL을 사용하는 구성을 기준으로 합니다.

EC2 보안 그룹에서 SSH 22번 포트와 앱 접속 포트를 열어둡니다. 기본 앱 포트는 8080입니다.

Ubuntu EC2 기준 Docker 설치 예시는 아래와 같습니다.

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin git
sudo usermod -aG docker $USER
newgrp docker
```

EC2에서 프로젝트를 받은 뒤 `implementation` 디렉터리로 이동합니다.

```bash
git clone <repository-url>
cd SURF_the_TAsk/implementation
```

`.env` 파일을 생성하고 RDS, JWT, SMTP 접속 정보를 입력합니다.

```bash
cat > .env <<'EOF'
SPRING_PROFILES_ACTIVE=prod
APP_PORT=8080

DB_URL=jdbc:mysql://<rds-endpoint>:3306/surf_the_task?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<rds-user>
DB_PASSWORD=<rds-password>
DDL_AUTO=validate

JWT_SECRET=<replace-with-at-least-32-bytes-random-secret>
JWT_EXPIRATION_MINUTES=1440

EMAIL_NOTIFICATIONS_ENABLED=true
EMAIL_FROM=<sender-email>
SMTP_HOST=<smtp-host>
SMTP_PORT=587
SMTP_USERNAME=<smtp-username>
SMTP_PASSWORD=<smtp-app-password>
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
EOF
```

RDS에는 `surf_the_task` 데이터베이스와 테이블이 먼저 준비되어 있어야 합니다. 스키마 SQL은 아래 경로에 있습니다.

```text
implementation/src/main/resources/db/mysql/01-create-database.sql
implementation/src/main/resources/db/mysql/02-create-tables.sql
```

EC2에서 앱을 빌드하고 백그라운드로 실행합니다.

```bash
docker compose -f docker-compose.aws.yml up -d --build
```

로그 확인:

```bash
docker compose -f docker-compose.aws.yml logs -f app
```

브라우저에서 아래 주소로 접속합니다.

```text
http://<EC2_PUBLIC_IP>:8080
```

80번 포트로 접속하고 싶다면 `.env`에서 `APP_PORT=80`으로 바꾼 뒤 다시 실행합니다.

```bash
docker compose -f docker-compose.aws.yml up -d --build
```

새 커밋을 배포할 때는 EC2에서 아래 명령을 실행합니다.

```bash
git pull
docker compose -f docker-compose.aws.yml up -d --build
```

## Commit Message Convention

커밋 메시지는 아래 형식을 따릅니다.

```text
type: message
```

| Type | Description |
| --- | --- |
| feat | 새로운 기능 추가 및 기존 기능 수정 |
| fix | 버그 수정 |
| docs | 문서 및 주석 수정 (README 등) |
| style | 코드 스타일 및 포맷팅 변경 (로직 변화 없음) |
| refactor | 코드 리팩토링 (기능 변화 없음) |
| test | 테스트 코드 추가/수정 |
| chore | 패키지 매니저 수정 및 기타 잡다한 변경(ex: .gitignore) |
