# 1. Conceptualization

## Project Title: SURF the TAsk
**SURF the TAsk: Availability-aware Intelligent To-do Planner**

- **Student No**: 22212069
- **Name**: 박성준
- **E-mail**: seongjun1228@gmail.com
![로고](https://github.com/psj-1228/SURF_the_TAsk/blob/main/images/Logo.png)

---

## Revision history

| Revision date | Version # | Description | Author |
|---|---:|---|---|
| 03/26/2026 | 1.00 | Initial Concept | 박성준 |
| | | | |
| | | | |

---

## = Contents =
1. [Business purpose](#2-business-purpose)
2. [System context diagram](#3-system-context-diagram)
3. [Use case list](#4-use-case-list)
4. [Concept of operation](#5-concept-of-operation)
5. [Problem statement](#6-problem-statement)
6. [Glossary](#7-glossary)
7. [References](#8-references)

---

## 2. Business purpose

### Project background
많은 사용자가 To-do 리스트 앱을 사용하지만, 단순히 할 일을 기록하는 수준에 머무는 경우가 많다. 특히 대학생이나 자기주도 학습자는 수업, 과제, 시험 준비, 개인 일정이 동시에 존재하므로 실제로 **언제 수행 가능한지**를 고려한 계획 관리가 필요하다. 그러나 기존의 단순 To-do 앱은 사용자의 시간표나 가용 시간을 충분히 반영하지 못해, 바쁜 시간에도 알림이 오거나 정작 수행 가능한 시간에는 적절한 유도가 이루어지지 않는 문제가 있다.

또한 사용자는 할 일을 시작하더라도 작업 중 다른 앱이나 활동으로 쉽게 이탈할 수 있다. 단순한 체크리스트만으로는 집중 유지가 어렵고, 반복 수행에 대한 성취감도 부족하다. 따라서 본 프로젝트는 할 일 관리뿐 아니라, **집중 유지**, **가용 시간 기반 알림**, **연속 수행 성과 시각화**를 결합한 지능형 플래너를 목표로 한다.

### Motivation
본 프로젝트의 동기는 다음과 같다.

1. 사용자의 실제 생활 패턴을 반영한 더 현실적인 계획 관리 기능이 필요하다.
2. 할 일을 단순 기록하는 것이 아니라, 완료까지 이어지도록 집중을 유도하는 기능이 필요하다.
3. 매일 수행해야 할 목표의 연속 달성 현황을 시각적으로 보여주면 동기 부여 효과가 높아질 수 있다.
4. 마감 기한이 있는 업무에 대해 적절한 시점에 알림을 제공하면 미루기를 줄일 수 있다.

### Goal
본 프로젝트의 목표는 **사용자의 개인 시간표를 분석하여 가용 시간에만 지능적으로 알림을 보내고, 업무의 우선순위와 연속 수행 성과를 시각화하며, 집중 모드(On/Off)를 통해 몰입을 돕는 웹 기반 지능형 플래너를 개발하는 것**이다.

### Differentiation
기존 To-do 리스트는 일정 기록과 완료 체크 중심인 경우가 많다. 본 프로젝트는 여기에 다음과 같은 차별점을 둔다.

- **Availability-aware reminder**: 사용자의 시간표를 기반으로 실제 수행 가능한 시간에만 알림을 보냄
- **Focus On/Off mode**: 작업 중 이탈을 줄이기 위해 집중 상태를 명시적으로 관리함
- **Delayed confirmation alert**: On 상태에서 Off로 전환할 때 실제 종료 여부를 재확인하여 중도 이탈을 방지함
- **Streak & priority visualization**: 연속 수행 성과와 우선순위를 함께 시각화하여 동기 부여를 강화함

### Core functions
- 할 일 등록 및 수정, 삭제
- 목표 유형 구분
  - **Daily Goal**: 매일 반복 수행해야 하는 목표
  - **Deadline Task**: 기한 내 완료해야 하는 업무
- 목표 완료 여부 체크 및 연속 달성일(streak) 시각화
- 업무 우선순위 설정 및 시각화
- 사용자 시간표 등록 및 가용 시간 분석
- 가용 시간 기반 이메일 알림
- 마감일까지 미완료 시 이메일 알림
- 작업 집중을 위한 **On/Off 모드**
- On 상태에서 Off 전환 시 업무 종료 여부 확인
- 실제 종료가 아니라면 5분 후 사이트 내부 알림 재전송

### Target market
- 대학생 및 고등교육 학습자
- 시험 준비생 및 자기주도 학습자
- 과제, 프로젝트, 개인 목표를 동시에 관리해야 하는 사용자
- 일반적인 체크리스트보다 더 강한 몰입 유도와 피드백을 원하는 사용자

---

## 3. System context diagram

![System Context Diagram](https://github.com/psj-1228/SURF_the_TAsk/blob/main/images/System%20Context%20Diagram.jpg?raw=true)



### Description of terms in the diagram

| Term | Description |
|---|---|
| User | 회원가입, 로그인, 시간표 등록, 목표 등록, 완료 체크, On/Off 전환을 수행하는 최종 사용자 |
| System | 목표 관리, 가용 시간 분석, streak 계산, 우선순위 관리, 알림 판단을 수행하는 핵심 시스템 |
| Email Service | 가용 시간 기반 알림, 마감 임박/마감 초과 알림을 발송하는 외부 이메일 전송 서비스 |
| Desktop Web Browser | 사이트 내부에서 팝업 또는 알림 형태로 사용자에게 메시지를 전달하는 기능 |
| Monitoring Service | 사용자 조회 및 서비스 모니터링 |
| Database | 사용자별 데이터 저장 및 조회 |

### Context explanation
본 시스템의 중심은 SURF the TAsk System이다. 사용자는 웹 환경에서 목표와 시간표를 입력하고, 시스템은 이를 바탕으로 수행 가능한 시간대를 계산한다. 계산 결과에 따라 이메일 알림을 발송하거나, 집중 모드 종료 직후 일정 시간이 지나도 업무가 끝나지 않은 것으로 판단되면 사이트 내부 알림을 재전송한다. 즉, 시스템은 단순 저장소가 아니라 **시간표 분석 + 목표 관리 + 알림 제어 + 몰입 보조**를 결합한 서비스이다.

---

## 4. Use case list

| No. | Use case | Actor | Description |
|---:|---|---|---|
| UC-01 | Sign up / Login | User | 사용자가 계정을 생성하고 로그인하여 개인화된 목표 관리 서비스를 이용한다. |
| UC-02 | Personal information registration | User | 사용자가 수업, 고정 일정, 개인 스케줄을 등록하여 시스템이 가용 시간을 계산할 수 있도록 한다. |
| UC-03 | Register daily goal | User | 사용자가 매일 반복 수행해야 하는 목표를 등록한다. |
| UC-04 | Register deadline task | User | 사용자가 마감 기한을 포함한 업무를 등록한다. |
| UC-05 | Check task completion | User | 사용자가 목표 또는 업무의 완료 여부를 체크한다. |
| UC-06 | Edit item | User | 사용자가 목표 또는 업무를 수정한다. |
| UC-07 | Delete item | User | 사용자가 목표 또는 업무를 삭제한다. |
| UC-08 | Turn On focus mode | User | 사용자가 특정 작업을 시작하며 집중 상태(On)로 전환한다. |
| UC-09 | Turn Off focus mode | User | 사용자가 집중 상태를 종료할 때 실제 업무 종료 여부를 확인한다. |
| UC-10 | Send availability-based reminder | System | 시스템이 사용자의 가용 시간에 아직 수행하지 않은 목표가 있을 경우 이메일 알림을 보낸다. |
| UC-11 | Send deadline warning / overdue alert | System | 시스템이 마감일까지 완료되지 않은 업무에 대해 이메일 알림을 보낸다. |
| UC-12 | Send delayed in-site alert | System | 사용자가 Off로 전환했지만 실제 종료가 아니라고 응답한 경우, 5분 후 사이트 내부 알림을 보낸다. |
| UC-13 | Prioritize tasks | User, System | 마감 시점을 바탕으로 미수행 업무를 정렬하여 보여준다. |
| UC-14 | Review progress | User | 사용자가 수행한 업무와 목표의 기록을 조회한다. |

---

## 5. Concept of operation

| Use case | Purpose | Approach | Dynamics | Goals |
|---|---|---|---|---|
| Sign up / Login | 사용자별 데이터 분리를 통해 개인화된 플래너 환경을 제공한다. | 사용자가 회원가입 후 ID/PW로 로그인하면 서버가 사용자 정보를 검증하고 세션을 생성한다. | 서비스 접속 진입 → 로그인 요청 → 인증 성공 시 홈 화면 진입 | 사용자 인증 및 개인 데이터 보호 |
| Personal information registration | 사용자의 실제 생활 패턴을 시스템이 이해하고 알림을 보낼 이메일을 확인한다. | 사용자가 요일별 수업과 고정 일정을 입력하면 시스템이 빈 시간을 계산 가능한 형태로 저장한다. 또한 이메일 주소도 저장한다. | 시간표 입력 → 저장 → 가용 시간 슬롯 계산 | 가용 시간 기반 알림의 기준 데이터와 알림의 연락처 확보 |
| Register daily goal / deadline task | 수행할 목표를 구조적으로 관리한다. | 사용자가 목표명, 유형, 마감일, 우선순위를 입력하면 시스템이 이를 저장하고 목록에 반영한다. | 목표 입력 → 유형 선택 → 저장 → 목록/대시보드 반영 | 체계적인 목표 관리 기능 구현 |
| Check task completion & streak | 목표 달성 여부와 연속 수행 성과를 기록한다. | 사용자가 완료 버튼을 누르면 시스템이 완료 상태를 변경하고 날짜 기준 streak 및 달성률을 재계산한다. | 완료 체크 → streak 계산 → 시각화 갱신 | 동기 부여를 위한 피드백 제공 |
| Turn On / Off focus mode | 사용자가 작업 시작과 종료를 명시적으로 관리하며 몰입을 유지하도록 한다. | On 버튼으로 작업 시작 상태를 기록하고, Off 전환 시 실제 종료 여부를 사용자에게 재확인한다. | On 전환 → 집중 진행 → Off 전환 → 종료 여부 확인 | 작업 중 이탈 감소 및 실제 작업 시간 추적 |
| Send delayed in-site alert | 사용자가 아직 업무를 끝내지 않았는데 중도 종료하는 상황을 완화한다. | Off 전환 시 “정말 종료했는가?”를 묻고, 아니라면 5분 뒤 사이트 내부 알림을 전송한다. | Off 요청 → 종료 아님 선택 → 5분 대기 → 내부 알림 전송 | 중도 이탈 방지 및 재집중 유도 |
| Send availability-based reminder | 사용 가능한 시간에 아직 남은 목표를 수행하도록 유도한다. | 시간표를 분석하여 현재 또는 가까운 시간대가 빈 시간이고 미완료 목표가 있으면 이메일 알림을 보낸다. | 스케줄 검사 → 가용 시간 판단 → 미완료 목표 탐색 → 이메일 발송 | 불필요한 알림 감소, 적절한 시점의 리마인드 |
| Send deadline warning / overdue alert | 마감 업무 누락을 최소화한다. | 마감일이 임박했거나 이미 지났는데 완료되지 않은 업무를 정기적으로 검사해 이메일을 발송한다. | 마감 시점 검사 → 미완료 여부 판단 → 이메일 발송 | 기한 내 업무 완료율 향상 |

---

## 6. Problem statement

### Problems to be considered

#### 1) Existing To-do apps do not consider real availability
많은 To-do 서비스는 사용자가 무엇을 해야 하는지는 저장하지만, **언제 할 수 있는지**는 충분히 고려하지 않는다. 사용자가 수업 중이거나 이동 중일 때 알림을 보내면 실제 행동으로 이어질 가능성이 낮다. 따라서 본 프로젝트는 사용자의 시간표를 이용하여 빈 시간에만 알림을 제공해야 한다.

#### 2) Task abandonment during work is common
사용자는 할 일을 시작하더라도 중간에 쉽게 이탈한다. 단순한 타이머나 체크리스트는 사용자의 중도 종료를 충분히 제어하지 못한다. 따라서 작업 중이라는 상태를 명시적으로 표현하는 On/Off 기능과 종료 재확인 기능이 필요하다.

#### 3) Motivation decreases when progress is not visible
목표가 꾸준히 수행되고 있는지 한눈에 보이지 않으면 동기 부여가 떨어질 수 있다. 따라서 streak, 달성률, 우선순위 정보를 시각적으로 보여주는 기능이 필요하다.

### Technical difficulties

1. **Timetable-based availability analysis**  
   시간표 데이터를 기반으로 현재 가용 시간과 미래 가용 시간을 계산해야 하며, 고정 일정과 유동 일정의 충돌 처리가 필요하다.

2. **Event-driven reminder logic**  
   가용 시간 기반 이메일 알림, 마감 알림, 5분 후 내부 알림 등 서로 다른 조건의 알림을 정확한 시점에 실행해야 한다.

3. **Reliable state transition for On/Off mode**  
   사용자의 집중 상태 전환을 일관되게 관리해야 하며, 잘못된 종료 처리나 중복 알림을 방지해야 한다.

4. **Streak calculation by date boundary**  
   매일 수행형 목표의 연속 달성일은 날짜 경계에 민감하므로, 하루 단위 완료 판단 규칙을 명확히 정의해야 한다.

5. **Priority and dashboard visualization**  
   업무 우선순위와 달성 현황을 사용자에게 직관적으로 보여줄 수 있는 UI 설계가 필요하다.

### 구현 방식/화면 구성

1. 웹앱 방식으로 작동하며, 서버가 제공하는 웹 서비스를 사용자가 브라우저틀 접속하여 이용하는 방식이다.
2. UI/UX를 통해 수행할 목표/업무를 보여줄 예정이다.
3. 돌아보기 페이지를 따로 생성하여 발자국 형식으로 수행 여부를 보여줄 것이다.

---

## 7. Glossary

| Term | Meaning |
|---|---|
| To-do | 사용자가 수행해야 하는 할 일 또는 업무 |
| Daily Goal | 매일 반복적으로 수행해야 하는 목표 |
| Deadline Task | 정해진 마감 기한 이전에 완료해야 하는 업무 |
| Streak | 목표를 연속으로 수행한 일수 |
| Priority | 업무의 중요도 또는 선행 처리 필요성을 나타내는 값 |
| Availability | 시간표 분석 결과 사용자가 수행 가능한 빈 시간 |
| On Mode | 사용자가 현재 특정 업무를 수행 중인 집중 상태 |
| Off Mode | 사용자가 집중 상태를 종료한 상태 |
| In-site Notification | 웹사이트 내부에서 표시되는 알림 |
| Reminder Engine | 시간표, 미완료 목표, 마감일을 기반으로 알림 발송 여부를 결정하는 로직 |
| Timetable Analysis | 등록된 시간표를 바탕으로 가용 시간을 계산하는 과정 |

---

## 8. References

1. FocusFlight - 비행시간 집중법
2. [Duolingo](https://blog.duolingo.com/ko/what-is-duolingo-streak/) 
3. General idea reference: web-based study planner with motivation elements such as progress and level-up.

---

## Appendix: short project summary
SURF the TAsk는 단순 체크리스트가 아닌, **가용 시간 분석**, **연속 수행 성과 시각화**, **집중 상태 관리(On/Off)**, **이메일 및 내부 알림**을 결합한 지능형 To-do 플래너이다. 본 프로젝트는 사용자가 실제로 할 수 있는 시간에 맞춰 행동을 유도하고, 중도 이탈을 줄이며, 장기적인 습관 형성을 돕는 것을 핵심 가치로 한다.
