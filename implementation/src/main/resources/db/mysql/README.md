# MySQL schema setup

Run the setup helper with a MySQL administrator account before starting the Spring Boot app.

```powershell
.\scripts\setup-mysql.ps1 -AdminUser root -AdminPassword "<mysql-root-password>"
```

The helper executes all numbered `*.sql` files in this directory in name order.
For an existing database that already has the base tables, run `03-update-reminder-notification-types.sql` with a MySQL administrator account before deploying the reminder notification changes.

Default local application credentials:

```text
database: surf_the_task
username: surf_user
password: surf_password
```

For AWS RDS, create the database with the same schema name and set these environment variables on the backend server:

```text
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://<rds-endpoint>:3306/surf_the_task?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<rds-user>
DB_PASSWORD=<rds-password>
```

Verify the local schema:

```powershell
mysql -u surf_user -p -e "USE surf_the_task; SHOW TABLES;"
```
