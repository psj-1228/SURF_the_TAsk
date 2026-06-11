package com.surfthetask;

import com.surfthetask.domain.entity.PersonalSchedule;
import com.surfthetask.domain.entity.User;
import com.surfthetask.domain.enums.RepeatType;
import com.surfthetask.domain.value.AvailabilitySlot;
import com.surfthetask.service.ScheduleAnalyzer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleAnalyzerTest {

    private final ScheduleAnalyzer analyzer = new ScheduleAnalyzer();

    @Test
    void availabilityStartsAtSevenWhenNoScheduleExists() {
        List<AvailabilitySlot> slots = analyzer.calculateAvailability(List.of());

        assertThat(slots).hasSize(7);
        assertThat(slots).allSatisfy(slot -> {
            assertThat(slot.getStartTime()).isEqualTo(LocalTime.of(7, 0));
            assertThat(slot.getEndTime()).isEqualTo(LocalTime.of(23, 59));
        });
    }

    @Test
    void scheduleOverlappingSevenMovesFirstAvailableSlotAfterScheduleEnd() {
        PersonalSchedule earlyClass = schedule(
                "Early class",
                DayOfWeek.MONDAY,
                LocalTime.of(6, 30),
                LocalTime.of(8, 30)
        );

        List<AvailabilitySlot> mondaySlots = analyzer.calculateAvailability(List.of(earlyClass))
                .stream()
                .filter(slot -> slot.getDayOfWeek() == DayOfWeek.MONDAY)
                .toList();

        assertThat(mondaySlots).hasSize(1);
        assertThat(mondaySlots.get(0).getStartTime()).isEqualTo(LocalTime.of(8, 30));
        assertThat(mondaySlots.get(0).getEndTime()).isEqualTo(LocalTime.of(23, 59));
    }

    private PersonalSchedule schedule(String title, DayOfWeek day, LocalTime start, LocalTime end) {
        return new PersonalSchedule(
                new User("schedule_test", "hash", "Schedule Test", "schedule@example.com"),
                title,
                day,
                start,
                end,
                RepeatType.WEEKLY
        );
    }
}
