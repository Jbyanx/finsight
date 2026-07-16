# Finsight — Architecture

> **Nota de auditoría (2026-07-16):** el esquema DBML de abajo tiene *drift* frente
> al código y debe leerse con estas salvedades:
> - `fixed_debts` en el código incluye además `interest_rate` (no está en el DBML).
> - Varios campos que la API expone son **derivados/calculados**, no columnas:
>   deuda → `paid_amount`, `progress_percentage`, `paid_off`; ahorro →
>   `available_balance`, `own_balance`, `shared_total`; tarjeta →
>   `utilization_percentage`, `available_amount`.
> Pendiente: regenerar el DBML desde las entidades reales y marcar los derivados.

## Diagrama de Base de Datos

```dbml
// Finsight — Database Schema
// PostgreSQL — datos críticos financieros

Table users {
  id uuid [pk, default: `gen_random_uuid()`]
  email varchar(255) [not null, unique]
  name varchar(255) [not null]
  picture_url varchar(500)
  provider varchar(50) [not null, note: 'google | local']
  provider_id varchar(255)
  password_hash varchar(255) [note: 'null para OAuth2']
  active boolean [not null, default: true]
  created_at timestamp [not null]
  last_login timestamp
}

Table tokens {
  id uuid [pk, default: `gen_random_uuid()`]
  token varchar(500) [not null, unique]
  type varchar(10) [not null, note: 'ACCESS | REFRESH']
  user_id uuid [not null, ref: > users.id]
  device_info varchar(500)
  expires_at timestamp [not null]
  revoked boolean [not null, default: false]
  created_at timestamp [not null]
}

Table credit_cards {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null, ref: > users.id]
  institution varchar(100) [not null, note: 'Nu, Rappi, Bancolombia...']
  nickname varchar(100)
  credit_limit decimal(15,2) [not null]
  used_amount decimal(15,2) [not null, default: 0]
  cut_off_day int [not null, note: 'Día del mes del corte']
  payment_due_day int [not null, note: 'Día límite de pago']
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table savings_accounts {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null, ref: > users.id]
  institution varchar(100) [not null]
  nickname varchar(100)
  balance decimal(15,2) [not null, default: 0]
  is_shared boolean [not null, default: false, note: 'Ej: ahorro de un familiar']
  shared_owner_name varchar(100) [note: 'Nombre del dueño real si is_shared=true']
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table pockets {
  id uuid [pk, default: `gen_random_uuid()`]
  savings_account_id uuid [not null, ref: > savings_accounts.id]
  name varchar(100) [not null]
  balance decimal(15,2) [not null, default: 0]
  goal_amount decimal(15,2) [note: 'Meta opcional']
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table transactions {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null, ref: > users.id]
  description varchar(255) [not null]
  amount decimal(15,2) [not null]
  category varchar(100)
  date timestamp [not null]
  origin_type varchar(20) [not null, note: 'CREDIT_CARD | SAVINGS | CASH']
  credit_card_id uuid [ref: > credit_cards.id, note: 'null si no es tarjeta']
  savings_account_id uuid [ref: > savings_accounts.id, note: 'null si no es cuenta']
  is_shared boolean [not null, default: false]
  shared_with_name varchar(100)
  shared_amount decimal(15,2)
  payment_deadline date [note: 'Para cobrar la parte compartida']
  installments int [not null, default: 1, note: 'Número de cuotas']
  status varchar(20) [not null, default: 'COMPLETED', note: 'PENDING | COMPLETED']
  created_at timestamp [not null]
}

Table invoices {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null, ref: > users.id]
  description varchar(255) [not null, note: 'Plan telefonía, servicios públicos...']
  amount decimal(15,2) [not null]
  frequency varchar(20) [not null, note: 'MONTHLY | BIMONTHLY | ANNUAL']
  due_day int [not null, note: 'Día del mes límite de pago']
  payment_method varchar(20) [note: 'CREDIT_CARD | CASH | TRANSFER']
    payer_note varchar(255) [note: 'Ej: Factura de luz de papá — yo pago pero él reembolsa']
  credit_card_id uuid [ref: > credit_cards.id]
  is_own boolean [not null, default: true, note: 'false = cobrar a alguien']
  responsible_name varchar(100) [note: 'Nombre si is_own=false']
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table invoice_payments {
  id uuid [pk, default: `gen_random_uuid()`]
  invoice_id uuid [not null, ref: > invoices.id]
  amount_paid decimal(15,2) [not null]
  paid_at timestamp
  period_month int [not null, note: '1-12']
  period_year int [not null]
  status varchar(20) [not null, default: 'PENDING', note: 'PENDING | PAID | OVERDUE']
  notes varchar(255)
  voucher_url varchar(500) [note: 'URL del comprobante — imagen o PDF']
  voucher_type varchar(10) [note: 'IMAGE | PDF | null']
  created_at timestamp [not null]
}

Table fixed_debts {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null, ref: > users.id]
  institution varchar(100) [not null]
  description varchar(255)
  total_amount decimal(15,2) [not null]
  monthly_payment decimal(15,2) [not null]
  payment_due_day int [not null]
  remaining_balance decimal(15,2) [not null]
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table operational_expenses {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null, ref: > users.id]
  description varchar(255) [not null, note: 'Gasolina, merienda...']
  frequency varchar(20) [not null, note: 'PER_SHIFT | WEEKLY | MONTHLY']
  estimated_amount decimal(15,2) [not null]
  income_source_id uuid [ref: > income_sources.id, note: 'Fuente de ingreso asociada']
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table income_sources {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  description varchar(255) [not null, note: 'Turno hotel, mesada, venta...']
  type varchar(10) [not null, note: 'FIXED | VARIABLE']
  estimated_amount decimal(15,2) [note: 'null si VARIABLE']
  frequency varchar(20) [note: 'PER_SHIFT | WEEKLY | MONTHLY | null si VARIABLE']
  active boolean [not null, default: true]
  created_at timestamp [not null]
}

Table income_entries {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  income_source_id uuid [ref: > income_sources.id, note: 'null si ingreso espontáneo']
  amount decimal(15,2) [not null]
  description varchar(255)
  received_at timestamp [not null]
  destination_type varchar(20) [note: 'CASH | SAVINGS | POCKET']
  savings_account_id uuid [ref: > savings_accounts.id]
  pocket_id uuid [ref: > pockets.id]
  created_at timestamp [not null]
}

Table cash {
  id uuid [pk, default: `gen_random_uuid()`]
  user_id uuid [not null, ref: > users.id, note: 'Uno por usuario']
  balance decimal(15,2) [not null, default: 0]
  updated_at timestamp [not null]
}
```

