# Personal Schedule Page Design

## Goal

Build a personal schedule page that matches prototype screen 3: a dedicated timetable page with the schedule grid and the registration UI visible at the same time.

## Route

The user-facing route is `/schedule`.

## Experience

The page keeps the existing SURF the TAsk sidebar style. The center area shows a weekly timetable from Monday to Friday, from 07:00 through 23:00. The right panel contains an inline schedule form, so users can add a personal schedule without navigating away or opening a modal.

Registered schedules appear as colored blocks inside the timetable. Each block shows the title and time range. A compact list below the grid gives users an easy way to scan and delete schedules.

## Data Flow

The page loads `/api/schedules` and `/api/availability` with the stored JWT token. Creating a schedule posts to `/api/schedules`. Deleting a schedule calls `DELETE /api/schedules/{scheduleId}`. After each change, the page reloads schedules and availability.

## Availability Rule

Available time means time that is not occupied by a registered personal schedule and is not in the reserved 00:00-07:00 range. The server-side schedule analyzer should start availability at 07:00 so dashboard and schedule pages share the same rule.

## Verification

Add MVC rendering coverage for `/schedule`. Add unit coverage that `ScheduleAnalyzer` never emits availability before 07:00.
