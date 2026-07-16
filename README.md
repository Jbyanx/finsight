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

## Implementation status (audited 2026-07-16)

Backend (Spring Boot) — 10 domains with controller + service + tests:

- **Auth:** Google OAuth2 + local email/password, JWT with refresh-token rotation, multi-device sessions.
- **Cards / Savings (pockets, deposit/withdraw) / Debts (payments):** full CRUD.
- **Cash, Transactions, Income sources & entries, Operational expenses, Invoices (+ payments):** endpoints ready.
- **Financial summary, utilization & net-income calculations.**
- **Telegram bot + AI (Spring AI + Gemini):** account linking, natural-language registration of transactions and debt payments with inline-button confirmation, conversation memory in MongoDB.
- **Infra:** Liquibase, Swagger (springdoc), CI + CD (GitHub Actions).

Frontend (React PWA) — screens: auth, dashboard, cards, savings, debts, Telegram.
Pending UI: income, operational expenses, invoices, transactions (endpoints already exist).

See [Roadmap](docs/roadmap.md) for the full done-vs-pending breakdown and open domain decisions.

## Documentation

- [Domain](docs/domain.md)
- [Architecture](docs/architecture.md)
- [Roadmap](docs/roadmap.md)
- [Conventions](CONVENTIONS.md)
