package com.surfthetask.dto.request;

import jakarta.validation.constraints.Positive;

public record NotificationPreferenceReqDto(
        Boolean emailEnabled,
        Boolean inSiteEnabled,
        Boolean availabilityReminderEnabled,
        Boolean deadlineReminderEnabled,
        @Positive Integer minimumIntervalMinutes
) {
}
