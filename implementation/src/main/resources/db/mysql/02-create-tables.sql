USE surf_the_task;

CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT NOT NULL AUTO_INCREMENT,
  login_id VARCHAR(50) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(50) NOT NULL,
  email VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (user_id),
  CONSTRAINT uk_users_login_id UNIQUE (login_id),
  CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notification_preferences (
  preference_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  email_enabled BIT NOT NULL,
  in_site_enabled BIT NOT NULL,
  availability_reminder_enabled BIT NOT NULL,
  deadline_reminder_enabled BIT NOT NULL,
  minimum_interval_minutes INT NOT NULL,
  PRIMARY KEY (preference_id),
  CONSTRAINT uk_notification_preferences_user UNIQUE (user_id),
  CONSTRAINT fk_notification_preferences_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS personal_schedules (
  schedule_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  day_of_week ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') NOT NULL,
  start_time TIME(6) NOT NULL,
  end_time TIME(6) NOT NULL,
  repeat_type ENUM('NONE', 'DAILY', 'WEEKLY') NOT NULL,
  PRIMARY KEY (schedule_id),
  INDEX idx_personal_schedules_user_id (user_id),
  CONSTRAINT fk_personal_schedules_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS tasks (
  task_id BIGINT NOT NULL AUTO_INCREMENT,
  task_type VARCHAR(20) NOT NULL,
  user_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  description TEXT NULL,
  estimated_minutes INT NOT NULL,
  importance INT NOT NULL,
  status ENUM('TODO', 'IN_PROGRESS', 'DONE') NOT NULL,
  deadline_at DATETIME(6) NULL,
  warning_threshold_hours INT NULL,
  target_count_per_day INT NULL,
  current_streak INT NULL,
  last_completed_date DATE NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NULL,
  PRIMARY KEY (task_id),
  INDEX idx_tasks_user_id (user_id),
  INDEX idx_tasks_status (status),
  INDEX idx_tasks_deadline_at (deadline_at),
  CONSTRAINT fk_tasks_user
    FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS completion_records (
  record_id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  completed_at DATETIME(6) NOT NULL,
  completed_date DATE NOT NULL,
  is_canceled BIT NOT NULL,
  PRIMARY KEY (record_id),
  INDEX idx_completion_records_task_id (task_id),
  INDEX idx_completion_records_completed_date (completed_date),
  CONSTRAINT fk_completion_records_task
    FOREIGN KEY (task_id) REFERENCES tasks (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS focus_sessions (
  session_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  start_at DATETIME(6) NOT NULL,
  end_at DATETIME(6) NULL,
  is_active BIT NOT NULL,
  actual_finished BIT NULL,
  PRIMARY KEY (session_id),
  INDEX idx_focus_sessions_user_active (user_id, is_active),
  INDEX idx_focus_sessions_task_id (task_id),
  CONSTRAINT fk_focus_sessions_user
    FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT fk_focus_sessions_task
    FOREIGN KEY (task_id) REFERENCES tasks (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reminders (
  reminder_id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  task_id BIGINT NULL,
  focus_session_id BIGINT NULL,
  reminder_type ENUM('AVAILABILITY_BASED', 'DEADLINE_WARNING', 'OVERDUE_ALERT', 'DELAYED_IN_SITE') NOT NULL,
  channel ENUM('EMAIL', 'IN_SITE') NOT NULL,
  message TEXT NOT NULL,
  scheduled_at DATETIME(6) NOT NULL,
  sent_at DATETIME(6) NULL,
  status ENUM('PENDING', 'SENT', 'FAILED', 'SKIPPED', 'CANCELED') NOT NULL,
  result_reason VARCHAR(255) NULL,
  PRIMARY KEY (reminder_id),
  INDEX idx_reminders_user_id (user_id),
  INDEX idx_reminders_task_id (task_id),
  INDEX idx_reminders_focus_session_id (focus_session_id),
  INDEX idx_reminders_status_scheduled_at (status, scheduled_at),
  CONSTRAINT fk_reminders_user
    FOREIGN KEY (user_id) REFERENCES users (user_id),
  CONSTRAINT fk_reminders_task
    FOREIGN KEY (task_id) REFERENCES tasks (task_id),
  CONSTRAINT fk_reminders_focus_session
    FOREIGN KEY (focus_session_id) REFERENCES focus_sessions (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reminder_histories (
  history_id BIGINT NOT NULL AUTO_INCREMENT,
  reminder_id BIGINT NOT NULL,
  result_status ENUM('PENDING', 'SENT', 'FAILED', 'SKIPPED', 'CANCELED') NOT NULL,
  reason VARCHAR(255) NULL,
  recorded_at DATETIME(6) NOT NULL,
  PRIMARY KEY (history_id),
  INDEX idx_reminder_histories_reminder_id (reminder_id),
  CONSTRAINT fk_reminder_histories_reminder
    FOREIGN KEY (reminder_id) REFERENCES reminders (reminder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
