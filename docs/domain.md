# Finsight — Domain Document

## El problema
Gestión financiera personal dispersa entre apps bancarias, hojas de cálculo y chats de IA.
El usuario necesita un sistema centralizado que registre, analice y recomiende acciones
sobre su dinero en tiempo real, con mínima fricción.

## Entidades del dominio

### Usuario
- Autenticación OAuth2 con Google (principal)
- Registro y login propios con formulario (email + contraseña) — Fase 2
- Botón inteligente: registra si no existe, loguea si ya existe — mismo flujo para ambos métodos
- Multi-tenant desde el día uno — datos de cada usuario completamente aislados
- Tokens de sesión persistidos en BD con soporte de múltiples dispositivos simultáneos
- Access token (15 min) + Refresh token (30 días) con rotation strategy

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
- Origen: tarjeta de crédito, cuenta de ahorro o efectivo
- Tipo: propia / compartida
- Si compartida: responsable y fecha límite de cobro
- Número de cuotas (para compras a crédito)
- Estado: pendiente / pagada

### Factura
- Diferente a una Transacción — es recurrente y tiene fecha límite
- Descripción (plan de telefonía, servicios públicos, Netflix, etc)
- Monto
- Frecuencia (mensual, bimestral, etc)
- Fecha límite de pago
- Responsable del pago (propio o externo — "cobrarle a mamá")
- Estado: pendiente / pagada
- Método de pago asociado (tarjeta, efectivo, transferencia)
- El bot distingue: "pagué Temu" → Transacción / "debo pagar el recibo de luz el 15" → Factura

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

### Facturas recurrentes
- El sistema alerta antes de la fecha límite
- Distingue facturas propias de las que se deben cobrar a otros
- "Recuérdale a mamá el plan de telefonía, límite en 5 días"

### Bot de Telegram
- Lenguaje natural — el bot interpreta la intención
- Registro de transacciones por mensaje
- Registro de facturas recurrentes por mensaje
- Creación de cuentas y bolsillos por mensaje
- Consultas de estado y recomendaciones
- El sistema nunca modifica datos sin confirmación explícita del usuario

### Configuración de Gasto Operativo
- Los montos y frecuencias son estimados configurables por el usuario
- Los cambios aplican desde la siguiente transacción, no retroactivo
- El historial conserva el valor que tenía cuando se registró

### Aprendizaje del perfil
- El sistema detecta si el gasto real difiere del estimado consistentemente
- Sugiere actualizar configuración pero nunca lo hace automáticamente
- El usuario siempre confirma o rechaza los cambios sugeridos

## Ciclo financiero mensual

### Fase 1 — Día a día
Registro de transacciones y facturas via bot o app. Actualización de utilización en tiempo real.

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

## Buenas prácticas técnicas establecidas
- Mapeo siempre en el Service, nunca en el Controller (excepto cuando el principal ya es POJO)
- Paginación en todas las colecciones que crecen con el tiempo
- Nunca retornar entidades JPA desde controllers o services públicos — siempre DTOs
- FetchType.LAZY por defecto, @EntityGraph explícito donde se necesite
- Excepciones específicas con GlobalExceptionHandler — nunca RuntimeException desnuda

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
