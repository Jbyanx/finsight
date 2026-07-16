# Finsight — Roadmap

> **Auditoría de estado — 2026-07-16.** Este documento se re-sincronizó contra el
> código real (backend `domain/*`, `frontend/src/pages/*`, tests). El código es la
> fuente de verdad. Los ítems que no se pudieron confirmar con certeza se marcan
> **(verificar)** en vez de afirmarse.

## Estado actual

- **Backend financiero:** los 10 dominios tienen controller + service + tests
  (auth, card, savings, debt, cash, transaction, income, invoice, financial,
  telegram). CRUD y cálculos operativos.
- **Bot de Telegram + IA:** vinculación, registro por lenguaje natural con
  confirmación por botones inline, memoria de conversación en MongoDB. Funcional.
- **Frontend React:** 5 módulos con pantalla (auth, dashboard, tarjetas, ahorros,
  deudas) + Telegram. Faltan pantallas para ingresos, gastos operativos, facturas
  y transacciones (sus endpoints ya existen).
- **Infra:** Liquibase, Swagger (springdoc), CI y CD (GitHub Actions), Docker
  Compose local.

Resumen de qué está hecho vs. pendiente al final del documento.

---

## Fase 1 — Base sólida
- [x] Estructura del monorepo
- [x] Docker Compose local (PostgreSQL + MongoDB + Redis)
- [x] Setup Spring Boot con dependencias base
- [x] OAuth2 con Google — login inteligente (registra o loguea)
- [x] JWT — access token + refresh token con rotation strategy
- [x] Tokens persistidos en BD con soporte multi-dispositivo
- [x] Endpoints: /me, /sessions, /logout, /logout-all, /refresh
- [x] UserPrincipal como POJO en SecurityContext
- [x] MapStruct configurado
- [x] GlobalExceptionHandler base
- [x] Excepciones específicas (reemplazar RuntimeException desnuda)
- [x] Registro y login propio con formulario (email + contraseña) — backend
      (`/auth/register`, `/auth/login`) y front (`Register.tsx`, `Login.tsx`)
- [x] Configuración de ambientes (dev, prod)
- [x] Variables de entorno con `.env.example`
- [x] Modelo de datos (diagrama DBML en architecture.md) — con drift conocido, ver
      Deuda técnica
- [x] GitHub Actions CI básico (`.github/workflows/ci.yml`)
- [x] Tests unitarios de AuthService, TokenService y JwtService
- [ ] Coverage mínimo 80% en servicios Fase 1 **(verificar)** — hay tests, el
      porcentaje real no se midió en esta auditoría

---

## Fase 2 — Core financiero
- [x] CRUD de tarjetas de crédito (`/api/v1/cards`, + `by-utilization`)
- [x] CRUD de cuentas de ahorro (`/api/v1/savings`)
- [x] CRUD de bolsillos (+ `deposit` / `withdraw` de monto)
- [x] Efectivo — saldo único por usuario: consulta (`GET`) y ajuste (`PATCH`).
      NO es CRUD completo por diseño (una sola fila `cash` por usuario)
- [x] Registro de transacciones (propia / compartida / cuotas) — backend
      (`POST/GET/DELETE /api/v1/transactions`, `/card/{id}`, `/shared`)
- [x] CRUD de facturas recurrentes (`/api/v1/invoices` + `/payments`) — backend
- [x] CRUD de deudas fijas (`/api/v1/debts` + `/payments`)
- [x] Registro de gastos operativos recurrentes (`/api/v1/income/expenses`)
- [x] Registro de fuentes de ingreso (`/api/v1/income/sources`, `/entries`)
- [x] Cálculo de utilización de tarjetas (`utilizationPercentage`, orden por uso)
- [x] Cálculo de ingreso neto real (`FinancialSummary.estimatedNetIncome`)
- [x] Alertas de utilización (umbral 25% / 30%) — surface en front (`healthOf`) y
      `cardAlerts` del resumen
- [ ] Plan de pagos post-corte **(verificar / pendiente)** — no se encontró lógica
      dedicada
- [~] Pipeline de ahorro hacia metas — las metas existen (`goalAmount`,
      `progressPercentage` en bolsillos); la automatización de "pipeline" no
