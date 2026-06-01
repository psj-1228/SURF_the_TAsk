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
| 05/31/2026 | 2.00 | UseCase 수정 | 박성준 |
| 05/31/2026 | 2.10 | 문맥 수정 | 박성준 |
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
많은 사용자가 To-do 리스트 앱을 사용하지만, 기존 서비스는 할 일을 단순히 기록하는 데 그치는 경우가 많다. 특히 대학생이나 자기주도 학습자는 수업, 과제, 시험 준비, 개인 일정이 동시에 존재하므로 실제로 **언제 수행할 수 있는지**를 고려한 계획 관리가 필요하다. 그러나 일반적인 To-do 앱은 사용자의 시간표나 가용 시간을 충분히 반영하지 못해, 바쁜 시간에 알림이 오거나 정작 수행 가능한 시간에는 적절한 유도가 이루어지지 않는 문제가 있다.

또한 사용자는 할 일을 시작하더라도 작업 중 다른 앱이나 활동으로 쉽게 이탈할 수 있다. 단순한 체크리스트만으로는 집중 상태를 유지하기 어렵고, 반복 수행에 대한 성취감도 충분히 제공하기 어렵다. 따라서 본 프로젝트는 할 일 관리에 **집중 유지**, **가용 시간 기반 알림**, **연속 수행 성과 시각화**를 결합한 지능형 플래너를 제안한다.

### Motivation
본 프로젝트의 동기는 다음과 같다.

1. 사용자의 실제 생활 패턴을 반영한 더 현실적인 계획 관리 기능이 필요하다.
2. 할 일을 단순히 기록하는 것을 넘어, 완료까지 이어지도록 집중을 유도하는 기능이 필요하다.
3. 매일 수행해야 하는 목표의 연속 달성 현황을 시각화하면 동기 부여 효과를 높일 수 있다.
4. 마감 기한이 있는 업무에 대해 적절한 시점에 알림을 제공하면 미루는 행동을 줄일 수 있다.

### Goal
본 프로젝트의 목표는 **사용자의 개인 시간표를 분석하여 가용 시간에 맞춰 알림을 제공하고, 업무 우선순위와 연속 수행 성과를 시각화하며, 집중 모드(On/Off)를 통해 몰입을 돕는 웹 기반 지능형 플래너를 개발하는 것**이다.

### Differentiation
기존 To-do 리스트는 일정 기록과 완료 체크에 초점을 두는 경우가 많다. 본 프로젝트는 다음과 같은 기능을 통해 차별성을 확보한다.

- **Availability-aware reminder**: 사용자의 시간표를 기반으로 실제 수행 가능한 시간에 알림을 제공한다.
- **Focus On/Off mode**: 작업 중 이탈을 줄이기 위해 집중 상태를 명시적으로 관리한다.
- **Delayed confirmation alert**: On 상태에서 Off로 전환할 때 실제 종료 여부를 재확인하여 중도 이탈을 방지한다.
- **Streak & priority visualization**: 연속 수행 성과와 우선순위를 함께 시각화하여 동기 부여를 강화한다.

### Core functions
- 할 일 등록 및 수정, 삭제
- 목표 유형 구분
  - **Daily Goal**: 매일 반복 수행해야 하는 목표
  - **Deadline Task**: 기한 내 완료해야 하는 업무
- 목표 완료 여부 체크 및 연속 달성일(streak) 시각화
- 업무 우선순위 설정 및 시각화
- 사용자 시간표 등록 및 가용 시간 분석
- 가용 시간 기반 이메일 알림
- 마감 임박 또는 마감 초과 시 이메일 알림
- 작업 집중을 위한 **On/Off 모드**
- On 상태에서 Off 전환 시 업무 종료 여부 확인
- 실제 종료가 아닌 경우 5분 후 사이트 내부 알림 재전송

### Target market
- 대학생 및 고등교육 학습자
- 시험 준비생 및 자기주도 학습자
- 과제, 프로젝트, 개인 목표를 동시에 관리해야 하는 사용자
- 일반적인 체크리스트보다 강한 몰입 유도와 피드백을 원하는 사용자

---

## 3. System context diagram

