# Finsight — Domain Document

## El problema
Gestión financiera personal dispersa entre apps bancarias, hojas de cálculo y chats de IA.
El usuario necesita un sistema centralizado que registre, analice y recomiende acciones
sobre su dinero en tiempo real, con mínima fricción.

## Entidades del dominio

### Usuario
- Autenticación OAuth2 con Google
- Multi-tenant desde el día uno
- Cada usuario tiene sus propias tarjetas, cuentas y transacciones aisladas

### Tarjeta de Crédito
- Institución (Nu, Rappi, Bancolombia, etc)
- Cupo total
- Fecha de corte (puede variar ±1 día)
- Fecha límite de pago (puede variar ±1 día)
- Cupo utilizado actual
- Porcentaje de utilización (meta: máximo 30%)
- Transacciones asociadas (propias y compartidas)

### Cuenta de Ahorro
- Institución
- Saldo total
- Bolsillos internos opcionales
- El usuario decide si existen bolsillos — se confía en su criterio

### Bolsillo
- Nombre
- Saldo actual
- Meta opcional con porcentaje de avance

### Transacción
- Monto, descripción, fecha, categoría
- Tarjeta o cuenta origen
- Tipo: propia / compartida
- Si compartida: responsable y fecha límite de cobro
- Número de cuotas
- Estado: pendiente / pagada

### Deuda Fija
- Institución, monto total, cuota mensual
- Fecha límite de pago mensual
- Saldo restante

### Gasto Recurrente Operativo
- Descripción (gasolina, merienda, transporte)
- Frecuencia (por turno, semanal, mensual)
- Monto estimado
- Asociado a fuente de ingreso opcional
- El sistema lo descuenta automáticamente del ingreso bruto
  para calcular el ingreso neto real disponible

## Reglas de negocio

### Utilización de tarjeta
- Utilización ideal: máximo 30% del cupo
- Alerta amarilla: 25%
- Alerta roja: 30%
- Fórmula: (saldo_utilizado / cupo_total) * 100

### Recomendación de pago
- Pagar al mayor plazo posible sin generar intereses
- Nunca pagar solo el mínimo
- Recomendar tarjeta de menor utilización disponible

### Gastos compartidos
- El sistema registra quién debe qué
- Alerta N días antes del vencimiento del cobro

### Bot de Telegram
- Lenguaje natural — el bot interpreta la intención
- Registro de transacciones por mensaje
- Creación de cuentas y bolsillos por mensaje
- Consultas de estado y recomendaciones

## Ciclo financiero mensual

### Fase 1 — Día a día
Registro de transacciones via bot o app. Actualización de utilización en tiempo real.

### Fase 2 — Pre-corte (2-3 días antes)
Alertas sobre estado de tarjetas y recomendaciones de abono.

### Fase 3 — Post-corte
Cálculo del monto a pagar, separación de deudas propias y compartidas,
plan de pagos con fechas exactas.

### Fase 4 — Pipeline de ahorro
Redirección de ingresos disponibles hacia metas de ahorro activas.

## Regla — Ingreso neto real
- Ingreso bruto: lo que ganas
- Ingreso neto: ingreso bruto - gastos operativos asociados
- El sistema nunca cuenta el ingreso bruto como disponible
- Toda recomendación financiera se basa en el ingreso neto real

### Configuración de Gasto Operativo
- Los montos y frecuencias son estimados configurables por el usuario
- El usuario puede actualizar desde la app o via bot
- "Oye, cambia mi gasto de gasolina por dia laboral a $15.000"
- "A partir de ahora tanqueo full cada 15 días, son $50.000"
- "A partir de ahora tanqueo full cada que el vehículo me pida reserva, y te indico cuando fue y cuanto me gasté"
- Los cambios aplican desde la siguiente transacción, no retroactivo
- El historial conserva el valor que tenía cuando se registró

### Aprendizaje del perfil
- El sistema detecta si el gasto real difiere del estimado consistentemente
- Sugiere al usuario actualizar su configuración
- "Noto que llevas 3 semanas gastando $15.000 en gasolina,
  ¿quieres que actualice tu estimado de $20.000?"
- El usuario confirma o rechaza — nunca actualiza automáticamente sin confirmación

## Stack técnico
- Backend: Spring Boot 3, Java 21
- BD Relacional: PostgreSQL
- BD Documental: MongoDB
- Caché: Redis
- IA: Spring AI + Gemini API
- Bot: Telegram
- Frontend: React PWA
- Deploy: Railway
- CI/CD: GitHub Actions