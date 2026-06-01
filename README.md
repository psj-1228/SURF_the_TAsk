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