- [x] Tests unitarios de servicios (Card, Debt, Cash, Savings, Income, Invoice,
      Financial, Transaction) — coverage exacto **(verificar)**
- [x] Tests de integración de endpoints principales (Card, Financial, Debt,
      Transaction)

---

## Fase 3 — Bot de Telegram + IA
- [x] Setup del bot (long-polling, `TelegramBotRegistrar` + `FinsightBot`)
- [x] Integración Spring Boot + Telegram API
- [x] Vinculación de cuenta — `TelegramLink` + `LinkCode` (+ pantalla web para
      generar el código y desvincular)
- [x] Setup Spring AI + Gemini (`AiConfig`, `AiServiceImpl`; `DisabledAiService`
      como fallback si no hay API key — degradación elegante)
- [x] Function calling — tools que mapean a los services (Card, Cash, Debt,
      Summary como lectura; Transaction y Debt-payment como escritura)
- [x] Contexto financiero del usuario como prompt (`FinancialContextBuilder`,
      `SystemPrompts`)
- [x] Confirmación de acciones de escritura con botones inline ✅/❌
      (`PendingAction` + `ConfirmationHandler` + `PendingActionExecutor`)
- [x] Comando: registrar transacción/gasto por mensaje natural
- [x] Comando: pagar deuda por mensaje natural
- [x] Comando: consultar estado de tarjeta ("¿con qué tarjeta pago?")
- [x] Comando: consultar resumen financiero
- [ ] Comando: registrar ingreso por mensaje natural — no hay tool de ingreso
- [ ] Comando: registrar factura recurrente por mensaje natural — no hay tool
- [ ] Comando: crear cuenta o bolsillo por mensaje natural — no hay tool
- [x] Historial de conversaciones en MongoDB (`Conversation` + `ConversationService`)
- [ ] Resumen mensual generado por IA **(verificar / pendiente)**
- [ ] Recomendaciones pre-corte automáticas **(verificar / pendiente)**
- [ ] Alertas inteligentes de facturas próximas — hay `InvoiceAlertResponse` y
      `upcomingInvoices` en el resumen; el envío proactivo por bot **(verificar)**
- [x] Tests del bot (CommandDispatcher, MessageRouter, Confirmation, PendingAction,
      Read/Write tools, Start/Unlink, TelegramFormatter)

---

## Fase 4 — React PWA
- [x] Setup React + Vite
- [x] Autenticación con Google + formulario propio (`Login`, `Register`,
      `AuthCallback`)
- [x] Dashboard — estado financiero en tiempo real (`/financial/summary`)
- [x] Vista de tarjetas y utilización (`Cards.tsx`, `UtilizationBar`)
- [x] Vista de cuentas y bolsillos (`Savings.tsx`, con deposit/withdraw)
- [x] Vista de deudas fijas (`Debts.tsx`, con registro de abonos)
- [x] Diseño responsive — mobile first (contenedor 480px, BottomNav)
- [ ] Vista de facturas recurrentes — **pendiente de UI** (endpoints listos)
- [ ] Historial de transacciones con filtros y paginación — **pendiente de UI**
- [ ] Registro manual de transacciones — **pendiente de UI**
- [ ] Vista de ingresos / gastos operativos — **pendiente de UI** (endpoints listos)
- [ ] Plan de pagos en la vista de deudas — pendiente
- [ ] Notificaciones push (alertas de utilización y facturas) — pendiente
- [ ] PWA — installable en Android **(verificar / pendiente)** — no se encontró
      manifest ni service worker

---

## Fase 5 — Polish y producción
- [x] Liquibase para migraciones (`db/changelog/db.changelog-master.xml`)
- [x] GitHub Actions CD (`.github/workflows/cd.yml`)
- [x] Swagger / OpenAPI (springdoc-openapi) — no estaba en el roadmap original
- [~] README con badge de CI — profesional/demo aún por pulir
- [ ] Deploy backend en Railway **(verificar)**
- [ ] Deploy frontend en Railway o Vercel **(verificar)**
- [ ] Subdominio finsight.jbyanx.dev **(verificar)**
- [ ] Variables de entorno en Railway **(verificar)**
- [ ] Monitoreo básico de errores
- [ ] Code coverage report en CI **(verificar)**
- [ ] Hacer repo público
- [ ] Agregar a jbyanx.dev en proyectos destacados

