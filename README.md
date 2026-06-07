# Finsight

![CI](https://github.com/Jbyanx/finsight/actions/workflows/ci.yml/badge.svg)

Personal finance manager with AI-powered recommendations.

## Stack

- **Backend:** Spring Boot 3, Java 21
- **Databases:** PostgreSQL, MongoDB, Redis
- **AI:** Spring AI + Gemini
- **Bot:** Telegram
- **Frontend:** React PWA
- **Deploy:** Railway

## Local setup

```bash
# 1. Clone the repo
git clone git@github.com:Jbyanx/finsight.git
cd finsight

# 2. Copy env file and fill values
cp .env.example .env

# 3. Start databases
docker compose up -d

# 4. Run backend
cd backend
mvn spring-boot:run
```

## Documentation

- [Domain](docs/domain.md)
- [Architecture](docs/architecture.md)
- [Roadmap](docs/roadmap.md)
- [Conventions](CONVENTIONS.md)
