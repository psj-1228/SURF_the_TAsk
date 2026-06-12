(function () {
    const storageKey = "surfUser";
    const user = readStoredUser();
    let lastFocusedElement = null;
    const displayedReminderStorageKey = "surfDisplayedReminderIds";
    const reminderPollIntervalMs = 30000;
    const initialReminderReplayWindowMs = 30 * 60 * 1000;
    let displayedReminderIds = readDisplayedReminderIds();
    let hasLoadedInitialReminders = false;
    let reminderPollTimer = null;

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
        reminderToastRegion: document.querySelector("[data-reminder-toast-region]"),
        taskModal: document.querySelector("[data-task-modal]"),
        taskCreateForm: document.querySelector("[data-task-create-form]"),
        taskModalTitle: document.querySelector("[data-task-modal-title]"),
        taskModalEyebrow: document.querySelector("[data-task-modal-eyebrow]"),
        taskModalMessage: document.querySelector("[data-task-modal-message]"),
        taskModalSubmit: document.querySelector("[data-task-modal-submit]"),
        dailyFields: document.querySelector("[data-daily-fields]"),
        deadlineFields: document.querySelector("[data-deadline-fields]"),
        completionRate: document.querySelector("[data-completion-rate]"),
        achievementNote: document.querySelector("[data-achievement-note]"),
        todayDailyGoalCount: document.querySelector("[data-today-daily-goal-count]"),
        todayDeadlineTaskCount: document.querySelector("[data-today-deadline-task-count]"),
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

    refs.logout.addEventListener("click", logout);
    refs.taskCreateForm.addEventListener("submit", handleTaskCreate);
    refs.taskModal.addEventListener("click", handleModalBackdropClick);
    document.querySelectorAll("[data-close-task-modal]").forEach(function (button) {
        button.addEventListener("click", closeTaskModal);
    });
    document.querySelectorAll("[data-open-task-modal]").forEach(function (button) {
        button.addEventListener("click", function () {
            openTaskModal(button.dataset.openTaskModal);
        });
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !refs.taskModal.hidden) {
            closeTaskModal();
        }
    });
    refs.dashboard.addEventListener("click", handleTaskClick);
    refs.dashboard.addEventListener("submit", handleTaskEdit);

    loadDashboard().then(startReminderPolling);

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
        if (remindersResult.ok) {
            handleInSiteReminderDisplay(reminders);
        }

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

    function openTaskModal(kind) {
        const isDaily = kind === "daily";
        const form = refs.taskCreateForm;

        lastFocusedElement = document.activeElement;
        form.reset();
        clearTaskModalMessage();
        form.dataset.taskType = isDaily ? "DAILY_GOAL" : "DEADLINE_TASK";
        refs.taskModalEyebrow.textContent = isDaily ? "Daily Goal" : "Task";
        refs.taskModalTitle.textContent = isDaily ? "Daily Goal 추가" : "Task 추가";
        refs.taskModalSubmit.textContent = isDaily ? "Daily Goal 추가" : "Task 추가";

        form.elements.estimatedMinutes.value = isDaily ? "30" : "60";
        form.elements.importance.value = isDaily ? "3" : "4";
        form.elements.targetCountPerDay.required = isDaily;
        form.elements.deadlineAt.required = !isDaily;
        form.elements.warningThresholdHours.required = !isDaily;
        refs.dailyFields.hidden = !isDaily;
        refs.deadlineFields.hidden = isDaily;

        if (!isDaily) {
            setDefaultDeadline();
        }

        refs.taskModal.hidden = false;
        document.body.classList.add("modal-open");
        window.setTimeout(function () {
            form.elements.title.focus();
        }, 0);
    }

    function closeTaskModal() {
        refs.taskModal.hidden = true;
        refs.taskCreateForm.reset();
        clearTaskModalMessage();
        document.body.classList.remove("modal-open");

        if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
            lastFocusedElement.focus();
        }
    }

    function handleModalBackdropClick(event) {
        if (event.target === refs.taskModal) {
            closeTaskModal();
        }
    }

    async function handleTaskCreate(event) {
        event.preventDefault();

        const form = refs.taskCreateForm;
        const isDaily = form.dataset.taskType === "DAILY_GOAL";

        if (!form.reportValidity()) {
            return;
        }

        const payload = readTaskForm(form);
        const url = isDaily ? "/api/tasks/daily-goals" : "/api/tasks/deadline-tasks";
        const successMessage = isDaily ? "Daily Goal을 추가했습니다." : "Task를 추가했습니다.";

        if (isDaily) {
            payload.targetCountPerDay = numberValue(form.elements.targetCountPerDay.value);
        } else {
            payload.deadlineAt = form.elements.deadlineAt.value;
            payload.warningThresholdHours = numberValue(form.elements.warningThresholdHours.value);
        }

        setFormBusy(form, true);
        const result = await sendJson(url, "POST", payload);
        setFormBusy(form, false);

        if (!result.ok) {
            setTaskModalMessage(formatError(result.data), true);
            return;
        }

        closeTaskModal();
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
        const todayDailyGoals = numberOrZero(progress.todayCompletedDailyGoals);
        const todayDeadlineTasks = numberOrZero(progress.todayCompletedDeadlineTasks);
        const todayCompletedTasks = numberOr(progress.todayCompletedTasks, todayDailyGoals + todayDeadlineTasks);
        refs.completionRate.textContent = rate + "%";
        refs.todayDailyGoalCount.textContent = todayDailyGoals;
        refs.todayDeadlineTaskCount.textContent = todayDeadlineTasks;
        refs.achievementNote.textContent = todayCompletedTasks > 0
                ? "오늘 Daily Goal " + todayDailyGoals + "개와 Task " + todayDeadlineTasks + "개를 완료했어요."
                : "오늘 완료한 Daily Goal과 Task가 아직 없습니다.";
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
        renderTaskList(refs.deadlineList, deadlineTasks, "아직 Task가 없습니다.");
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
        const type = task.taskType === "DAILY_GOAL" ? "Daily Goal" : "Task";
        const time = task.estimatedMinutes ? task.estimatedMinutes + "분" : "시간 미설정";
        const subtype = task.taskType === "DAILY_GOAL"
                ? "Streak " + (task.currentStreak || 0) + "일"
                : "마감 " + formatDateTime(task.deadlineAt);
        const detail = task.description || (task.taskType === "DAILY_GOAL"
                ? "하루 목표 " + (task.targetCountPerDay || 1) + "회"
                : subtype);

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
        const subtypeFields = task.taskType === "DAILY_GOAL" ? dailyEditFields(task) : deadlineEditFields(task);

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

    function startReminderPolling() {
        if (reminderPollTimer !== null) {
            return;
        }

        reminderPollTimer = window.setInterval(pollReminders, reminderPollIntervalMs);
    }

    async function pollReminders() {
        const result = await fetchJson("/api/reminders");

        if (isUnauthorized(result)) {
            redirectToLogin();
            return;
        }

        if (!result.ok) {
            return;
        }

        const reminders = asArray(result.data);
        renderReminders(reminders);
        handleInSiteReminderDisplay(reminders);
    }

    function handleInSiteReminderDisplay(reminders) {
        const popupReminders = asArray(reminders).filter(isPopupReminder);

        if (!hasLoadedInitialReminders) {
            popupReminders.forEach(function (reminder) {
                const reminderId = reminder.reminderId;

                if (reminderId === null || reminderId === undefined) {
                    return;
                }

                if (!displayedReminderIds.includes(String(reminderId))
                        && isRecentReminder(reminder, initialReminderReplayWindowMs)) {
                    showReminderToast(reminder);
                }

                rememberDisplayedReminder(reminderId);
            });
            hasLoadedInitialReminders = true;
            return;
        }

        popupReminders.forEach(function (reminder) {
            const reminderId = reminder.reminderId;

            if (reminderId === null || reminderId === undefined) {
                return;
            }

            if (displayedReminderIds.includes(String(reminderId))) {
                return;
            }

            showReminderToast(reminder);
            rememberDisplayedReminder(reminderId);
        });
    }

    function isPopupReminder(reminder) {
        return reminder && reminder.status === "SENT"
                && (reminder.channel === "IN_SITE" || isDeadlinePopupReminderType(reminder.reminderType));
    }

    function isDeadlinePopupReminderType(value) {
        return value === "DEADLINE_ONE_HOUR"
                || value === "DEADLINE_THIRTY_MINUTES"
                || value === "DAILY_GOAL_DAY_END_ONE_HOUR"
                || value === "DAILY_GOAL_DAY_END_THIRTY_MINUTES";
    }

    function showReminderToast(reminder) {
        if (!refs.reminderToastRegion) {
            return;
        }

        const toast = document.createElement("article");
        const content = document.createElement("div");
        const title = document.createElement("strong");
        const message = document.createElement("span");
        const time = document.createElement("small");
        const dismiss = document.createElement("button");

        toast.className = "reminder-toast";
        toast.setAttribute("role", "status");
        content.className = "reminder-toast-content";
        title.textContent = reminderLabel(reminder.reminderType);
        message.textContent = reminder.message || "Reminder";
        time.textContent = formatDateTime(reminder.sentAt || reminder.scheduledAt);
        dismiss.className = "reminder-toast-dismiss";
        dismiss.type = "button";
        dismiss.setAttribute("aria-label", "Dismiss reminder");
        dismiss.textContent = "x";

        content.appendChild(title);
        content.appendChild(message);
        content.appendChild(time);
        toast.appendChild(content);
        toast.appendChild(dismiss);
        refs.reminderToastRegion.appendChild(toast);

        const removeToast = function () {
            toast.remove();
        };

        dismiss.addEventListener("click", removeToast);
        window.setTimeout(removeToast, 10000);
    }

    function rememberDisplayedReminder(reminderId) {
        if (reminderId === null || reminderId === undefined) {
            return;
        }

        const normalizedId = String(reminderId);
        displayedReminderIds = displayedReminderIds.filter(function (storedId) {
            return storedId !== normalizedId;
        });
        displayedReminderIds.push(normalizedId);
        const persistedReminderIds = displayedReminderIds.slice(-50);

        try {
            localStorage.setItem(displayedReminderStorageKey, JSON.stringify(persistedReminderIds));
        } catch (error) {
            // Ignore storage errors; the in-memory list still prevents duplicate toasts this session.
        }
    }

    function readDisplayedReminderIds() {
        try {
            const storedIds = JSON.parse(localStorage.getItem(displayedReminderStorageKey) || "[]");

            if (!Array.isArray(storedIds)) {
                return [];
            }

            return storedIds.filter(function (storedId) {
                return storedId !== null && storedId !== undefined;
            }).map(String).slice(-50);
        } catch (error) {
            return [];
        }
    }

    function isRecentReminder(reminder, windowMs) {
        const timestamp = dateValue(reminder.sentAt || reminder.scheduledAt);
        const ageMs = Date.now() - timestamp;
        return Number.isFinite(timestamp) && ageMs >= 0 && ageMs <= windowMs;
    }

    function fallbackProgress(tasks) {
        const taskList = asArray(tasks);
        const done = taskList.filter(function (task) { return task.status === "DONE"; }).length;
        const doneDailyGoals = taskList.filter(function (task) {
            return task.taskType === "DAILY_GOAL" && task.status === "DONE";
        }).length;
        const doneDeadlineTasks = taskList.filter(function (task) {
            return task.taskType === "DEADLINE_TASK" && task.status === "DONE";
        }).length;
        return {
            totalTasks: taskList.length,
            doneTasks: done,
            incompleteTasks: taskList.length - done,
            completionRate: taskList.length === 0 ? 0 : done / taskList.length * 100,
            bestDailyGoalStreak: 0,
            priorityTasks: [],
            todayCompletedDailyGoals: doneDailyGoals,
            todayCompletedDeadlineTasks: doneDeadlineTasks,
            todayCompletedTasks: done,
            todayCompletionRate: taskList.length === 0 ? 0 : done / taskList.length * 100
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
        const form = refs.taskCreateForm;
        if (!form || !form.elements.deadlineAt) {
            return;
        }
        const input = form.elements.deadlineAt;
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
            DELAYED_IN_SITE: "Focus 확인",
            DAILY_GOAL_DAY_END_ONE_HOUR: "Daily Goal 마감 1시간 전",
            DAILY_GOAL_DAY_END_THIRTY_MINUTES: "Daily Goal 마감 30분 전",
            DEADLINE_ONE_HOUR: "Task 마감 1시간 전",
            DEADLINE_THIRTY_MINUTES: "Task 마감 30분 전"
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

    function numberOr(value, fallback) {
        return Number.isFinite(Number(value)) ? Number(value) : fallback;
    }

    function numberOrZero(value) {
        return numberOr(value, 0);
    }

    function formatError(body) {
        if (typeof body === "string") {
            return body;
        }
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

    function setTaskModalMessage(text, isError) {
        refs.taskModalMessage.textContent = text || "";
        refs.taskModalMessage.className = isError ? "form-message error" : "form-message";
    }

    function clearTaskModalMessage() {
        setTaskModalMessage("");
    }
})();