---

## Colecciones MongoDB

```
conversationsBot       → historial de mensajes con el bot de Telegram
aiSuggestions          → log de sugerencias generadas por Gemini
profileLearningLogs    → registro de discrepancias en gastos operativos
```

---

## Redis — key patterns

```
tarjeta:estado:{userId}:{cardId}     → utilización actual de una tarjeta
usuario:perfil:{userId}              → datos del perfil cacheados
usuario:resumen:{userId}             → resumen financiero del mes
factura:proximas:{userId}            → facturas próximas a vencer
```

---

## Decisiones de arquitectura

### ¿Por qué PostgreSQL para los datos financieros?
Transacciones ACID — un movimiento de dinero no puede quedar a medias.
Las relaciones entre tarjetas, transacciones y usuarios son complejas y relacionales por naturaleza.

### ¿Por qué MongoDB para el bot y la IA?
Los mensajes del bot y las respuestas de Gemini son documentos no estructurados que varían en forma.
El historial de conversaciones no tiene schema fijo — cada sesión puede tener diferente contexto.

### ¿Por qué Redis para el estado financiero?
El estado de una tarjeta (utilización actual) se consulta en cada mensaje del bot y en el dashboard.
Cachear ese cálculo evita queries pesados en cada interacción.
TTL corto (5 min) garantiza que los datos no queden obsoletos.
