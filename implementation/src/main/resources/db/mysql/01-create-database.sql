CREATE DATABASE IF NOT EXISTS surf_the_task
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'surf_user'@'localhost' IDENTIFIED BY 'surf_password';
CREATE USER IF NOT EXISTS 'surf_user'@'%' IDENTIFIED BY 'surf_password';

GRANT ALL PRIVILEGES ON surf_the_task.* TO 'surf_user'@'localhost';
GRANT ALL PRIVILEGES ON surf_the_task.* TO 'surf_user'@'%';

FLUSH PRIVILEGES;
