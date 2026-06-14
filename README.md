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

## 배포된 서비스 접속

현재 AWS(서울 리전, EC2 + RDS MySQL)에 배포되어 있으며 아래 주소로 바로 접속할 수 있습니다.

```text
http://43.201.30.249:8080
```

처음 사용하는 경우 회원가입 후 로그인합니다.

- 로그인: http://43.201.30.249:8080/login
- 회원가입: http://43.201.30.249:8080/register

> 운영 인스턴스에 고정(탄력적) IP를 연결하지 않은 경우, 서버를 재시작하면 접속 주소가 변경될 수 있습니다.

## 로컬 실행 방법

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

인증이 필요한 화면은 일반 로그인 또는 회원가입 흐름으로 확인합니다.

```text
http://localhost:8080/login
http://localhost:8080/register
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

## 사용 순서

1. 회원가입 또는 로그인으로 개인 작업 공간에 들어갑니다.
2. 개인 시간표를 등록해 실제로 작업 가능한 시간을 확인합니다.
3. 매일 반복할 목표는 Daily Goal로, 마감이 있는 일은 Deadline Task로 등록합니다.
4. 대시보드에서 오늘의 목표, 마감 업무, 우선순위를 확인합니다.
5. 집중할 작업을 고른 뒤 Focus On/Off로 실제 진행 시간을 기록합니다.
6. 진행률 화면에서 완료율, streak, 주간 변화, 우선순위 작업을 확인합니다.
7. 알림 화면에서 가용 시간 알림, 마감 임박 알림, 집중 재개 알림을 확인합니다.

## 관련 문서

- 배포 방법: `docs/Deployment.md`
- 백엔드 구현 및 API 계약: `Implement.md`
- 설계/분석 문서: `docs/`
