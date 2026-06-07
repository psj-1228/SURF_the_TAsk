(function () {
    const storageKey = "surfUser";
    const user = readStoredUser();

    if (!user.token) {
        window.location.href = "/login";
        return;
    }

    const refs = {
        date: document.querySelector("[data-progress-date]"),
        message: document.querySelector("[data-progress-message]"),
        completionRate: document.querySelector("[data-completion-rate]"),
        waveRate: document.querySelector("[data-wave-rate]"),
        waveVisual: document.querySelector("[data-wave-visual]"),
        waveNote: document.querySelector("[data-wave-note]"),
        doneTasks: document.querySelector("[data-done-tasks]"),
        totalTasks: document.querySelector("[data-total-tasks]"),
        incompleteTasks: document.querySelector("[data-incomplete-tasks]"),
        bestStreak: document.querySelector("[data-best-streak]"),
        dailyGoalCount: document.querySelector("[data-daily-goal-count]"),
        dailyStreakList: document.querySelector("[data-daily-streak-list]"),
        priorityList: document.querySelector("[data-priority-list]")
    };

    refs.date.textContent = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long"
    }).format(new Date());

    loadProgress();

    async function loadProgress() {
        setMessage("진행도 데이터를 불러오는 중입니다.");

        const [progressResult, tasksResult] = await Promise.all([
            fetchJson("/api/progress"),
            fetchJson("/api/tasks")
        ]);

        if ([progressResult, tasksResult].some(isUnauthorized)) {
            redirectToLogin();
            return;
        }

        const tasks = tasksResult.ok ? asArray(tasksResult.data) : [];
        const progress = progressResult.ok ? progressResult.data : fallbackProgress(tasks);

        renderSummary(progress);
        renderDailyGoalStreaks(tasks);
        renderPriority(asArray(progress.priorityTasks));

        setMessage(progressResult.ok && tasksResult.ok ? "" : "일부 진행도 데이터를 불러오지 못했습니다.", !progressResult.ok || !tasksResult.ok);
    }

    async function fetchJson(url) {
        try {
            const response = await fetch(url, {
                headers: {
                    "Authorization": "Bearer " + user.token
                }
            });
            const data = await readJson(response);
            return { ok: response.ok, status: response.status, data: data };
        } catch (error) {
            return { ok: false, status: 0, data: null };
        }
    }

    function renderSummary(progress) {
        const rate = clamp(Math.round(progress.completionRate || 0), 0, 100);
        refs.completionRate.textContent = rate + "%";
        refs.waveRate.textContent = rate + "%";
        refs.waveVisual.style.setProperty("--wave-level", Math.max(rate, 12) + "%");
        refs.doneTasks.textContent = progress.doneTasks || 0;
        refs.totalTasks.textContent = "전체 " + (progress.totalTasks || 0) + "개";
        refs.incompleteTasks.textContent = progress.incompleteTasks || 0;
        refs.bestStreak.textContent = (progress.bestDailyGoalStreak || 0) + "일";
        refs.waveNote.textContent = rate >= 80
                ? "이번 달 진행도가 안정적으로 올라왔습니다."
                : "완료 체크가 쌓이면 이번 달 파도가 더 높아집니다.";
    }

    function renderDailyGoalStreaks(tasks) {
        const dailyGoals = tasks.filter(function (task) {
            return task.taskType === "DAILY_GOAL";
        }).sort(compareDailyGoals);

        refs.dailyGoalCount.textContent = dailyGoals.length;
        refs.dailyStreakList.innerHTML = "";

        if (dailyGoals.length === 0) {
            const empty = emptyState("아직 Daily Goal이 없습니다. ");
            const link = document.createElement("a");
            link.href = "/dashboard";
            link.textContent = "대시보드에서 추가";
            empty.appendChild(link);
            refs.dailyStreakList.appendChild(empty);
            return;
        }

        dailyGoals.forEach(function (goal) {
            const item = document.createElement("article");
            item.className = "streak-item";
            item.innerHTML = "<div class=\"streak-score\">"
                    + "<strong>" + (goal.currentStreak || 0) + "</strong>"
                    + "<span>days</span>"
                    + "</div>"
                    + "<div>"
                    + "<strong>" + escapeHtml(goal.title || "제목 없음") + "</strong>"
                    + "<small>" + escapeHtml(goal.description || "하루 목표 " + (goal.targetCountPerDay || 1) + "회") + "</small>"
                    + "<div class=\"meta-row\">"
                    + "<span>하루 " + (goal.targetCountPerDay || 1) + "회</span>"
                    + "<span>" + statusLabel(goal.status) + "</span>"
                    + "<span>마지막 " + formatDate(goal.lastCompletedDate) + "</span>"
                    + "<span>" + (goal.estimatedMinutes || 0) + "분</span>"
                    + "<span>중요도 " + (goal.importance || 0) + "</span>"
                    + "</div>"
                    + "</div>";
            refs.dailyStreakList.appendChild(item);
        });
    }

    function renderPriority(tasks) {
        refs.priorityList.innerHTML = "";

        if (tasks.length === 0) {
            refs.priorityList.appendChild(emptyState("우선순위 Task가 없습니다."));
            return;
        }

        tasks.slice(0, 5).forEach(function (task) {
            const item = document.createElement("article");
            item.className = "task-item";
            item.innerHTML = "<strong>" + escapeHtml(task.title || "제목 없음") + "</strong>"
                    + "<small>" + escapeHtml(task.description || taskTypeLabel(task.taskType)) + "</small>"
                    + "<div class=\"meta-row\">"
                    + "<span>" + taskTypeLabel(task.taskType) + "</span>"
                    + "<span>" + statusLabel(task.status) + "</span>"
                    + "<span>중요도 " + (task.importance || 0) + "</span>"
                    + taskDeadlineMeta(task)
                    + "</div>";
            refs.priorityList.appendChild(item);
        });
    }

    function compareDailyGoals(a, b) {
        const streakDiff = (b.currentStreak || 0) - (a.currentStreak || 0);
        if (streakDiff !== 0) {
            return streakDiff;
        }

        const dateDiff = dateValue(b.lastCompletedDate) - dateValue(a.lastCompletedDate);
        if (dateDiff !== 0) {
            return dateDiff;
        }

        return (b.importance || 0) - (a.importance || 0);
    }

    function taskDeadlineMeta(task) {
        if (!task.deadlineAt) {
            return "";
        }
        return "<span>마감 " + formatDateTime(task.deadlineAt) + "</span>";
    }

    function fallbackProgress(tasks) {
        const done = asArray(tasks).filter(function (task) { return task.status === "DONE"; }).length;
        return {
            totalTasks: asArray(tasks).length,
            doneTasks: done,
            incompleteTasks: asArray(tasks).length - done,
            completionRate: asArray(tasks).length === 0 ? 0 : done / asArray(tasks).length * 100,
            bestDailyGoalStreak: asArray(tasks)
                    .filter(function (task) { return task.taskType === "DAILY_GOAL"; })
                    .reduce(function (best, task) { return Math.max(best, task.currentStreak || 0); }, 0),
            priorityTasks: []
        };
    }

    function readJson(response) {
        return response.text().then(function (text) {
            return text ? JSON.parse(text) : null;
        });
    }

    function readStoredUser() {
        try {
            return JSON.parse(localStorage.getItem(storageKey) || "{}");
        } catch (error) {
            return {};
        }
    }

    function isUnauthorized(result) {
        return result.status === 401;
    }

    function redirectToLogin() {
        localStorage.removeItem(storageKey);
        window.location.href = "/login";
    }

    function asArray(value) {
        return Array.isArray(value) ? value : [];
    }

    function emptyState(text) {
        const node = document.createElement("p");
        node.className = "empty-state";
        node.textContent = text;
        return node;
    }

    function dateValue(value) {
        return value ? new Date(value).getTime() : 0;
    }

    function formatDate(value) {
        if (!value) {
            return "기록 없음";
        }
        return new Intl.DateTimeFormat("ko-KR", {
            month: "short",
            day: "numeric"
        }).format(new Date(value));
    }

    function formatDateTime(value) {
        if (!value) {
            return "미설정";
        }
        return new Intl.DateTimeFormat("ko-KR", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }).format(new Date(value));
    }

    function taskTypeLabel(value) {
        return value === "DAILY_GOAL" ? "Daily Goal" : "Deadline Task";
    }

    function statusLabel(value) {
        const labels = {
            TODO: "대기",
            IN_PROGRESS: "진행 중",
            DONE: "완료"
        };
        return labels[value] || value || "상태 없음";
    }

    function escapeHtml(value) {
        return String(value)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#039;");
    }

    function clamp(value, min, max) {
        return Math.min(Math.max(value, min), max);
    }

    function setMessage(text, isError) {
        refs.message.textContent = text || "";
        refs.message.className = isError ? "load-message error" : "load-message";
    }
})();
