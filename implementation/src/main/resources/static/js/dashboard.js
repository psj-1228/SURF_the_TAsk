(function () {
    const storageKey = "surfUser";
    const user = readStoredUser();

    if (!user.token) {
        window.location.href = "/login";
        return;
    }

    const refs = {
        dashboard: document.querySelector("[data-dashboard]"),
        userName: document.querySelector("[data-user-name]"),
        todayLabel: document.querySelector("[data-today-label]"),
        logout: document.querySelector("[data-logout]"),
        loadMessage: document.querySelector("[data-load-message]"),
        dailyGoalForm: document.querySelector("[data-daily-goal-form]"),
        deadlineTaskForm: document.querySelector("[data-deadline-task-form]"),
        completionRate: document.querySelector("[data-completion-rate]"),
        waveRate: document.querySelector("[data-wave-rate]"),
        waveVisual: document.querySelector("[data-wave-visual]"),
        waveNote: document.querySelector("[data-wave-note]"),
        doneTasks: document.querySelector("[data-done-tasks]"),
        totalTasks: document.querySelector("[data-total-tasks]"),
        incompleteTasks: document.querySelector("[data-incomplete-tasks]"),
        bestStreak: document.querySelector("[data-best-streak]"),
        dailyCount: document.querySelector("[data-daily-count]"),
        deadlineCount: document.querySelector("[data-deadline-count]"),
        availabilityCount: document.querySelector("[data-availability-count]"),
        reminderCount: document.querySelector("[data-reminder-count]"),
        priorityList: document.querySelector("[data-priority-list]"),
        dailyList: document.querySelector("[data-daily-list]"),
        deadlineList: document.querySelector("[data-deadline-list]"),
        availabilityList: document.querySelector("[data-availability-list]"),
        reminderList: document.querySelector("[data-reminder-list]")
    };

    refs.userName.textContent = user.name || "User";
    refs.todayLabel.textContent = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long"
    }).format(new Date());

    setDefaultDeadline();
    refs.logout.addEventListener("click", logout);
    refs.dailyGoalForm.addEventListener("submit", handleDailyGoalCreate);
    refs.deadlineTaskForm.addEventListener("submit", handleDeadlineTaskCreate);
    refs.dashboard.addEventListener("click", handleTaskClick);
    refs.dashboard.addEventListener("submit", handleTaskEdit);

    loadDashboard();

    async function loadDashboard() {
        setMessage("대시보드 데이터를 불러오는 중입니다.");

        const [progressResult, tasksResult, availabilityResult, remindersResult] = await Promise.all([
            fetchJson("/api/progress"),
            fetchJson("/api/tasks"),
            fetchJson("/api/availability"),
            fetchJson("/api/reminders")
        ]);

        if ([progressResult, tasksResult, availabilityResult, remindersResult].some(isUnauthorized)) {
            redirectToLogin();
            return;
        }

        const tasks = tasksResult.ok ? tasksResult.data : [];
        const taskList = asArray(tasks);
        const progress = progressResult.ok ? progressResult.data : fallbackProgress(taskList);
        const availability = availabilityResult.ok ? asArray(availabilityResult.data) : [];
        const reminders = remindersResult.ok ? asArray(remindersResult.data) : [];

        renderProgress(progress);
        renderTasks(taskList, asArray(progress.priorityTasks));
        renderAvailability(availability);
        renderReminders(reminders);

        const failed = [progressResult, tasksResult, availabilityResult, remindersResult].filter(function (result) {
            return !result.ok;
        });
        setMessage(failed.length > 0 ? "일부 데이터를 불러오지 못했습니다." : "");
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

    async function handleDailyGoalCreate(event) {
        event.preventDefault();
        if (!refs.dailyGoalForm.reportValidity()) {
            return;
        }

        const payload = readTaskForm(refs.dailyGoalForm);
        payload.targetCountPerDay = numberValue(refs.dailyGoalForm.elements.targetCountPerDay.value);
        await submitTaskForm(refs.dailyGoalForm, "/api/tasks/daily-goals", payload, "Daily Goal을 추가했습니다.");
    }

    async function handleDeadlineTaskCreate(event) {
        event.preventDefault();
        if (!refs.deadlineTaskForm.reportValidity()) {
            return;
        }

        const payload = readTaskForm(refs.deadlineTaskForm);
        payload.deadlineAt = refs.deadlineTaskForm.elements.deadlineAt.value;
        payload.warningThresholdHours = numberValue(refs.deadlineTaskForm.elements.warningThresholdHours.value);
        await submitTaskForm(refs.deadlineTaskForm, "/api/tasks/deadline-tasks", payload, "Deadline Task를 추가했습니다.");
    }

    async function submitTaskForm(form, url, payload, successMessage) {
        setFormBusy(form, true);
        const result = await sendJson(url, "POST", payload);
        setFormBusy(form, false);

        if (!result.ok) {
            setMessage(formatError(result.data), true);
            return;
        }

        form.reset();
        setDefaultDeadline();
        setMessage(successMessage);
        await loadDashboard();
    }

    async function handleTaskEdit(event) {
        const form = event.target.closest("[data-edit-form]");
        if (!form) {
            return;
        }

        event.preventDefault();
        if (!form.reportValidity()) {
            return;
        }

        const taskId = form.dataset.taskId;
        const taskType = form.dataset.taskType;
        const payload = readTaskForm(form);
        payload.status = form.elements.status.value;

        if (taskType === "DAILY_GOAL") {
            payload.targetCountPerDay = numberValue(form.elements.targetCountPerDay.value);
            payload.deadlineAt = null;
            payload.warningThresholdHours = null;
        } else {
            payload.targetCountPerDay = null;
            payload.deadlineAt = form.elements.deadlineAt.value;
            payload.warningThresholdHours = numberValue(form.elements.warningThresholdHours.value);
        }

        setFormBusy(form, true);
        const result = await sendJson("/api/tasks/" + encodeURIComponent(taskId), "PUT", payload);
        setFormBusy(form, false);

        if (!result.ok) {
            setMessage(formatError(result.data), true);
            return;
        }

        setMessage("Task를 수정했습니다.");
        await loadDashboard();
    }

    async function handleTaskClick(event) {
        const action = event.target.closest("[data-task-action]");
        if (!action) {
            return;
        }

        const item = action.closest("[data-task-id]");
        if (!item) {
            return;
        }

        const taskId = item.dataset.taskId;
        const taskTitle = item.dataset.taskTitle || "Task";

        if (action.dataset.taskAction === "edit") {
            item.classList.add("is-editing");
            return;
        }

        if (action.dataset.taskAction === "cancel-edit") {
            item.classList.remove("is-editing");
            return;
        }

        if (action.dataset.taskAction === "complete") {
            await completeTask(taskId);
            return;
        }

        if (action.dataset.taskAction === "delete") {
            if (window.confirm("'" + taskTitle + "'을 삭제할까요?")) {
                await deleteTask(taskId);
            }
        }
    }

    async function completeTask(taskId) {
        const result = await sendJson("/api/tasks/" + encodeURIComponent(taskId) + "/completion", "POST", {
            cancel: false
        });

        if (!result.ok) {
            setMessage(formatError(result.data), true);
            return;
        }

        setMessage("완료 체크했습니다.");
        await loadDashboard();
    }

    async function deleteTask(taskId) {
        const result = await sendJson("/api/tasks/" + encodeURIComponent(taskId), "DELETE");

        if (!result.ok) {
            setMessage(formatError(result.data), true);
            return;
        }

        setMessage("Task를 삭제했습니다.");
        await loadDashboard();
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

    async function sendJson(url, method, payload) {
        try {
            const options = {
                method: method,
                headers: jsonHeaders()
            };

            if (payload !== undefined) {
                options.body = JSON.stringify(payload);
            }

            const response = await fetch(url, options);
            const data = await readJson(response);

            if (response.status === 401) {
                redirectToLogin();
                return { ok: false, status: 401, data: data };
            }

            return { ok: response.ok, status: response.status, data: data };
        } catch (error) {
            return { ok: false, status: 0, data: null };
        }
    }

    function authHeaders() {
        return {
            "Authorization": "Bearer " + user.token
        };
    }

    function jsonHeaders() {
        return {
            "Authorization": "Bearer " + user.token,
            "Content-Type": "application/json"
        };
    }

    function readJson(response) {
        return response.text().then(function (text) {
            return text ? JSON.parse(text) : null;
        });
    }

    function isUnauthorized(result) {
        return result.status === 401;
    }

    function redirectToLogin() {
        localStorage.removeItem(storageKey);
        window.location.href = "/login";
    }

    function readTaskForm(form) {
        return {
            title: form.elements.title.value.trim(),
            description: form.elements.description.value.trim(),
            estimatedMinutes: numberValue(form.elements.estimatedMinutes.value),
            importance: numberValue(form.elements.importance.value)
        };
    }

    function renderProgress(progress) {
        const rate = clamp(Math.round(progress.completionRate || 0), 0, 100);
        refs.completionRate.textContent = rate + "%";
        refs.waveRate.textContent = rate + "%";
        refs.waveVisual.style.setProperty("--wave-level", Math.max(rate, 12) + "%");
        refs.waveNote.textContent = rate >= 80
                ? "진행도가 좋습니다. 이 흐름을 유지하세요."
                : "완료 체크가 쌓이면 진행도 파도가 높아집니다.";
        refs.doneTasks.textContent = progress.doneTasks || 0;
        refs.totalTasks.textContent = "전체 " + (progress.totalTasks || 0) + "개";
        refs.incompleteTasks.textContent = progress.incompleteTasks || 0;
        refs.bestStreak.textContent = (progress.bestDailyGoalStreak || 0) + "일";
    }

    function renderTasks(tasks, priorityTasks) {
        const dailyGoals = tasks.filter(function (task) {
            return task.taskType === "DAILY_GOAL";
        }).sort(function (a, b) {
            return (b.currentStreak || 0) - (a.currentStreak || 0);
        });
        const deadlineTasks = tasks.filter(function (task) {
            return task.taskType === "DEADLINE_TASK";
        }).sort(function (a, b) {
            return dateValue(a.deadlineAt) - dateValue(b.deadlineAt);
        });
        const priority = priorityTasks.length > 0 ? priorityTasks : tasks
                .filter(function (task) { return task.status !== "DONE"; })
                .sort(function (a, b) {
                    return priorityScore(b) - priorityScore(a);
                })
                .slice(0, 5);

        refs.dailyCount.textContent = dailyGoals.length;
        refs.deadlineCount.textContent = deadlineTasks.length;

        renderTaskList(refs.dailyList, dailyGoals, "아직 Daily Goal이 없습니다.");
        renderTaskList(refs.deadlineList, deadlineTasks, "아직 Deadline Task가 없습니다.");
        renderPriorityList(priority);
    }

    function renderTaskList(container, tasks, emptyText) {
        container.innerHTML = "";

        if (tasks.length === 0) {
            container.appendChild(emptyState(emptyText));
            return;
        }

        tasks.forEach(function (task) {
            container.appendChild(taskCard(task));
        });
    }

    function renderPriorityList(tasks) {
        refs.priorityList.innerHTML = "";

        if (tasks.length === 0) {
            refs.priorityList.appendChild(emptyState("미완료 우선순위 Task가 없습니다."));
            return;
        }

        tasks.slice(0, 5).forEach(function (task, index) {
            const item = document.createElement("article");
            item.className = "task-item priority-item";
            item.innerHTML = "<span class=\"rank\">" + (index + 1) + "</span>" + taskMarkup(task)
                    + "<span class=\"task-score\">" + priorityScore(task) + "</span>";
            refs.priorityList.appendChild(item);
        });
    }

    function taskCard(task) {
        const item = document.createElement("article");
        item.className = "task-item editable-task";
        item.dataset.taskId = task.taskId;
        item.dataset.taskTitle = task.title || "Task";
        item.innerHTML = "<div class=\"task-view\">"
                + taskMarkup(task)
                + taskActions(task)
                + "</div>"
                + editFormMarkup(task);
        return item;
    }

    function taskMarkup(task) {
        const type = task.taskType === "DAILY_GOAL" ? "Daily Goal" : "Deadline Task";
        const time = task.estimatedMinutes ? task.estimatedMinutes + "분" : "시간 미설정";
        const subtype = task.taskType === "DAILY_GOAL"
                ? "Streak " + (task.currentStreak || 0) + "일"
                : "마감 " + formatDateTime(task.deadlineAt);
        const detail = task.description || (task.taskType === "DAILY_GOAL" ? "하루 목표 " + (task.targetCountPerDay || 1) + "회" : subtype);

        return "<div class=\"task-main\">"
                + "<strong>" + escapeHtml(task.title || "제목 없음") + "</strong>"
                + "<small>" + escapeHtml(detail) + "</small>"
                + "<div class=\"task-meta\">"
                + "<span>" + type + "</span>"
                + "<span>" + statusLabel(task.status) + "</span>"
                + "<span>" + time + "</span>"
                + "<span>중요도 " + (task.importance || 0) + "</span>"
                + "<span>" + escapeHtml(subtype) + "</span>"
                + "</div>"
                + "</div>";
    }

    function taskActions(task) {
        const completeButton = task.status === "DONE" ? "" :
                "<button class=\"small-button success\" type=\"button\" data-task-action=\"complete\">완료</button>";

        return "<div class=\"task-actions\">"
                + completeButton
                + "<button class=\"small-button\" type=\"button\" data-task-action=\"edit\">수정</button>"
                + "<button class=\"small-button danger\" type=\"button\" data-task-action=\"delete\">삭제</button>"
                + "</div>";
    }

    function editFormMarkup(task) {
        const isDaily = task.taskType === "DAILY_GOAL";
        const subtypeFields = isDaily ? dailyEditFields(task) : deadlineEditFields(task);

        return "<form class=\"task-edit-form\" data-edit-form data-task-id=\"" + task.taskId
                + "\" data-task-type=\"" + task.taskType + "\">"
                + "<label><span>제목</span><input name=\"title\" type=\"text\" maxlength=\"100\" value=\""
                + escapeHtml(task.title || "") + "\" required></label>"
                + "<label><span>설명</span><textarea name=\"description\" rows=\"2\">"
                + escapeHtml(task.description || "") + "</textarea></label>"
                + "<div class=\"form-row\">"
                + "<label><span>예상 시간</span><input name=\"estimatedMinutes\" type=\"number\" min=\"1\" value=\""
                + (task.estimatedMinutes || 1) + "\" required></label>"
                + "<label><span>중요도</span><input name=\"importance\" type=\"number\" min=\"1\" max=\"5\" value=\""
                + (task.importance || 3) + "\" required></label>"
                + "<label><span>상태</span><select name=\"status\">"
                + statusOption("TODO", task.status)
                + statusOption("IN_PROGRESS", task.status)
                + statusOption("DONE", task.status)
                + "</select></label>"
                + "</div>"
                + subtypeFields
                + "<div class=\"task-actions\">"
                + "<button class=\"small-button success\" type=\"submit\">저장</button>"
                + "<button class=\"small-button\" type=\"button\" data-task-action=\"cancel-edit\">취소</button>"
                + "</div>"
                + "</form>";
    }

    function dailyEditFields(task) {
        return "<div class=\"form-row\">"
                + "<label><span>하루 목표</span><input name=\"targetCountPerDay\" type=\"number\" min=\"1\" value=\""
                + (task.targetCountPerDay || 1) + "\" required></label>"
                + "</div>";
    }

    function deadlineEditFields(task) {
        return "<div class=\"form-row\">"
                + "<label><span>마감</span><input name=\"deadlineAt\" type=\"datetime-local\" value=\""
                + toDateTimeLocal(task.deadlineAt) + "\" required></label>"
                + "<label><span>경고 시간</span><input name=\"warningThresholdHours\" type=\"number\" min=\"1\" value=\""
                + (task.warningThresholdHours || 24) + "\" required></label>"
                + "</div>";
    }

    function statusOption(value, selected) {
        return "<option value=\"" + value + "\"" + (value === selected ? " selected" : "") + ">"
                + statusLabel(value) + "</option>";
    }

    function renderAvailability(slots) {
        refs.availabilityCount.textContent = slots.length;
        refs.availabilityList.innerHTML = "";

        if (slots.length === 0) {
            refs.availabilityList.appendChild(emptyState("계산된 가능 시간이 없습니다."));
            return;
        }

        slots.slice(0, 6).forEach(function (slot) {
            const item = document.createElement("article");
            item.className = "slot-item";
            item.innerHTML = "<strong>" + dayName(slot.dayOfWeek) + "</strong>"
                    + "<span>" + trimTime(slot.startTime) + " - " + trimTime(slot.endTime)
                    + " | " + slot.durationMinutes + "분</span>";
            refs.availabilityList.appendChild(item);
        });
    }

    function renderReminders(reminders) {
        refs.reminderCount.textContent = reminders.length;
        refs.reminderList.innerHTML = "";

        if (reminders.length === 0) {
            refs.reminderList.appendChild(emptyState("아직 알림이 없습니다."));
            return;
        }

        reminders.slice(0, 5).forEach(function (reminder) {
            const item = document.createElement("article");
            item.className = "reminder-item";
            item.innerHTML = "<strong>" + reminderLabel(reminder.reminderType) + "</strong>"
                    + "<span>" + escapeHtml(reminder.message || "알림") + "</span>"
                    + "<span>" + reminder.status + " | " + formatDateTime(reminder.sentAt || reminder.scheduledAt) + "</span>";
            refs.reminderList.appendChild(item);
        });
    }

    function fallbackProgress(tasks) {
        const done = asArray(tasks).filter(function (task) { return task.status === "DONE"; }).length;
        return {
            totalTasks: asArray(tasks).length,
            doneTasks: done,
            incompleteTasks: asArray(tasks).length - done,
            completionRate: asArray(tasks).length === 0 ? 0 : done / asArray(tasks).length * 100,
            bestDailyGoalStreak: 0,
            priorityTasks: []
        };
    }

    function priorityScore(task) {
        const importance = task.importance || 0;
        const deadlineBonus = task.deadlineAt ? 10 : 0;
        return importance * 10 + deadlineBonus;
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

    function readStoredUser() {
        try {
            return JSON.parse(localStorage.getItem(storageKey) || "{}");
        } catch (error) {
            return {};
        }
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

    function toDateTimeLocal(value) {
        if (!value) {
            return "";
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return "";
        }
        const offset = date.getTimezoneOffset() * 60000;
        return new Date(date.getTime() - offset).toISOString().slice(0, 16);
    }

    function setDefaultDeadline() {
        if (!refs.deadlineTaskForm || !refs.deadlineTaskForm.elements.deadlineAt) {
            return;
        }
        const input = refs.deadlineTaskForm.elements.deadlineAt;
        if (!input.value) {
            const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
            const offset = tomorrow.getTimezoneOffset() * 60000;
            input.value = new Date(tomorrow.getTime() - offset).toISOString().slice(0, 16);
        }
    }

    function trimTime(value) {
        return value ? value.slice(0, 5) : "--:--";
    }

    function dayName(value) {
        const days = {
            MONDAY: "월요일",
            TUESDAY: "화요일",
            WEDNESDAY: "수요일",
            THURSDAY: "목요일",
            FRIDAY: "금요일",
            SATURDAY: "토요일",
            SUNDAY: "일요일"
        };
        return days[value] || value || "요일 미설정";
    }

    function reminderLabel(value) {
        const labels = {
            AVAILABILITY_BASED: "가능 시간",
            DEADLINE_WARNING: "마감 경고",
            OVERDUE_ALERT: "기한 초과",
            DELAYED_IN_SITE: "Focus 확인"
        };
        return labels[value] || "알림";
    }

    function statusLabel(value) {
        const labels = {
            TODO: "대기",
            IN_PROGRESS: "진행 중",
            DONE: "완료"
        };
        return labels[value] || value || "상태 없음";
    }

    function numberValue(value) {
        return Number.parseInt(value, 10);
    }

    function formatError(body) {
        if (body && Array.isArray(body.details) && body.details.length > 0) {
            return body.details.join(" / ");
        }
        return body && body.message ? body.message : "요청을 처리하지 못했습니다.";
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

    function setFormBusy(form, isBusy) {
        form.querySelectorAll("button").forEach(function (button) {
            button.disabled = isBusy;
        });
    }

    function setMessage(text, isError) {
        refs.loadMessage.textContent = text || "";
        refs.loadMessage.className = isError ? "load-message error" : "load-message";
    }
})();
