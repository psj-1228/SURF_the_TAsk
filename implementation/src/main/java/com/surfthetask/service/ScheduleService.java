package com.surfthetask.service;

import com.surfthetask.domain.entity.PersonalSchedule;
import com.surfthetask.domain.entity.User;
import com.surfthetask.domain.enums.RepeatType;
import com.surfthetask.domain.value.AvailabilitySlot;
import com.surfthetask.dto.request.ScheduleReqDto;
import com.surfthetask.exception.BadRequestException;
import com.surfthetask.exception.ForbiddenException;
import com.surfthetask.exception.NotFoundException;
import com.surfthetask.repository.PersonalScheduleRepository;
import com.surfthetask.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private final UserRepository userRepository;
    private final PersonalScheduleRepository personalScheduleRepository;
    private final ScheduleAnalyzer scheduleAnalyzer;

    public ScheduleService(
            UserRepository userRepository,
            PersonalScheduleRepository personalScheduleRepository,
            ScheduleAnalyzer scheduleAnalyzer
    ) {
        this.userRepository = userRepository;
        this.personalScheduleRepository = personalScheduleRepository;
        this.scheduleAnalyzer = scheduleAnalyzer;
    }

    @Transactional
    public PersonalSchedule createSchedule(Long userId, ScheduleReqDto req) {
        User user = findUser(userId);
        PersonalSchedule schedule = new PersonalSchedule(
                user,
                req.title(),
                req.dayOfWeek(),
                req.startTime(),
                req.endTime(),
                defaultRepeatType(req.repeatType())
        );
        if (scheduleAnalyzer.hasScheduleConflict(schedule, personalScheduleRepository.findByUserUserId(userId))) {
            throw new BadRequestException("schedule overlaps with existing schedule");
        }
        return personalScheduleRepository.save(schedule);
    }

    @Transactional
    public PersonalSchedule updateSchedule(Long userId, Long scheduleId, ScheduleReqDto req) {
        PersonalSchedule schedule = findScheduleForUser(userId, scheduleId);
        schedule.update(
                req.title(),
                req.dayOfWeek(),
                req.startTime(),
                req.endTime(),
                defaultRepeatType(req.repeatType())
        );

        boolean overlapped = personalScheduleRepository.findByUserUserId(userId)
                .stream()
                .filter(existing -> !existing.getScheduleId().equals(scheduleId))
                .anyMatch(schedule::isOverlapped);
        if (overlapped) {
            throw new BadRequestException("schedule overlaps with existing schedule");
        }
        return schedule;
    }

    @Transactional
    public void deleteSchedule(Long userId, Long scheduleId) {
        findScheduleForUser(userId, scheduleId);
        personalScheduleRepository.deleteById(scheduleId);
    }

    public List<PersonalSchedule> getSchedules(Long userId) {
        findUser(userId);
        return personalScheduleRepository.findByUserUserId(userId);
    }

    public List<AvailabilitySlot> calculateAvailability(Long userId) {
        return scheduleAnalyzer.calculateAvailability(getSchedules(userId));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("user not found: " + userId));
    }

    private PersonalSchedule findSchedule(Long scheduleId) {
        return personalScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("schedule not found: " + scheduleId));
    }

    private PersonalSchedule findScheduleForUser(Long userId, Long scheduleId) {
        PersonalSchedule schedule = findSchedule(scheduleId);
        if (!schedule.getUser().getUserId().equals(userId)) {
            throw new ForbiddenException("schedule does not belong to authenticated user");
        }
        return schedule;
    }

    private RepeatType defaultRepeatType(RepeatType repeatType) {
        return repeatType == null ? RepeatType.WEEKLY : repeatType;
    }
}
