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
        weeklyDelta: document.querySelector("[data-weekly-delta]"),
        miniRing: document.querySelector("[data-mini-ring]"),
        bestStreak: document.querySelector("[data-best-streak]"),
        totalFocusTime: document.querySelector("[data-total-focus-time]"),
        completedGoals: document.querySelector("[data-completed-goals]"),
        doneTasks: document.querySelector("[data-done-tasks]"),
        completionDonut: document.querySelector("[data-completion-donut]"),
        donutRate: document.querySelector("[data-donut-rate]"),
        donutDone: document.querySelector("[data-donut-done]"),
        donutIncomplete: document.querySelector("[data-donut-incomplete]"),
        dateRange: document.querySelector("[data-date-range]"),
        dailyLineChart: document.querySelector("[data-daily-line-chart]"),
        priorityList: document.querySelector("[data-priority-list]"),
        insight: document.querySelector("[data-progress-insight]")
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

        const progressResult = await fetchJson("/api/progress");

        if (isUnauthorized(progressResult)) {
            redirectToLogin();
            return;
        }

        const progress = progressResult.ok ? progressResult.data : fallbackProgress();

        renderSummary(progress);
        renderCompletionDonut(progress);
        renderLineChart(asArray(progress.dailyCompletionRates));
        renderPriority(asArray(progress.priorityTasks));
        renderInsight(progress);

        setMessage(progressResult.ok ? "" : "진행도 데이터를 불러오지 못했습니다.", !progressResult.ok);
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
        refs.bestStreak.textContent = (progress.bestDailyGoalStreak || 0) + "일";
        refs.totalFocusTime.textContent = formatMinutes(progress.totalFocusMinutes || 0);
        refs.completedGoals.textContent = (progress.completedGoalCount || progress.doneTasks || 0)
                + " / " + (progress.totalTasks || 0);
        refs.miniRing.style.setProperty("--rate", rate + "%");

        const delta = Number(progress.weeklyCompletionRateDelta || 0);
        refs.weeklyDelta.textContent = "지난 주 대비 " + signedPercent(delta);
        refs.weeklyDelta.className = delta >= 0 ? "trend positive" : "trend negative";

        const dailyRates = asArray(progress.dailyCompletionRates);
        if (dailyRates.length > 0) {
            refs.dateRange.textContent = dailyRates[0].label + " - " + dailyRates[dailyRates.length - 1].label;
        }
    }

    function renderCompletionDonut(progress) {
        const rate = clamp(Math.round(progress.completionRate || 0), 0, 100);
        const done = progress.doneTasks || 0;
        const incomplete = progress.incompleteTasks || 0;

        refs.completionDonut.style.setProperty("--done", rate + "%");
        refs.donutRate.textContent = rate + "%";
        refs.doneTasks.textContent = done;
        refs.donutDone.textContent = done + "개";
        refs.donutIncomplete.textContent = incomplete + "개";
    }

    function renderLineChart(dailyRates) {
        if (dailyRates.length === 0) {
            refs.dailyLineChart.innerHTML = "<p class=\"empty-state\">최근 7일 완료 기록이 없습니다.</p>";
            return;
        }

        const width = 660;
        const height = 280;
        const padding = { top: 22, right: 26, bottom: 46, left: 44 };
        const chartWidth = width - padding.left - padding.right;
        const chartHeight = height - padding.top - padding.bottom;
        const points = dailyRates.map(function (day, index) {
            const x = padding.left + (chartWidth / Math.max(dailyRates.length - 1, 1)) * index;
            const y = padding.top + chartHeight - (clamp(day.completionRate || 0, 0, 100) / 100) * chartHeight;
            return {
                x: x,
                y: y,
                rate: Math.round(day.completionRate || 0),
                label: day.label,
                completedCount: day.completedCount || 0
            };
        });
        const path = points.map(function (point, index) {
            return (index === 0 ? "M " : "L ") + point.x.toFixed(1) + " " + point.y.toFixed(1);
        }).join(" ");
        const areaPath = path + " L " + points[points.length - 1].x.toFixed(1) + " " + (height - padding.bottom)
                + " L " + points[0].x.toFixed(1) + " " + (height - padding.bottom) + " Z";

        refs.dailyLineChart.innerHTML = "<svg viewBox=\"0 0 " + width + " " + height
                + "\" role=\"img\" aria-label=\"최근 7일 일별 완료율 선 그래프\">"
                + "<defs>"
                + "<linearGradient id=\"dailyLineArea\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">"
                + "<stop offset=\"0%\" stop-color=\"#26b97a\" stop-opacity=\"0.22\"/>"
                + "<stop offset=\"100%\" stop-color=\"#26b97a\" stop-opacity=\"0.02\"/>"
                + "</linearGradient>"
                + "</defs>"
                + gridMarkup(width, padding, chartHeight)
                + "<path class=\"chart-area\" d=\"" + areaPath + "\"></path>"
                + "<path class=\"chart-line\" d=\"" + path + "\"></path>"
                + pointMarkup(points)
                + xAxisMarkup(points, height, padding)
                + "</svg>";
    }

    function gridMarkup(width, padding, chartHeight) {
        return [0, 50, 100].map(function (value) {
            const y = padding.top + chartHeight - (value / 100) * chartHeight;
            return "<g class=\"chart-grid\">"
                    + "<line x1=\"" + padding.left + "\" y1=\"" + y + "\" x2=\"" + (width - padding.right)
                    + "\" y2=\"" + y + "\"></line>"
                    + "<text x=\"12\" y=\"" + (y + 4) + "\">" + value + "</text>"
                    + "</g>";
        }).join("");
    }

    function pointMarkup(points) {
        return points.map(function (point) {
            return "<g class=\"chart-point\">"
                    + "<circle cx=\"" + point.x.toFixed(1) + "\" cy=\"" + point.y.toFixed(1) + "\" r=\"5\"></circle>"
                    + "<text x=\"" + point.x.toFixed(1) + "\" y=\"" + (point.y - 12).toFixed(1) + "\">"
                    + point.rate + "%</text>"
                    + "</g>";
        }).join("");
    }

    function xAxisMarkup(points, height, padding) {
        return points.map(function (point) {
            return "<text class=\"x-label\" x=\"" + point.x.toFixed(1) + "\" y=\"" + (height - padding.bottom + 28)
                    + "\">" + escapeHtml(point.label) + "</text>";
        }).join("");
    }

    function renderPriority(tasks) {
        refs.priorityList.innerHTML = "";

        if (tasks.length === 0) {
            refs.priorityList.appendChild(emptyState("우선순위 Task가 없습니다."));
            return;
        }

        tasks.slice(0, 5).forEach(function (task, index) {
            const item = document.createElement("article");
            item.className = "priority-row";
            item.innerHTML = "<span class=\"priority-rank\">" + (index + 1) + "</span>"
                    + "<div class=\"priority-main\">"
                    + "<strong>" + escapeHtml(task.title || "제목 없음") + "</strong>"
                    + "<small>" + escapeHtml(taskDeadlineMeta(task)) + "</small>"
                    + "</div>"
                    + "<span class=\"priority-badge " + priorityTone(task.importance || 0) + "\">"
                    + priorityLabel(task.importance || 0) + "</span>";
            refs.priorityList.appendChild(item);
        });
    }

    function renderInsight(progress) {
        const delta = Number(progress.weeklyCompletionRateDelta || 0);
        if (delta > 0) {
            refs.insight.textContent = "이번 주 수행율이 지난주보다 " + signedPercent(delta)
                    + " 좋아졌습니다. 좋은 흐름을 계속 타보세요.";
            return;
        }
        if (delta < 0) {
            refs.insight.textContent = "이번 주 수행율이 지난주보다 " + signedPercent(delta)
                    + " 낮습니다. 작은 목표부터 다시 올려보세요.";
            return;
        }
        refs.insight.textContent = "이번 주 수행율이 지난주와 비슷합니다. 오늘 완료 체크로 흐름을 바꿀 수 있습니다.";
    }

    function fallbackProgress() {
        return {
            totalTasks: 0,
            doneTasks: 0,
            incompleteTasks: 0,
            completionRate: 0,
            bestDailyGoalStreak: 0,
            totalFocusMinutes: 0,
            completedGoalCount: 0,
            weeklyCompletionRateDelta: 0,
            dailyCompletionRates: [],
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

    function taskDeadlineMeta(task) {
        if (!task.deadlineAt) {
            return task.taskType === "DAILY_GOAL" ? "Daily Goal" : "마감 미설정";
        }
        const days = Math.ceil((new Date(task.deadlineAt).getTime() - Date.now()) / (24 * 60 * 60 * 1000));
        return days >= 0 ? "D-" + days : "기한 초과";
    }

    function priorityTone(importance) {
        if (importance >= 5) {
            return "high";
        }
        if (importance >= 3) {
            return "medium";
        }
        return "low";
    }

    function priorityLabel(importance) {
        if (importance >= 5) {
            return "높음";
        }
        if (importance >= 3) {
            return "보통";
        }
        return "낮음";
    }

    function signedPercent(value) {
        const rounded = Math.round(value);
        return (rounded > 0 ? "+" : "") + rounded + "%";
    }

    function formatMinutes(minutes) {
        const safeMinutes = Math.max(Number(minutes) || 0, 0);
        const hours = Math.floor(safeMinutes / 60);
        const rest = safeMinutes % 60;

        if (hours === 0) {
            return rest + "분";
        }
        return hours + "h " + rest + "m";
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
