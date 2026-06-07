package com.surfthetask.dto.response;

import com.surfthetask.domain.value.AvailabilitySlot;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilitySlotResDto(
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer durationMinutes
) {

    public static AvailabilitySlotResDto from(AvailabilitySlot slot) {
        return new AvailabilitySlotResDto(
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getDurationMinutes()
        );
    }
}
