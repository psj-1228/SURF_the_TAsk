(function () {
    const storageKey = "surfUser";
    const user = readStoredUser();
    const dayStartMinutes = 7 * 60;
    const dayEndMinutes = 24 * 60;
    const hourHeight = 52;
    const dayHeight = ((dayEndMinutes - dayStartMinutes) / 60) * hourHeight;
    let editingScheduleId = null;
    let pendingDeleteScheduleId = null;
    let lastFocusedElement = null;
    const days = [
        { value: "MONDAY", label: "월" },
        { value: "TUESDAY", label: "화" },
        { value: "WEDNESDAY", label: "수" },
        { value: "THURSDAY", label: "목" },
        { value: "FRIDAY", label: "금" }
    ];

    if (!user.token) {
        window.location.href = "/login";
        return;
    }

    const refs = {
        date: document.querySelector("[data-schedule-date]"),
        logout: document.querySelector("[data-logout]"),
        message: document.querySelector("[data-schedule-message]"),
        form: document.querySelector("[data-schedule-form]"),
        formMessage: document.querySelector("[data-schedule-form-message]"),
        grid: document.querySelector("[data-schedule-grid]"),
        list: document.querySelector("[data-schedule-list]"),
        availabilityList: document.querySelector("[data-availability-list]"),
        scheduleCount: document.querySelector("[data-schedule-count]"),
        availabilityHours: document.querySelector("[data-availability-hours]"),
        nextAvailability: document.querySelector("[data-next-availability]"),
        nextAvailabilityDay: document.querySelector("[data-next-availability-day]"),
        scheduleMode: document.querySelector("[data-schedule-mode]"),
        scheduleFormTitle: document.querySelector("[data-schedule-form-title]"),
        scheduleSubmit: document.querySelector("[data-schedule-submit]"),
        cancelScheduleEdit: document.querySelector("[data-cancel-schedule-edit]"),
        confirmDeleteModal: document.querySelector("[data-confirm-delete-modal]"),
        deleteConfirmTitle: document.querySelector("[data-delete-confirm-title]"),
        deleteConfirmMessage: document.querySelector("[data-delete-confirm-message]"),
        confirmDelete: document.querySelector("[data-confirm-delete]")
    };

    refs.date.textContent = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long"
    }).format(new Date());

    refs.logout.addEventListener("click", logout);
    refs.form.addEventListener("submit", handleSaveSchedule);
    refs.form.addEventListener("reset", function () {
        window.setTimeout(function () {
            editingScheduleId = null;
            updateScheduleFormMode();
            clearFormMessage();
        }, 0);
    });
    refs.list.addEventListener("click", handleScheduleListClick);
    refs.cancelScheduleEdit.addEventListener("click", resetScheduleForm);
    refs.confirmDeleteModal.addEventListener("click", handleDeleteModalBackdropClick);
    refs.confirmDelete.addEventListener("click", confirmDeleteSchedule);
    document.querySelectorAll("[data-cancel-delete]").forEach(function (button) {
        button.addEventListener("click", closeDeleteConfirm);
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && !refs.confirmDeleteModal.hidden) {
            closeDeleteConfirm();
        }
    });

    buildGridShell();
    loadSchedulePage();

    async function loadSchedulePage() {
        setMessage("개인 시간표를 불러오는 중입니다.");

        const [schedulesResult, availabilityResult] = await Promise.all([
            fetchJson("/api/schedules"),
            fetchJson("/api/availability")
        ]);

        if (isUnauthorized(schedulesResult) || isUnauthorized(availabilityResult)) {
            redirectToLogin();
            return;
        }

        const schedules = schedulesResult.ok ? asArray(schedulesResult.data) : [];
        const availability = availabilityResult.ok ? asArray(availabilityResult.data) : [];

        renderSchedules(schedules);
        renderAvailability(availability);

        const failed = [schedulesResult, availabilityResult].some(function (result) {
            return !result.ok;
        });
        setMessage(failed ? "일부 시간표 데이터를 불러오지 못했습니다." : "", failed);
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

    async function handleSaveSchedule(event) {
        event.preventDefault();

        if (!refs.form.reportValidity()) {
            return;
        }

        const payload = readScheduleForm();
        const validation = validateSchedulePayload(payload);
        if (validation) {
            setFormMessage(validation, true);
            return;
        }

        const url = editingScheduleId
                ? "/api/schedules/" + encodeURIComponent(editingScheduleId)
                : "/api/schedules";
        const method = editingScheduleId ? "PUT" : "POST";
        const successMessage = editingScheduleId ? "일정을 수정했습니다." : "일정이 등록되었습니다.";

        setFormBusy(true);
        const result = await sendJson(url, method, payload);
        setFormBusy(false);

        if (!result.ok) {
            setFormMessage(formatError(result.data), true);
            return;
        }

        resetScheduleForm();
        setFormMessage(successMessage);
        await loadSchedulePage();
    }

    async function handleScheduleListClick(event) {
        const editButton = event.target.closest("[data-edit-schedule]");
        if (editButton) {
            startScheduleEdit(editButton);
            return;
        }

        const button = event.target.closest("[data-delete-schedule]");
        if (!button) {
            return;
        }

        const scheduleId = button.dataset.deleteSchedule;
        const title = button.dataset.scheduleTitle || "일정";
        openDeleteConfirm(scheduleId, title);
    }

    function startScheduleEdit(button) {
        editingScheduleId = button.dataset.editSchedule;
        refs.form.elements.title.value = button.dataset.scheduleTitle || "";
        refs.form.elements.dayOfWeek.value = button.dataset.scheduleDay || "MONDAY";
        refs.form.elements.startTime.value = trimTime(button.dataset.scheduleStart || "09:00");
        refs.form.elements.endTime.value = trimTime(button.dataset.scheduleEnd || "10:15");
        refs.form.elements.repeatType.value = button.dataset.scheduleRepeat || "WEEKLY";
        updateScheduleFormMode();
        clearFormMessage();
        if (window.innerWidth < 900) {
            refs.form.scrollIntoView({ behavior: "smooth", block: "start" });
        }
        refs.form.elements.title.focus();
    }

    function resetScheduleForm() {
        editingScheduleId = null;
        refs.form.reset();
        refs.form.elements.startTime.value = "09:00";
        refs.form.elements.endTime.value = "10:15";
        updateScheduleFormMode();
    }

    function updateScheduleFormMode() {
        const isEditing = editingScheduleId !== null;
        refs.scheduleMode.textContent = isEditing ? "Edit schedule" : "Add schedule";
        refs.scheduleFormTitle.textContent = isEditing ? "일정 수정" : "일정 추가";
        refs.scheduleSubmit.textContent = isEditing ? "수정 저장" : "저장";
        refs.cancelScheduleEdit.hidden = !isEditing;
    }

    function openDeleteConfirm(scheduleId, title) {
        pendingDeleteScheduleId = scheduleId;
        lastFocusedElement = document.activeElement;
        refs.deleteConfirmTitle.textContent = "일정을 삭제할까요?";
        refs.deleteConfirmMessage.textContent = "'" + title + "'을 삭제하면 가능 시간 계산에서 바로 제외됩니다.";
        refs.confirmDeleteModal.hidden = false;
        document.body.classList.add("modal-open");
        refs.confirmDelete.focus();
    }

    function closeDeleteConfirm() {
        pendingDeleteScheduleId = null;
        refs.confirmDeleteModal.hidden = true;
        document.body.classList.remove("modal-open");
        if (lastFocusedElement && typeof lastFocusedElement.focus === "function") {
            lastFocusedElement.focus();
        }
    }

    function handleDeleteModalBackdropClick(event) {
        if (event.target === refs.confirmDeleteModal) {
            closeDeleteConfirm();
        }
    }

    async function confirmDeleteSchedule() {
        if (!pendingDeleteScheduleId) {
            closeDeleteConfirm();
            return;
        }

        const scheduleId = pendingDeleteScheduleId;
        refs.confirmDelete.disabled = true;
        const result = await sendJson("/api/schedules/" + encodeURIComponent(scheduleId), "DELETE");
        refs.confirmDelete.disabled = false;

        if (!result.ok) {
            setMessage(formatError(result.data), true);
            closeDeleteConfirm();
            return;
        }

        closeDeleteConfirm();
        setMessage("일정이 삭제되었습니다.");
        await loadSchedulePage();
    }

    function buildGridShell() {
        refs.grid.innerHTML = "";
        refs.grid.appendChild(div("board-corner", "시간"));

        days.forEach(function (day) {
            refs.grid.appendChild(div("day-header", day.label));
        });

        const timeRail = div("time-rail", "");
        timeRail.style.minHeight = dayHeight + "px";
        for (let hour = 7; hour <= 23; hour += 1) {
            const label = div("time-label", pad(hour) + ":00");
            label.style.top = ((hour * 60 - dayStartMinutes) / 60 * hourHeight) + "px";
            timeRail.appendChild(label);
        }
        refs.grid.appendChild(timeRail);

        days.forEach(function (day, index) {
            const column = div("day-column", "");
            column.dataset.dayColumn = day.value;
            column.style.gridColumn = String(index + 2);
            column.style.minHeight = dayHeight + "px";
            refs.grid.appendChild(column);
        });
    }

    function renderSchedules(schedules) {
        const sorted = asArray(schedules).sort(compareSchedules);
        refs.scheduleCount.textContent = sorted.length;
        clearScheduleBlocks();
        renderScheduleList(sorted);

        sorted.forEach(function (schedule) {
            const column = refs.grid.querySelector("[data-day-column=\"" + schedule.dayOfWeek + "\"]");
            if (!column) {
                return;
            }

            const start = Math.max(minutes(schedule.startTime), dayStartMinutes);
            const end = Math.min(minutes(schedule.endTime), dayEndMinutes);
            if (end <= dayStartMinutes || start >= dayEndMinutes || end <= start) {
                return;
            }

            const block = document.createElement("article");
            block.className = "schedule-block tone-" + toneIndex(schedule);
            block.style.top = ((start - dayStartMinutes) / 60 * hourHeight) + "px";
            block.style.height = Math.max(((end - start) / 60 * hourHeight) - 6, 30) + "px";
            block.innerHTML = "<strong>" + escapeHtml(schedule.title || "일정") + "</strong>"
                    + "<span>" + trimTime(schedule.startTime) + "-" + trimTime(schedule.endTime) + "</span>";
            column.appendChild(block);
        });
    }

    function renderScheduleList(schedules) {
        refs.list.innerHTML = "";

        if (schedules.length === 0) {
            refs.list.appendChild(emptyState("등록된 일정이 없습니다."));
            return;
        }

        schedules.forEach(function (schedule) {
            const item = document.createElement("article");
            item.className = "registered-item";
            item.style.borderLeftColor = toneColor(schedule);
            item.innerHTML = "<div class=\"registered-main\">"
                    + "<strong>" + escapeHtml(schedule.title || "일정") + "</strong>"
                    + "<span>" + dayName(schedule.dayOfWeek) + " "
                    + trimTime(schedule.startTime) + " - " + trimTime(schedule.endTime)
                    + " | " + repeatLabel(schedule.repeatType) + "</span>"
                    + "</div>"
                    + "<div class=\"registered-actions\">"
                    + "<button class=\"small-button\" type=\"button\" data-edit-schedule=\""
                    + schedule.scheduleId + "\" data-schedule-title=\""
                    + escapeHtml(schedule.title || "일정") + "\" data-schedule-day=\""
                    + schedule.dayOfWeek + "\" data-schedule-start=\""
                    + trimTime(schedule.startTime) + "\" data-schedule-end=\""
                    + trimTime(schedule.endTime) + "\" data-schedule-repeat=\""
                    + (schedule.repeatType || "WEEKLY") + "\">수정</button>"
                    + "<button class=\"small-button danger\" type=\"button\" data-delete-schedule=\""
                    + schedule.scheduleId + "\" data-schedule-title=\""
                    + escapeHtml(schedule.title || "일정") + "\">삭제</button>"
                    + "</div>";
            refs.list.appendChild(item);
        });
    }

    function renderAvailability(slots) {
        const visibleSlots = asArray(slots)
                .filter(function (slot) {
                    return days.some(function (day) { return day.value === slot.dayOfWeek; });
                })
                .filter(function (slot) {
                    return minutes(slot.endTime) > dayStartMinutes;
                })
                .sort(compareSlots);
        const totalMinutes = visibleSlots.reduce(function (sum, slot) {
            return sum + Math.max(Number(slot.durationMinutes) || 0, 0);
        }, 0);
        const next = visibleSlots[0];

        refs.availabilityHours.textContent = formatMinutes(totalMinutes);
        refs.nextAvailability.textContent = next ? trimTime(next.startTime) : "--:--";
        refs.nextAvailabilityDay.textContent = next ? dayName(next.dayOfWeek) : "가능 시간 없음";
        refs.availabilityList.innerHTML = "";

        if (visibleSlots.length === 0) {
            refs.availabilityList.appendChild(emptyState("07시 이후 가능한 시간이 없습니다."));
            return;
        }

        visibleSlots.slice(0, 6).forEach(function (slot) {
            const item = document.createElement("article");
            item.className = "availability-item";
            item.innerHTML = "<strong>" + dayName(slot.dayOfWeek) + "</strong>"
                    + "<span>" + trimTime(slot.startTime) + " - " + trimTime(slot.endTime)
                    + " | " + formatMinutes(slot.durationMinutes) + "</span>";
            refs.availabilityList.appendChild(item);
        });
    }

    function clearScheduleBlocks() {
        refs.grid.querySelectorAll(".schedule-block").forEach(function (block) {
            block.remove();
        });
    }

    function readScheduleForm() {
        return {
            title: refs.form.elements.title.value.trim(),
            dayOfWeek: refs.form.elements.dayOfWeek.value,
            startTime: refs.form.elements.startTime.value,
            endTime: refs.form.elements.endTime.value,
            repeatType: refs.form.elements.repeatType.value
        };
    }

    function validateSchedulePayload(payload) {
        if (!payload.title) {
            return "일정명을 입력해 주세요.";
        }
        if (minutes(payload.startTime) < dayStartMinutes) {
            return "가능 시간 계산 기준에 맞춰 07:00 이후로 등록해 주세요.";
        }
        if (minutes(payload.endTime) <= minutes(payload.startTime)) {
            return "종료 시간은 시작 시간보다 늦어야 합니다.";
        }
        return "";
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

    function readStoredUser() {
        try {
            return JSON.parse(localStorage.getItem(storageKey) || "{}");
        } catch (error) {
            return {};
        }
    }

    function compareSchedules(a, b) {
        return dayIndex(a.dayOfWeek) - dayIndex(b.dayOfWeek)
                || minutes(a.startTime) - minutes(b.startTime);
    }

    function compareSlots(a, b) {
        return dayIndex(a.dayOfWeek) - dayIndex(b.dayOfWeek)
                || minutes(a.startTime) - minutes(b.startTime);
    }

    function dayIndex(value) {
        const index = days.findIndex(function (day) {
            return day.value === value;
        });
        return index === -1 ? 99 : index;
    }

    function dayName(value) {
        const labels = {
            MONDAY: "월요일",
            TUESDAY: "화요일",
            WEDNESDAY: "수요일",
            THURSDAY: "목요일",
            FRIDAY: "금요일",
            SATURDAY: "토요일",
            SUNDAY: "일요일"
        };
        return labels[value] || value || "요일 없음";
    }

    function repeatLabel(value) {
        const labels = {
            NONE: "반복 없음",
            DAILY: "매일 반복",
            WEEKLY: "매주 반복"
        };
        return labels[value] || "매주 반복";
    }

    function minutes(value) {
        if (!value) {
            return 0;
        }
        const parts = String(value).split(":");
        return Number(parts[0] || 0) * 60 + Number(parts[1] || 0);
    }

    function trimTime(value) {
        return value ? String(value).slice(0, 5) : "--:--";
    }

    function pad(value) {
        return String(value).padStart(2, "0");
    }

    function toneIndex(schedule) {
        return Math.abs(hash(String(schedule.scheduleId || schedule.title || ""))) % 6;
    }

    function toneColor(schedule) {
        return ["#176bd2", "#29b978", "#f1b83b", "#7a6be8", "#e85c57", "#10a8a0"][toneIndex(schedule)];
    }

    function hash(value) {
        let result = 0;
        for (let index = 0; index < value.length; index += 1) {
            result = ((result << 5) - result) + value.charCodeAt(index);
            result |= 0;
        }
        return result;
    }

    function formatMinutes(value) {
        const safeMinutes = Math.max(Number(value) || 0, 0);
        const hours = Math.floor(safeMinutes / 60);
        const rest = safeMinutes % 60;

        if (hours === 0) {
            return rest + "분";
        }
        if (rest === 0) {
            return hours + "h";
        }
        return hours + "h " + rest + "m";
    }

    function emptyState(text) {
        const node = document.createElement("p");
        node.className = "empty-state";
        node.textContent = text;
        return node;
    }

    function div(className, text) {
        const node = document.createElement("div");
        node.className = className;
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
            return body.details.join(" / ");
        }
        return body && body.message ? translateError(body.message) : "요청을 처리하지 못했습니다.";
    }

    function translateError(message) {
        if (message && message.includes("overlaps")) {
            return "이미 등록된 일정과 시간이 겹칩니다.";
        }
        if (message && message.includes("endTime")) {
            return "종료 시간은 시작 시간보다 늦어야 합니다.";
        }
        return message || "요청을 처리하지 못했습니다.";
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
        refs.form.querySelectorAll("button").forEach(function (button) {
            button.disabled = isBusy;
        });
    }

    function setMessage(text, isError) {
        refs.message.textContent = text || "";
        refs.message.className = isError ? "load-message error" : "load-message";
    }

    function setFormMessage(text, isError) {
        refs.formMessage.textContent = text || "";
        refs.formMessage.className = isError ? "form-message error" : "form-message";
    }

    function clearFormMessage() {
        setFormMessage("");
    }
})();
