package com.surfthetask.dto.response;

import com.surfthetask.domain.entity.PersonalSchedule;
import com.surfthetask.domain.enums.RepeatType;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleResDto(
        Long scheduleId,
        Long userId,
        String title,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        RepeatType repeatType,
        Integer durationMinutes
) {

    public static ScheduleResDto from(PersonalSchedule schedule) {
        return new ScheduleResDto(
                schedule.getScheduleId(),
                schedule.getUser().getUserId(),
                schedule.getTitle(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getRepeatType(),
                schedule.getDurationMinutes()
        );
    }
}
