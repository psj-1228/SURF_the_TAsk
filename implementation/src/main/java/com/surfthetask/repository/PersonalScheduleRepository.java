package com.surfthetask.repository;

import com.surfthetask.domain.entity.PersonalSchedule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalScheduleRepository extends JpaRepository<PersonalSchedule, Long> {

    List<PersonalSchedule> findByUserUserId(Long userId);
}
