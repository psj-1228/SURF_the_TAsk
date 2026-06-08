# Dashboard Today Achievements Wave Design

## Goal

Update the dashboard `wave-panel` so it describes today's accomplishment, not monthly or cumulative progress. The panel should show how many Daily Goals and deadline Tasks were completed today, using a shell illustration for Daily Goals and a conch illustration for Tasks.

## User-Facing Design

- Change the panel heading from monthly progress to today's accomplishment.
- Keep the ocean identity, but make the wave visual feel more like an illustration than a plain progress bar.
- Show two achievement chips inside the wave scene:
  - Daily Goal: pretty shell illustration, completed Daily Goal count.
  - Task: pretty conch illustration, completed deadline Task count.
- The wave height should respond to today's total completed count so the scene still feels alive.
- The note should explain today's completed Daily Goals and Tasks in a short sentence.

## Data Flow

- Extend `/api/progress` with today's completed counts:
  - `todayCompletedDailyGoals`
  - `todayCompletedDeadlineTasks`
  - `todayCompletedTasks`
  - `todayCompletionRate`
- Use `completion_records.completed_date == LocalDate.now()` and task subtype to calculate the counts.
- Keep existing progress fields intact so the progress page and dashboard summaries do not regress.
- In `dashboard.js`, render the wave panel from these new fields and fall back to current done/total values only when the new fields are missing.

## Components

- `ProgressResDto`: add today's accomplishment fields.
- `ProgressService`: calculate today's completion records by type.
- `dashboard/index.html`: add semantic elements for the shell/conch achievement display.
- `dashboard.js`: update `renderProgress` to populate today's wave heading, rate, icons, counts, and note.
- `dashboard.css`: restyle `.wave-visual` into an illustrated scene with layered waves, foam, light, shell, and conch visuals.

## Testing

- Add or update progress integration coverage to prove today's counts split Daily Goals and Tasks correctly.
- Update dashboard page rendering coverage if the HTML structure changes.
- Verify only the modified dashboard area through the developer build on port `8081`.

## Constraints

- Preserve existing user changes in the dirty worktree.
- Keep edits scoped to the dashboard wave panel and progress response data needed for it.
- Use CSS-built illustrations so no new image assets are required.
