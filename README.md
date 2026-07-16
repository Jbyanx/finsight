# Finsight

![CI](https://github.com/Jbyanx/finsight/actions/workflows/ci.yml/badge.svg)

Gestor de finanzas personales con recomendaciones por IA. Registra tus gastos
mandando un mensaje al bot de Telegram —sin abrir la app— y consulta tu situación
financiera en lenguaje natural o desde una PWA mobile-first.

> Proyecto personal en desarrollo activo. La construcción es pública: sigo el
> avance en LinkedIn.

## Qué hace

- **Registro por lenguaje natural (Telegram):** "pagué 130 mil en el mercado con
  la Nu" registra el gasto y te muestra la consecuencia (utilización proyectada)
  antes de confirmar con un botón. El modelo nunca ejecuta una escritura sin tu
  confirmación explícita.
- **Salud crediticia:** control de utilización por tarjeta con la regla del 30%,
  y recomendación de con qué tarjeta pagar hoy.
- **Ahorros tipo Nu:** cuentas con bolsillos donde apartas y liberas dinero
  ("agrega $50.000", "retira $20.000"), distinguiendo tu dinero del de terceros
  (la plata que tu familia guarda en tu cuenta no cuenta como tuya).
- **Deudas:** seguimiento de deudas fijas con abonos y progreso de pago.
- **Web y bot conectados:** te registras por la web, vinculas Telegram con un
  toque, y usas el que prefieras.

## Stack

- **Backend:** Spring Boot 3.5, Java 21
- **Bases de datos:** PostgreSQL (datos financieros, ACID), MongoDB
  (conversaciones del bot), Redis (caché)
- **Migraciones:** Liquibase
- **IA:** Spring AI + Gemini (function calling)
- **Bot:** Telegram (long polling)
- **Frontend:** React 18 + TypeScript + Vite (PWA, mobile-first)
- **Docs de API:** OpenAPI / Swagger UI
- **CI:** GitHub Actions
- **Despliegue:** en evaluación

## Estructura

```
finsight/
├── backend/     Spring Boot (API REST + bot de Telegram + IA)
├── frontend/    React PWA
└── docs/        Dominio, arquitectura y roadmap
```

## Requisitos

- Java 21
- Node.js 24
- Docker y Docker Compose

## Puesta en marcha local

```bash
# 1. Clonar
git clone git@github.com:Jbyanx/finsight.git
cd finsight

# 2. Variables de entorno (rellenar valores)
cp .env.example .env

# 3. Levantar las bases de datos
docker compose up -d

# 4. Backend  → http://localhost:8080
cd backend
./mvnw spring-boot:run

# 5. Frontend → http://localhost:5173
cd ../frontend
npm install
npm run dev
```

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- El frontend usa un proxy de Vite hacia el backend, así que no necesitas
  configurar CORS en desarrollo.

## Pruebas

```bash
cd backend
./mvnw test
```

## Documentación

- [Dominio](docs/domain.md)
- [Arquitectura](docs/architecture.md)
- [Roadmap](docs/roadmap.md)
- [Convenciones](CONVENTIONS.md)