![System Context Diagram](https://github.com/psj-1228/SURF_the_TAsk/blob/main/images/System%20Context%20Diagram.jpg?raw=true)



### Description of terms in the diagram

| Term | Description |
|---|---|
| User | 회원가입, 로그인, 시간표 등록, 목표 등록, 완료 체크, On/Off 전환을 수행하는 최종 사용자 |
| System | 목표 관리, 가용 시간 분석, streak 계산, 우선순위 관리, 알림 판단을 담당하는 핵심 시스템 |
| Email Service | 가용 시간 기반 알림, 마감 임박/마감 초과 알림을 발송하는 외부 이메일 전송 서비스 |
| Desktop Web Browser | 웹 UI를 렌더링하고 사이트 내부 알림을 사용자에게 표시하는 실행 환경 |
| Monitoring Service | 사용자 조회 및 서비스 모니터링 |
| Database | 사용자별 데이터 저장 및 조회 |

### Context explanation
본 시스템의 중심은 SURF the TAsk System이다. 사용자는 웹 환경에서 목표와 시간표를 입력하고, 시스템은 입력된 정보를 바탕으로 수행 가능한 시간대를 계산한다. 계산 결과에 따라 이메일 알림을 발송하며, 집중 모드 종료 과정에서 사용자가 실제 종료가 아니라고 응답한 경우에는 일정 시간이 지난 뒤 사이트 내부 알림을 다시 제공한다. 즉, 본 시스템은 단순한 저장소가 아니라 **시간표 분석 + 목표 관리 + 알림 제어 + 몰입 보조**를 결합한 서비스이다.

---

## 4. Use case list

| Use Case ID | Use Case Name | Korean Name | Primary Actor |
|---|---|---|---|
| UC-01 | Register | 회원가입 | User |
| UC-02 | Log in | 로그인 | User |
| UC-03 | Register personal schedule | 개인 시간표 등록 | User |
| UC-04 | Register daily goal | 매일 목표 등록 | User |
| UC-05 | Register deadline task | 마감 업무 등록 | User |
| UC-06 | Check task completion | 완료 체크 | User |
| UC-07 | Edit item | 항목 수정 | User |
| UC-08 | Delete item | 항목 삭제 | User |
| UC-09 | Turn on focus mode | 집중 모드 시작 | User |
| UC-10 | Turn off focus mode | 집중 모드 종료 | User |
| UC-11 | Prioritize tasks | 업무 우선순위 확인 | User, System |
| UC-12 | Review progress | 진행 현황 확인 | User |
| UC-13 | Send availability-based reminder | 가용 시간 기반 알림 전송 | Scheduler, Email Service |
| UC-14 | Send deadline warning / overdue alert | 마감 경고/초과 알림 전송 | Scheduler, Email Service |
| UC-15 | Send delayed in-site alert | 지연 사이트 내부 알림 전송 | Scheduler, Desktop Web Browser |

---

## 5. Concept of operation

| Use case | Purpose | Approach | Dynamics | Goals |
|---|---|---|---|---|
| Register | 사용자가 개인화된 플래너 기능을 사용할 수 있도록 계정을 생성한다. | 사용자가 ID, Password, 이메일, 이름 등의 필수 정보를 입력하면 시스템이 입력 형식과 중복 여부를 검사한 뒤 계정 정보를 저장한다. | 회원가입 화면 진입 → 정보 입력 → 검증 → 계정 저장 → 로그인 화면 이동 | 사용자 계정 생성 및 개인 데이터 관리 기반 확보 |
| Log in | 등록된 계정으로 개인화된 목표, 시간표, 알림, 진행 현황 기능에 접근한다. | 사용자가 ID와 Password를 입력하면 시스템이 저장된 회원 정보와 비교하고 로그인 세션을 생성한다. | 로그인 요청 → 계정 검증 → 세션 생성 → 개인화된 홈 화면 진입 | 사용자 인증 및 개인 데이터 보호 |
| Register personal schedule | 사용자의 실제 생활 패턴을 반영하여 가용 시간을 계산할 수 있도록 한다. | 사용자가 요일, 시작 시간, 종료 시간, 일정명, 반복 여부 등을 입력하면 시스템이 시간 충돌 여부를 검사한 뒤 일정을 저장한다. | 시간표 입력 → 저장 → 가용 시간 슬롯 계산 | 가용 시간 기반 알림의 기준 데이터 확보 |
| Register daily goal | 매일 반복 수행해야 하는 목표를 구조적으로 관리한다. | 사용자가 목표명, 설명, 예상 수행 시간, 중요도 등을 입력하면 시스템이 이를 Daily Goal 항목으로 저장하고 목록에 반영한다. | 목표 입력 → Daily Goal 유형 선택 → 저장 → 목록/진행 현황 반영 | 반복 목표 관리 및 streak 계산 기반 확보 |
| Register deadline task | 마감 기한이 있는 업무를 우선순위와 알림 대상으로 관리한다. | 사용자가 업무명, 설명, 마감일, 예상 수행 시간, 중요도 등을 입력하면 시스템이 이를 Deadline Task 항목으로 저장한다. | 업무 입력 → Deadline Task 유형 선택 → 저장 → 우선순위/마감 알림 대상 반영 | 기한 내 업무 완료율 향상 |
| Check task completion | 목표 또는 업무의 완료 여부를 기록하고 진행 현황을 갱신한다. | 사용자가 완료 버튼을 누르면 시스템이 완료 상태와 완료 시각을 저장하고 streak 및 달성률을 재계산한다. | 완료 체크 → 완료 기록 저장 → streak/달성률 계산 → 시각화 갱신 | 동기 부여를 위한 피드백 제공 |
| Edit item | 등록된 목표 또는 업무의 변경 사항을 반영한다. | 사용자가 항목 상세 화면에서 제목, 설명, 마감일, 중요도, 예상 수행 시간을 수정하면 시스템이 변경 값을 검증한 뒤 저장한다. | 항목 선택 → 수정 입력 → 검증 → 저장 → 목록/알림 조건 갱신 | 목표와 업무 정보의 최신성 유지 |
| Delete item | 더 이상 필요하지 않은 목표 또는 업무를 정리한다. | 사용자가 삭제를 확인하면 시스템이 해당 항목을 삭제하고 목표 목록, 우선순위 목록, 진행 현황을 갱신한다. | 항목 선택 → 삭제 확인 → 삭제 처리 → 관련 화면 갱신 | 불필요한 데이터 제거 및 목록 정리 |
| Turn on focus mode | 사용자가 특정 작업의 시작을 명시하고 집중 상태를 유지하도록 돕는다. | 사용자가 Focus On을 누르면 시스템이 기존 집중 상태를 확인하고 Focus Session의 시작 시간을 기록한다. | 작업 선택 → Focus On 요청 → 집중 상태 확인 → 세션 시작 | 작업 몰입 유도 및 실제 작업 시간 추적 |
| Turn off focus mode | 집중 상태 종료 시 실제 업무 종료 여부를 확인한다. | 사용자가 Focus Off를 누르면 시스템이 종료 확인 팝업을 표시하고, 실제 종료가 확인된 경우 종료 시간을 기록한다. | Focus Off 요청 → 종료 여부 확인 → 세션 종료 또는 유지 → 상태 갱신 | 잘못된 종료 처리 방지 및 집중 상태 일관성 유지 |
| Prioritize tasks | 사용자가 먼저 수행해야 할 업무를 쉽게 확인할 수 있도록 한다. | 시스템이 미완료 Daily Goal과 Deadline Task의 마감일, 중요도, 완료 여부를 기준으로 업무를 정렬한다. | 업무 목록 조회 → 우선순위 계산 → 정렬 목록 표시 | 사용자의 업무 선택 부담 감소 |
| Review progress | 사용자가 수행 기록, streak, 달성률, 우선순위 현황을 확인한다. | 시스템이 목표, 업무, 완료 기록을 불러와 진행 현황을 계산하고 그래프와 목록 형태로 제공한다. | 진행 현황 진입 → 기록 조회 → 지표 계산 → 시각화 표시 | 장기적인 습관 형성과 성취감 제공 |
| Send availability-based reminder | 사용자가 실제로 수행 가능한 시간에 미완료 목표를 진행하도록 유도한다. | Scheduler가 시간표를 분석하여 현재 또는 가까운 시간대가 가용 시간이고 미완료 목표가 있으면 Email Service에 발송을 요청한다. | 스케줄 검사 → 가용 시간 판단 → 미완료 목표 탐색 → 이메일 발송 | 불필요한 알림 감소 및 적절한 시점의 리마인드 |
| Send deadline warning / overdue alert | 마감 업무 누락을 최소화한다. | Scheduler가 Deadline Task의 마감일과 완료 여부를 검사하고, 마감 임박 또는 초과 상태인 경우 Email Service에 발송을 요청한다. | 마감 상태 검사 → 미완료 여부 판단 → 이메일 발송 → 전송 기록 저장 | 마감 기한 준수 지원 |
| Send delayed in-site alert | 사용자가 실제로 업무를 끝내지 않았는데 중도 종료하는 상황을 완화한다. | 사용자가 Focus Off 과정에서 실제 종료가 아니라고 응답하면 Scheduler가 5분 후 Desktop Web Browser에 사이트 내부 알림을 표시한다. | 종료 아님 선택 → 5분 대기 → 완료 여부 확인 → 내부 알림 전송 | 중도 이탈 방지 및 재집중 유도 |

---

## 6. Problem statement

### Problems to be considered

#### 1) Existing To-do apps do not consider real availability
많은 To-do 서비스는 사용자가 무엇을 해야 하는지는 저장하지만, **언제 수행할 수 있는지**는 충분히 고려하지 않는다. 사용자가 수업 중이거나 이동 중일 때 알림을 받으면 실제 행동으로 이어질 가능성이 낮다. 따라서 본 프로젝트는 사용자의 시간표를 기반으로 수행 가능한 시간에 알림을 제공해야 한다.

#### 2) Task abandonment during work is common
사용자는 할 일을 시작하더라도 작업 중간에 쉽게 이탈할 수 있다. 단순한 타이머나 체크리스트만으로는 중도 종료를 충분히 방지하기 어렵다. 따라서 작업 중이라는 상태를 명시적으로 표현하는 On/Off 기능과 종료 재확인 기능이 필요하다.

#### 3) Motivation decreases when progress is not visible
목표가 꾸준히 수행되고 있는지 한눈에 확인하기 어렵다면 사용자의 동기 부여가 약해질 수 있다. 따라서 streak, 달성률, 우선순위 정보를 시각적으로 제공하는 기능이 필요하다.

### Technical difficulties

1. **Timetable-based availability analysis**
   시간표 데이터를 기반으로 현재와 미래의 가용 시간을 계산해야 하며, 고정 일정과 유동 일정의 충돌도 처리해야 한다.

2. **Event-driven reminder logic**
   가용 시간 기반 이메일 알림, 마감 알림, 5분 후 내부 알림처럼 조건이 다른 알림을 정확한 시점에 실행해야 한다.

3. **Reliable state transition for On/Off mode**
   사용자의 집중 상태 전환을 일관되게 관리하고, 잘못된 종료 처리나 중복 알림을 방지해야 한다.

4. **Streak calculation by date boundary**
   매일 수행형 목표의 연속 달성일은 날짜 경계에 민감하므로 하루 단위 완료 판단 규칙을 명확히 정의해야 한다.

5. **Priority and dashboard visualization**
   업무 우선순위와 달성 현황을 사용자가 직관적으로 이해할 수 있도록 UI를 설계해야 한다.

### 구현 방식/화면 구성

1. 본 시스템은 웹앱 방식으로 동작하며, 사용자는 브라우저로 서버가 제공하는 웹 서비스에 접속하여 기능을 이용한다.
2. 주요 화면에서는 사용자가 수행해야 할 목표와 업무를 직관적으로 확인할 수 있도록 구성한다.
3. 진행 현황 화면에서는 완료 기록을 발자국 형식으로 표현하여 사용자가 자신의 수행 여부를 쉽게 돌아볼 수 있도록 한다.

---

## 7. Glossary

| Term | Meaning |
|---|---|
| To-do | 사용자가 수행해야 하는 할 일 또는 업무 |
| Daily Goal | 매일 반복적으로 수행해야 하는 목표 |
| Deadline Task | 정해진 마감 기한 이전에 완료해야 하는 업무 |
| Streak | 목표를 연속으로 수행한 일수 |
| Priority | 업무의 중요도 또는 우선 처리 필요성을 나타내는 값 |
| Availability | 시간표 분석 결과 사용자가 업무를 수행할 수 있는 빈 시간 |
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
SURF the TAsk는 단순 체크리스트가 아니라 **가용 시간 분석**, **연속 수행 성과 시각화**, **집중 상태 관리(On/Off)**, **이메일 및 내부 알림**을 결합한 지능형 To-do 플래너이다. 본 프로젝트는 사용자가 실제로 수행 가능한 시간에 맞춰 행동하도록 유도하고, 중도 이탈을 줄이며, 장기적인 습관 형성을 돕는 것을 핵심 가치로 한다.
