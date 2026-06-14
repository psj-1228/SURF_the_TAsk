# SURF the TAsk Deployment

이 문서는 운영 배포 절차만 따로 정리합니다. 로컬에서 앱을 실행하고 사용하는 방법은 루트 `README.md`를 참고합니다.

## AWS EC2 배포

이 프로젝트는 EC2에 Spring Boot 앱 컨테이너를 배포하고, 운영 데이터베이스는 Amazon RDS MySQL을 사용하는 구성을 기준으로 합니다.

EC2 보안 그룹에서 SSH 22번 포트와 앱 접속 포트를 열어둡니다. 기본 앱 포트는 8080입니다.

Ubuntu EC2 기준 Docker 설치 예시는 아래와 같습니다.

```bash
sudo apt update
sudo apt install -y docker.io docker-compose-plugin git
sudo usermod -aG docker $USER
newgrp docker
```

EC2에서 프로젝트를 받은 뒤 `implementation` 디렉터리로 이동합니다.

```bash
git clone <repository-url>
cd SURF_the_TAsk/implementation
```

`.env` 파일을 생성하고 RDS, JWT, SMTP 접속 정보를 입력합니다.

```bash
cat > .env <<'EOF'
SPRING_PROFILES_ACTIVE=prod
APP_PORT=8080

DB_URL=jdbc:mysql://<rds-endpoint>:3306/surf_the_task?useSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=<rds-user>
DB_PASSWORD=<rds-password>
DDL_AUTO=validate

JWT_SECRET=<replace-with-at-least-32-bytes-random-secret>
JWT_EXPIRATION_MINUTES=1440

EMAIL_NOTIFICATIONS_ENABLED=true
EMAIL_FROM=<sender-email>
SMTP_HOST=<smtp-host>
SMTP_PORT=587
SMTP_USERNAME=<smtp-username>
SMTP_PASSWORD=<smtp-app-password>
SMTP_AUTH=true
SMTP_STARTTLS_ENABLE=true
EOF
```

RDS에는 `surf_the_task` 데이터베이스와 테이블이 먼저 준비되어 있어야 합니다. 스키마 SQL은 아래 경로에 있습니다.

```text
implementation/src/main/resources/db/mysql/01-create-database.sql
implementation/src/main/resources/db/mysql/02-create-tables.sql
```

EC2에서 앱을 빌드하고 백그라운드로 실행합니다.

```bash
docker compose -f docker-compose.aws.yml up -d --build
```

로그를 확인합니다.

```bash
docker compose -f docker-compose.aws.yml logs -f app
```

브라우저에서 아래 주소로 접속합니다.

```text
http://<EC2_PUBLIC_IP>:8080
```

80번 포트로 접속하고 싶다면 `.env`에서 `APP_PORT=80`으로 바꾼 뒤 다시 실행합니다.

```bash
docker compose -f docker-compose.aws.yml up -d --build
```

새 커밋을 배포할 때는 EC2에서 아래 명령을 실행합니다.

```bash
git pull
docker compose -f docker-compose.aws.yml up -d --build
```
