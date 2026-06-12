(function () {
    const storageKey = "surfUser";
    const user = readStoredUser();

    if (!user.token) {
        window.location.href = "/login";
        return;
    }

    const refs = {
        date: document.querySelector("[data-reminders-date]"),
        logout: document.querySelector("[data-logout]"),
        message: document.querySelector("[data-reminders-message]"),
        total: document.querySelector("[data-total-reminders]"),
        sent: document.querySelector("[data-sent-reminders]"),
        pending: document.querySelector("[data-pending-reminders]"),
        historyCount: document.querySelector("[data-reminder-history-count]"),
        historyList: document.querySelector("[data-reminder-history-list]"),
        preferenceForm: document.querySelector("[data-notification-preference-form]"),
        preferenceMessage: document.querySelector("[data-preference-message]")
    };

    refs.date.textContent = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long"
    }).format(new Date());

    refs.logout.addEventListener("click", logout);
    refs.preferenceForm.addEventListener("submit", updatePreference);

    loadReminders();

    async function loadReminders() {
        setMessage("알림 기록을 불러오는 중입니다.");
        const result = await fetchJson("/api/reminders");

        if (isUnauthorized(result)) {
            redirectToLogin();
            return;
        }

        if (!result.ok) {
            setMessage(formatError(result.data), true);
            renderReminders([]);
            return;
        }

        renderReminders(asArray(result.data));
        setMessage("");
    }

    async function updatePreference(event) {
        event.preventDefault();

        if (!refs.preferenceForm.reportValidity()) {
            return;
        }

        setPreferenceMessage("설정을 저장하는 중입니다.");
        setFormBusy(true);
        const result = await sendJson("/api/notification-preference", "PATCH", readPreferenceForm());
        setFormBusy(false);

        if (isUnauthorized(result)) {
            redirectToLogin();
            return;
        }

        if (!result.ok) {
            setPreferenceMessage(formatError(result.data), true);
            return;
        }

        setPreferenceMessage("알림 설정을 저장했습니다.");
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

    async function sendJson(url, method, payload) {
        try {
            const response = await fetch(url, {
                method: method,
                headers: jsonHeaders(),
                body: JSON.stringify(payload)
            });
            const data = await readJson(response);
            return { ok: response.ok, status: response.status, data: data };
        } catch (error) {
            return { ok: false, status: 0, data: null };
        }
    }

    function renderReminders(reminders) {
        refs.total.textContent = reminders.length;
        refs.sent.textContent = reminders.filter(function (reminder) {
            return reminder.status === "SENT";
        }).length;
        refs.pending.textContent = reminders.filter(function (reminder) {
            return reminder.status === "PENDING";
        }).length;
        refs.historyCount.textContent = reminders.length;
        refs.historyList.innerHTML = "";

        if (reminders.length === 0) {
            refs.historyList.appendChild(emptyState("아직 표시할 알림이 없습니다."));
            return;
        }

        reminders.forEach(function (reminder) {
            const item = document.createElement("article");
            item.className = "reminder-history-item";
            item.innerHTML = "<div>"
                    + "<strong>" + reminderLabel(reminder.reminderType) + "</strong>"
                    + "<span>" + escapeHtml(reminder.message || "알림") + "</span>"
                    + "<small>" + channelLabel(reminder.channel) + " | "
                    + formatDateTime(reminder.sentAt || reminder.scheduledAt) + "</small>"
                    + "</div>"
                    + "<span class=\"status-chip\">" + statusLabel(reminder.status) + "</span>";
            refs.historyList.appendChild(item);
        });
    }

    function readPreferenceForm() {
        const form = refs.preferenceForm;
        return {
            emailEnabled: form.elements.emailEnabled.checked,
            inSiteEnabled: form.elements.inSiteEnabled.checked,
            availabilityReminderEnabled: form.elements.availabilityReminderEnabled.checked,
            deadlineReminderEnabled: form.elements.deadlineReminderEnabled.checked,
            minimumIntervalMinutes: Number(form.elements.minimumIntervalMinutes.value)
        };
    }

    async function readJson(response) {
        const text = await response.text();
        if (!text) {
            return null;
        }
        return JSON.parse(text);
    }

    function readStoredUser() {
        try {
            return JSON.parse(localStorage.getItem(storageKey) || "{}");
        } catch (error) {
            return {};
        }
    }

    function authHeaders() {
        return {
            Authorization: "Bearer " + user.token
        };
    }

    function jsonHeaders() {
        return {
            "Content-Type": "application/json",
            Authorization: "Bearer " + user.token
        };
    }

    function isUnauthorized(result) {
        return result.status === 401 || result.status === 403;
    }

    function asArray(value) {
        return Array.isArray(value) ? value : [];
    }

    function redirectToLogin() {
        localStorage.removeItem(storageKey);
        window.location.href = "/login";
    }

    function reminderLabel(value) {
        const labels = {
            AVAILABILITY_BASED: "가능 시간",
            DEADLINE_WARNING: "마감 경고",
            OVERDUE_ALERT: "마감 초과",
            DELAYED_IN_SITE: "집중 재개",
            DAILY_GOAL_DAY_END_ONE_HOUR: "Daily Goal 1시간 전",
            DAILY_GOAL_DAY_END_THIRTY_MINUTES: "Daily Goal 30분 전",
            DEADLINE_ONE_HOUR: "마감 1시간 전",
            DEADLINE_THIRTY_MINUTES: "마감 30분 전"
        };
        return labels[value] || "알림";
    }

    function channelLabel(value) {
        return value === "EMAIL" ? "Email" : "In-site";
    }

    function statusLabel(value) {
        const labels = {
            PENDING: "대기",
            SENT: "전송",
            FAILED: "실패",
            SKIPPED: "건너뜀",
            CANCELED: "취소"
        };
        return labels[value] || value || "상태";
    }

    function formatDateTime(value) {
        if (!value) {
            return "시간 미정";
        }
        return new Intl.DateTimeFormat("ko-KR", {
            month: "short",
            day: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        }).format(new Date(value));
    }

    function emptyState(text) {
        const node = document.createElement("p");
        node.className = "empty-state";
        node.textContent = text;
        return node;
    }

    function formatError(body) {
        if (!body) {
            return "요청을 처리하지 못했습니다.";
        }
        if (Array.isArray(body.details) && body.details.length > 0) {
            return body.details.join(" ");
        }
        return body.message || "요청을 처리하지 못했습니다.";
    }

    function escapeHtml(value) {
        return String(value)
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("\"", "&quot;")
                .replaceAll("'", "&#039;");
    }

    function setFormBusy(isBusy) {
        refs.preferenceForm.querySelectorAll("button").forEach(function (button) {
            button.disabled = isBusy;
        });
    }

    function setMessage(text, isError) {
        refs.message.textContent = text;
        refs.message.classList.toggle("error", Boolean(isError));
    }

    function setPreferenceMessage(text, isError) {
        refs.preferenceMessage.textContent = text;
        refs.preferenceMessage.classList.toggle("error", Boolean(isError));
    }
})();
