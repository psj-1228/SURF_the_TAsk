package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.NotificationPreference;

public record NotificationPreferenceResDto(
        Long preferenceId,
        Long userId,
        Boolean emailEnabled,
        Boolean inSiteEnabled,
        Boolean availabilityReminderEnabled,
        Boolean deadlineReminderEnabled,
        Integer minimumIntervalMinutes
) {

    public static NotificationPreferenceResDto from(NotificationPreference preference) {
        return new NotificationPreferenceResDto(
                preference.getPreferenceId(),
                preference.getUser().getUserId(),
                preference.isEmailEnabled(),
                preference.isInSiteEnabled(),
                preference.isAvailabilityReminderEnabled(),
                preference.isDeadlineReminderEnabled(),
                preference.getMinimumIntervalMinutes()
        );
    }
}
