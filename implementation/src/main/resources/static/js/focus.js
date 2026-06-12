(function () {
    const storageKey = "surfUser";
    const user = readStoredUser();
    const defaultDurationSeconds = 25 * 60;

    if (!user.token) {
        window.location.href = "/login";
        return;
    }

    const refs = {
        date: document.querySelector("[data-focus-date]"),
        logout: document.querySelector("[data-logout]"),
        taskSelect: document.querySelector("[data-task-select]"),
        startFocus: document.querySelector("[data-start-focus]"),
        togglePause: document.querySelector("[data-toggle-pause]"),
        openExitConfirm: document.querySelector("[data-open-exit-confirm]"),
        exitConfirmModal: document.querySelector("[data-exit-confirm-modal]"),
        confirmExit: document.querySelector("[data-confirm-exit]"),
        keepFocusButtons: document.querySelectorAll("[data-keep-focus]"),
        completeWrap: document.querySelector("[data-complete-wrap]"),
        completeTask: document.querySelector("[data-complete-task]"),
        focusState: document.querySelector("[data-focus-state]"),
        focusTaskTitle: document.querySelector("[data-focus-task-title]"),
        focusTaskDetail: document.querySelector("[data-focus-task-detail]"),
        focusTimer: document.querySelector("[data-focus-timer]"),
        remainingLabel: document.querySelector("[data-remaining-label]"),
        progressRing: document.querySelector("[data-progress-ring]"),
        sessionKind: document.querySelector("[data-session-kind]"),
        elapsedTime: document.querySelector("[data-elapsed-time]"),
        sessionTarget: document.querySelector("[data-session-target]"),
        message: document.querySelector("[data-focus-message]")
    };

    const state = {
        tasks: [],
        selectedTask: null,
        sessionId: null,
        backendSession: false,
        active: false,
        paused: false,
        durationSeconds: defaultDurationSeconds,
        remainingSeconds: defaultDurationSeconds,
        elapsedSeconds: 0,
        tickId: null
    };

    refs.date.textContent = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long"
    }).format(new Date());

    refs.logout.addEventListener("click", logout);
    refs.taskSelect.addEventListener("change", handleTaskSelection);
    refs.startFocus.addEventListener("click", startFocusSession);
    refs.togglePause.addEventListener("click", togglePause);
    refs.openExitConfirm.addEventListener("click", openExitConfirm);
    refs.confirmExit.addEventListener("click", confirmExit);
    refs.exitConfirmModal.addEventListener("click", handleModalBackdropClick);
    refs.keepFocusButtons.forEach(function (button) {
        button.addEventListener("click", keepFocus);
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !refs.exitConfirmModal.hidden) {
            keepFocus();
        }
    });

    renderIdle();
    loadTasks();

    async function loadTasks() {
        setMessage("집중할 과제를 불러오는 중입니다.");
        const result = await fetchJson("/api/tasks/incomplete");

        if (isUnauthorized(result)) {
            redirectToLogin();
            return;
        }

        state.tasks = result.ok ? asArray(result.data) : [];
        renderTaskOptions();
        selectFirstTaskIfAvailable();
        handleTaskSelection();
        setMessage(result.ok ? "과제를 선택하거나 자유 항해로 시작하세요." : "과제 목록을 불러오지 못했습니다. 자유 항해는 바로 시작할 수 있습니다.", !result.ok);
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

    function renderTaskOptions() {
        refs.taskSelect.innerHTML = "";
        refs.taskSelect.appendChild(option("", "과제 선택 안 함 - 자유 항해"));

        state.tasks
                .filter(function (task) { return task.status !== "DONE"; })
                .sort(compareTasks)
                .forEach(function (task) {
                    refs.taskSelect.appendChild(option(String(task.taskId), taskOptionLabel(task)));
        });
    }

    function selectFirstTaskIfAvailable() {
        const firstTask = state.tasks
                .filter(function (task) { return task.status !== "DONE"; })
                .sort(compareTasks)[0];

        if (firstTask) {
            refs.taskSelect.value = String(firstTask.taskId);
            state.remainingSeconds = state.durationSeconds;
        }
    }

    function handleTaskSelection() {
        const taskId = refs.taskSelect.value;
        state.selectedTask = state.tasks.find(function (task) {
            return String(task.taskId) === taskId;
        }) || null;

        if (!state.active) {
            renderIdle();
        }
    }

    async function startFocusSession() {
        if (state.active) {
            setMessage("이미 집중 항해가 진행 중입니다.");
            return;
        }

        refs.startFocus.disabled = true;
        const selectedTask = state.selectedTask;

        if (selectedTask) {
            const result = await sendJson("/api/focus-sessions", "POST", {
                taskId: selectedTask.taskId
            });

            if (!result.ok) {
                refs.startFocus.disabled = false;
                setMessage(formatError(result.data), true);
                return;
            }

            state.sessionId = result.data.sessionId;
            state.backendSession = true;
        } else {
            state.sessionId = null;
            state.backendSession = false;
        }

        state.active = true;
        state.paused = false;
        state.elapsedSeconds = 0;
        state.remainingSeconds = selectedTask ? state.durationSeconds : 0;
        startTicker();
        renderActive();
        setMessage(selectedTask ? "선택한 과제로 집중 항해를 시작했습니다." : "자유 항해로 집중을 시작했습니다.");
    }

    function startTicker() {
        stopTicker();
        state.tickId = window.setInterval(tick, 1000);
    }

    function stopTicker() {
        if (state.tickId) {
            window.clearInterval(state.tickId);
            state.tickId = null;
        }
    }

    function tick() {
        if (!state.active || state.paused) {
            return;
        }

        if (isStopwatchMode()) {
            state.elapsedSeconds += 1;
            renderTimer();
            return;
        }

        state.remainingSeconds = Math.max(state.remainingSeconds - 1, 0);
        state.elapsedSeconds = Math.max(state.durationSeconds - state.remainingSeconds, 0);
        renderTimer();

        if (state.remainingSeconds === 0) {
            state.paused = true;
            updateControls();
            setMessage("목표 시간이 끝났습니다. 항해를 마칠지 확인해 주세요.");
            openExitConfirm();
        }
    }

    function togglePause() {
        if (!state.active) {
            return;
        }

        state.paused = !state.paused;
        updateControls();
        setMessage(state.paused ? "항해를 잠시 멈췄습니다." : "집중 항해를 다시 시작했습니다.");
    }

    function openExitConfirm() {
        if (!state.active) {
            return;
        }

        refs.completeTask.checked = false;
        refs.completeWrap.hidden = !(state.backendSession && state.selectedTask);
        refs.exitConfirmModal.hidden = false;
        document.body.classList.add("modal-open");
        refs.confirmExit.focus();
    }

    function keepFocus() {
        refs.exitConfirmModal.hidden = true;
        document.body.classList.remove("modal-open");

        if (state.active && !isStopwatchMode() && state.remainingSeconds === 0) {
            state.remainingSeconds = 5 * 60;
            state.paused = false;
            updateControls();
            renderTimer();
            setMessage("5분 더 항해합니다.");
        } else if (state.active) {
            setMessage("집중 항해를 계속합니다.");
        }
    }

    async function confirmExit() {
        if (!state.active) {
            closeExitModal();
            return;
        }

        refs.confirmExit.disabled = true;
        if (state.backendSession && state.sessionId) {
            const result = await sendJson("/api/focus-sessions/" + encodeURIComponent(state.sessionId) + "/finish", "PATCH", {
                actualFinished: true,
                completeTask: refs.completeTask.checked
            });

            if (!result.ok) {
                refs.confirmExit.disabled = false;
                setMessage(formatError(result.data), true);
                return;
            }
        }

        refs.confirmExit.disabled = false;
        closeExitModal();
        stopTicker();
        const completedTask = state.selectedTask && refs.completeTask.checked;
        const wasBackendSession = state.backendSession;
        resetSession();
        renderIdle();

        if (wasBackendSession) {
            await loadTasks();
        }
        setMessage(completedTask ? "집중 항해를 마치고 과제를 완료 처리했습니다." : "집중 항해를 종료했습니다.");
    }

    function closeExitModal() {
        refs.exitConfirmModal.hidden = true;
        document.body.classList.remove("modal-open");
    }

    function handleModalBackdropClick(event) {
        if (event.target === refs.exitConfirmModal) {
            keepFocus();
        }
    }

    function resetSession() {
        state.sessionId = null;
        state.backendSession = false;
        state.active = false;
        state.paused = false;
        state.elapsedSeconds = 0;
        state.remainingSeconds = state.selectedTask ? state.durationSeconds : 0;
    }

    function renderIdle() {
        const task = state.selectedTask;
        state.elapsedSeconds = 0;
        state.remainingSeconds = task ? state.durationSeconds : 0;
        refs.focusState.textContent = "집중 모드 대기";
        refs.focusTaskTitle.textContent = task ? task.title : "자유 항해";
        refs.focusTaskDetail.textContent = task ? taskDetail(task) : "과제를 선택하거나 비워두고 바로 시작할 수 있습니다.";
        refs.sessionKind.textContent = "대기 중";
        refs.sessionTarget.textContent = task ? task.title : "자유 항해";
        refs.elapsedTime.textContent = "0초";
        refs.remainingLabel.textContent = task ? "남은 시간" : "항해 시간";
        renderTimer();
        updateControls();
    }

    function renderActive() {
        const task = state.selectedTask;
        refs.focusState.textContent = state.paused ? "항해 일시정지" : "집중 모드 ON";
        refs.focusTaskTitle.textContent = task ? task.title : "자유 항해";
        refs.focusTaskDetail.textContent = task ? taskDetail(task) : "오늘의 흐름을 놓치지 않는 자유 집중 세션입니다.";
        refs.sessionKind.textContent = state.backendSession ? "저장 중" : "스톱워치";
        refs.sessionTarget.textContent = task ? task.title : "자유 항해";
        renderTimer();
        updateControls();
    }

    function renderTimer() {
        const stopwatchMode = isStopwatchMode();
        const elapsedSeconds = stopwatchMode ? state.elapsedSeconds : Math.max(state.durationSeconds - state.remainingSeconds, 0);
        const progressBase = stopwatchMode ? state.elapsedSeconds % state.durationSeconds : elapsedSeconds;
        const progress = progressBase / state.durationSeconds * 360;

        refs.focusTimer.textContent = formatClock(stopwatchMode ? state.elapsedSeconds : state.remainingSeconds);
        refs.elapsedTime.textContent = formatElapsed(elapsedSeconds);
        refs.remainingLabel.textContent = stopwatchMode ? "항해 시간" : "남은 시간";
        refs.progressRing.style.setProperty("--timer-progress", progress.toFixed(1) + "deg");

        if (state.active) {
            refs.focusState.textContent = state.paused ? "항해 일시정지" : "집중 모드 ON";
        }
    }

    function updateControls() {
        refs.taskSelect.disabled = state.active;
        refs.startFocus.disabled = state.active;
        refs.togglePause.disabled = !state.active;
        refs.openExitConfirm.disabled = !state.active;
        refs.togglePause.textContent = state.paused ? "다시 시작" : "일시정지";
    }

    function isStopwatchMode() {
        return !state.selectedTask;
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

    function compareTasks(a, b) {
        return priorityScore(b) - priorityScore(a) || dateValue(a.deadlineAt) - dateValue(b.deadlineAt);
    }

    function priorityScore(task) {
        const importance = Number(task.importance) || 0;
        const deadlineBonus = task.deadlineAt ? 8 : 0;
        const progressBonus = task.status === "IN_PROGRESS" ? 6 : 0;
        return importance * 10 + deadlineBonus + progressBonus;
    }

    function dateValue(value) {
        return value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER;
    }

    function taskOptionLabel(task) {
        const type = task.taskType === "DAILY_GOAL" ? "Daily Goal" : "마감 업무";
        return task.title + " · " + type + " · " + (task.estimatedMinutes || 25) + "분";
    }

    function taskDetail(task) {
        const detail = task.description || (task.taskType === "DAILY_GOAL"
                ? "오늘의 반복 목표"
                : deadlineLabel(task.deadlineAt));
        return detail + " · 예상 " + (task.estimatedMinutes || 25) + "분 · 중요도 " + (task.importance || 0);
    }

    function deadlineLabel(value) {
        if (!value) {
            return "마감 미설정";
        }
        const days = Math.ceil((new Date(value).getTime() - Date.now()) / (24 * 60 * 60 * 1000));
        return days >= 0 ? "D-" + days : "기한 초과";
    }

    function formatClock(seconds) {
        const minutes = Math.floor(seconds / 60);
        const rest = seconds % 60;
        return String(minutes).padStart(2, "0") + ":" + String(rest).padStart(2, "0");
    }

    function formatElapsed(seconds) {
        const minutes = Math.floor(seconds / 60);
        if (minutes < 1) {
            return seconds + "초";
        }
        return minutes + "분";
    }

    function option(value, text) {
        const node = document.createElement("option");
        node.value = value;
        node.textContent = text;
        return node;
    }

    function asArray(value) {
        return Array.isArray(value) ? value : [];
    }

    function formatError(body) {
        if (typeof body === "string") {
            return translateError(body);
        }
        if (body && Array.isArray(body.details) && body.details.length > 0) {
            return body.details.map(translateError).join(" / ");
        }
        return body && body.message ? translateError(body.message) : "요청을 처리하지 못했습니다.";
    }

    function translateError(message) {
        if (!message) {
            return "요청을 처리하지 못했습니다.";
        }
        if (message.includes("active focus session")) {
            return "이미 진행 중인 집중 세션이 있습니다.";
        }
        if (message.includes("completed task")) {
            return "완료된 과제는 집중 모드를 시작할 수 없습니다.";
        }
        if (message.includes("task not found")) {
            return "선택한 과제를 찾을 수 없습니다.";
        }
        return message;
    }

    function setMessage(text, isError) {
        refs.message.textContent = text || "";
        refs.message.className = isError ? "error" : "";
    }
})();