---

## Fase 6 — Asistente financiero inteligente
- [x] Registro de gastos operativos asociados a fuentes de ingreso (backend:
      `operational_expenses` con `income_source_id`)
- [x] Cálculo de ingreso neto real (en `FinancialSummary`)
- [ ] Recomendaciones de compras inteligentes según contexto actual
- [ ] Sugerencias de optimización de gastos operativos
- [ ] Proyección de ahorro alcanzable según ritmo actual
- [ ] Aprendizaje del perfil — sugerencias de actualización de estimados
- [ ] Modo "sin deudas" — redirige inteligencia hacia metas y crecimiento

---

## Pendiente — desglose por tipo

### (a) Pendiente de UI (endpoints listos, sin pantalla)
- **Ingresos:** fuentes de ingreso y entradas (`/api/v1/income/sources`,
  `/entries`).
- **Gastos operativos:** recurrentes (`/api/v1/income/expenses`).
- **Facturas:** recurrentes y sus pagos/comprobantes (`/api/v1/invoices`).
- **Transacciones:** registro manual e historial con filtros/paginación
  (`/api/v1/transactions`).
- **Efectivo:** ajuste del saldo desde la web (`PATCH /api/v1/cash`).

### (b) Pendiente de backend
- Plan de pagos post-corte (recomendación de cuánto abonar antes del corte).
- Resumen mensual generado por IA y recomendaciones pre-corte automáticas.
- Envío proactivo de alertas por el bot (facturas próximas, utilización).
- Tools de IA para ingreso, factura y crear cuenta/bolsillo por lenguaje natural.
- Soporte de notificaciones push.

### (c) Deuda técnica conocida
- **Drift en architecture.md:** el esquema DBML quedó atrás del código —
  `fixed_debts` no incluye `interest_rate` (existe en la entidad y en el front);
  campos derivados como `progress_percentage`, `paid_off`, `paid_amount`
  (deuda), `available_balance`, `own_balance` (ahorro) no aparecen por ser
  calculados. Actualizar o anotar como derivados.
- **Drift en CONVENTIONS.md:** los ejemplos de paquetes/tablas están en español
  (`domain.tarjeta`, `tarjetas_credito`) mientras el código real está en inglés
  (`domain.card`, `credit_cards`). Alinear la convención con la realidad.
- **Coverage:** las metas por fase (0.40 / 0.35 / 0.60) no se verificaron en esta
  auditoría; falta reporte de coverage en CI.
- **PWA:** no hay manifest ni service worker, pese a figurar como objetivo Fase 4.

---

## Decisiones de dominio pendientes

Puntos surgidos que aún no se resuelven (afectan el modelo, no solo la UI):

### 1. Pago de deuda con descuento de una fuente (cuenta / efectivo)
Hoy el pago de deuda **solo reduce `remainingBalance`**: tanto la web
(`POST /debts/{id}/payments` con `{ amount }`) como el bot
(`DebtWriteTools.previewDebtPayment`) hacen exactamente eso.

> **Nota de auditoría:** el planteamiento original asumía que "el bot ya descuenta
> de una fuente"; el código NO lo confirma — el bot tampoco toca efectivo ni
> cuentas al pagar deuda. Por tanto la decisión aplica a **ambos** clientes: si se
> quiere que un abono descuente de una fuente real (cuenta/efectivo), hay que
> diseñarlo en el backend (nuevo campo de origen en `DebtPaymentRequest` +
> movimiento atómico saldo-deuda ↔ fuente) y luego reflejarlo en web y bot para
> que se comporten igual.

### 2. Deudas informales sin cuota mensual (préstamos familiares)
Hoy `FixedDebt.monthlyPayment` es **obligatorio** (`@Column(nullable = false)` y
`@DecimalMin` en el request). Un préstamo familiar sin cuota fija no encaja.
Evaluar hacer `monthlyPayment` opcional (nullable) y ajustar validación, UI y los
cálculos que asumen una cuota mensual.
