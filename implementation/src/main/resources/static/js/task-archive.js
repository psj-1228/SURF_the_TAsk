(function () {
    const storageKey = "surfUser";
    const page = document.querySelector("[data-task-archive-page]");
    const user = readStoredUser();

    if (!page) {
        return;
    }

    if (!user.token) {
        window.location.href = "/login";
        return;
    }

    const refs = {
        date: document.querySelector("[data-task-archive-date]"),
        logout: document.querySelector("[data-logout]"),
        message: document.querySelector("[data-task-archive-message]"),
        list: document.querySelector("[data-task-archive-list]"),
        count: document.querySelector("[data-task-archive-count]"),
        total: document.querySelector("[data-task-archive-total]"),
        active: document.querySelector("[data-task-archive-active]"),
        done: document.querySelector("[data-task-archive-done]")
    };

    refs.date.textContent = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long"
    }).format(new Date());

    refs.logout.addEventListener("click", logout);
    loadTasks();

    async function loadTasks() {
        setMessage("목록을 불러오는 중입니다.");
        const result = await fetchJson("/api/tasks");

        if (isUnauthorized(result)) {
            redirectToLogin();
            return;
        }

        if (!result.ok) {
            setMessage(formatError(result.data), true);
            renderTasks([]);
            return;
        }

        const type = page.dataset.taskArchivePage === "daily" ? "DAILY_GOAL" : "DEADLINE_TASK";
        const tasks = asArray(result.data).filter(function (task) {
            return task.taskType === type;
        });
        renderTasks(sortTasks(tasks));
        setMessage("");
    }

    async function logout() {
        try {
            await fetch("/api/auth/logout", {
                method: "POST",
                headers: authHeaders()
            });
        } finally {
            localStorage.removeItem(storageKey);
            window.location.href = "/login";
        }
    }

    async function fetchJson(url) {
        try {
            const response = await fetch(url, {
                headers: authHeaders()
            });
            const data = await readJson(response);
            return { ok: response.ok, status: response.status, data: data };
        } catch (error) {
            return { ok: false, status: 0, data: null };
        }
    }

    function renderTasks(tasks) {
        refs.total.textContent = tasks.length;
        refs.count.textContent = tasks.length;
        refs.done.textContent = tasks.filter(function (task) {
            return task.status === "DONE";
        }).length;
        refs.active.textContent = tasks.filter(function (task) {
            return task.status !== "DONE";
        }).length;
        refs.list.innerHTML = "";

        if (tasks.length === 0) {
            refs.list.appendChild(emptyState("아직 표시할 항목이 없습니다."));
            return;
        }

        tasks.forEach(function (task) {
            const item = document.createElement("article");
            item.className = "task-archive-item";
            item.innerHTML = "<div class=\"task-archive-title\">"
                    + "<strong>" + escapeHtml(task.title || "제목 없음") + "</strong>"
                    + "<span class=\"status-chip\">" + statusLabel(task.status) + "</span>"
                    + "</div>"
                    + "<p>" + escapeHtml(task.description || detailText(task)) + "</p>"
                    + "<div class=\"task-archive-meta\">"
                    + "<span>" + typeLabel(task.taskType) + "</span>"
                    + "<span>예상 " + (task.estimatedMinutes || 0) + "분</span>"
                    + "<span>중요도 " + (task.importance || 0) + "</span>"
                    + "<span>" + escapeHtml(detailText(task)) + "</span>"
                    + "</div>";
            refs.list.appendChild(item);
        });
    }

    function sortTasks(tasks) {
        return tasks.slice().sort(function (a, b) {
            if (a.taskType === "DAILY_GOAL") {
                return (b.currentStreak || 0) - (a.currentStreak || 0);
            }
            return dateValue(a.deadlineAt) - dateValue(b.deadlineAt);
        });
    }

    function detailText(task) {
        if (task.taskType === "DAILY_GOAL") {
            return "연속 " + (task.currentStreak || 0) + "일 · 하루 목표 " + (task.targetCountPerDay || 1) + "회";
        }
        return "마감 " + formatDateTime(task.deadlineAt);
    }

    async function readJson(response) {
        const text = await response.text();
        if (!text) {
            return null;
        }
        try {
            return JSON.parse(text);
        } catch (error) {
            return text;
        }
    }

    function authHeaders() {
        return {
            "Authorization": "Bearer " + user.token
        };
    }

    function isUnauthorized(result) {
        return result.status === 401;
    }

    function redirectToLogin() {
        localStorage.removeItem(storageKey);
        window.location.href = "/login";
    }

    function readStoredUser() {
        try {
            return JSON.parse(localStorage.getItem(storageKey) || "{}");
        } catch (error) {
            return {};
        }
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
        return value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER;
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

    function typeLabel(value) {
        return value === "DAILY_GOAL" ? "매일 목표" : "마감 업무";
    }

    function statusLabel(value) {
        const labels = {
            TODO: "대기",
            IN_PROGRESS: "진행 중",
            DONE: "완료"
        };
        return labels[value] || value || "상태 없음";
    }

    function formatError(body) {
        if (typeof body === "string") {
            return body;
        }
        if (body && Array.isArray(body.details) && body.details.length > 0) {
            return body.details.join(" / ");
        }
        return body && body.message ? body.message : "목록을 불러오지 못했습니다.";
    }

    function escapeHtml(value) {
        return String(value)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#039;");
    }

    function setMessage(text, isError) {
        refs.message.textContent = text || "";
        refs.message.className = isError ? "load-message error" : "load-message";
    }
})();
