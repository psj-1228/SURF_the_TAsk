package com.surfthetask.service;

import com.surfthetask.domain.entity.PersonalSchedule;
import com.surfthetask.domain.value.AvailabilitySlot;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ScheduleAnalyzer {

    private static final LocalTime DAY_START = LocalTime.MIN;
    private static final LocalTime DAY_END = LocalTime.of(23, 59);

    public List<AvailabilitySlot> calculateAvailability(List<PersonalSchedule> schedules) {
        List<AvailabilitySlot> slots = new ArrayList<>();

        for (DayOfWeek day : DayOfWeek.values()) {
            List<PersonalSchedule> dailySchedules = schedules.stream()
                    .filter(schedule -> schedule.getDayOfWeek() == day)
                    .sorted(Comparator.comparing(PersonalSchedule::getStartTime))
                    .toList();

            LocalTime cursor = DAY_START;
            for (PersonalSchedule schedule : dailySchedules) {
                if (schedule.getStartTime().isAfter(cursor)) {
                    slots.add(new AvailabilitySlot(day, cursor, schedule.getStartTime()));
                }
                if (schedule.getEndTime().isAfter(cursor)) {
                    cursor = schedule.getEndTime();
                }
            }
            if (DAY_END.isAfter(cursor)) {
                slots.add(new AvailabilitySlot(day, cursor, DAY_END));
            }
        }

        return slots;
    }

    public boolean hasScheduleConflict(PersonalSchedule newSchedule, List<PersonalSchedule> schedules) {
        return schedules.stream().anyMatch(newSchedule::isOverlapped);
    }

    public boolean isAvailable(LocalDateTime now, List<AvailabilitySlot> slots) {
        return slots.stream().anyMatch(slot -> slot.contains(now));
    }
}
