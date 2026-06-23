# Finsight — Roadmap

## Estado actual: Fase 1 — Base sólida (en progreso)

---

## Fase 1 — Base sólida
- [x] Estructura del monorepo
- [x] Docker Compose local (PostgreSQL + MongoDB + Redis)
- [x] Setup Spring Boot con dependencias base
- [x] OAuth2 con Google — login inteligente (registra o loguea)
- [x] JWT — access token (15min) + refresh token (30 días) con rotation strategy
- [x] Tokens persistidos en BD con soporte multi-dispositivo
- [x] Endpoints: /me, /sessions, /logout, /logout-all, /refresh
- [x] UserPrincipal como POJO en SecurityContext
- [x] MapStruct configurado
- [x] GlobalExceptionHandler base
- [x] Excepciones específicas (reemplazar RuntimeException desnuda)
- [ ] Registro y login propio con formulario (email + contraseña)
- [x] Configuración de ambientes (dev, prod)
- [x] Variables de entorno con `.env.example` completo
- [x] Modelo de datos completo (diagrama DBML en architecture.md)
- [ ] GitHub Actions CI básico (build + test en cada PR)
- [ ] Tests unitarios de AuthService y TokenService
- [ ] Suite de tests Fase 1 (AuthService, TokenService, JwtService — mínimo 80% coverage)

---

## Fase 2 — Core financiero
- [ ] CRUD de tarjetas de crédito
- [ ] CRUD de cuentas de ahorro
- [ ] CRUD de bolsillos
- [ ] CRUD de efectivo
- [ ] Registro de transacciones (propia / compartida / cuotas)
- [ ] CRUD de facturas recurrentes
- [ ] CRUD de deudas fijas
- [ ] Registro de gastos operativos recurrentes
- [ ] Registro de fuentes de ingreso
- [ ] Cálculo de utilización de tarjetas en tiempo real
- [ ] Cálculo de ingreso neto real
- [ ] Alertas de utilización (25% y 30%)
- [ ] Plan de pagos post-corte
- [ ] Pipeline de ahorro hacia metas
- [ ] Tests unitarios de servicios (mínimo 80% coverage)
- [ ] Tests de integración de endpoints principales

---


## Fase 3 — Bot de Telegram + IA
- [ ] Setup del bot con BotFather
- [ ] Integración Spring Boot + Telegram API (telegrambots-spring-boot-starter)
- [ ] Vinculación de cuenta — TelegramLink + LinkCode (autenticación sin login repetido)
- [ ] Setup Spring AI + Gemini
- [ ] Function calling — tools que mapean a los services existentes
- [ ] Contexto financiero del usuario como prompt
- [ ] Comando: registrar transacción por mensaje natural
- [ ] Comando: registrar ingreso por mensaje natural
- [ ] Comando: registrar factura recurrente por mensaje natural
- [ ] Comando: pagar deuda por mensaje natural
- [ ] Comando: consultar estado de tarjeta ("¿con qué tarjeta pago hoy?")
- [ ] Comando: crear cuenta o bolsillo
- [ ] Comando: consultar resumen financiero
- [ ] Resumen mensual generado por IA
- [ ] Recomendaciones pre-corte automáticas
- [ ] Alertas inteligentes de facturas próximas
- [ ] Historial de conversaciones en MongoDB
- [ ] Tests del bot + tests de integración con IA
- [ ] Suite de tests Fase 3 (mínimo 0.50 coverage)

---

## Fase 4 — React PWA
- [ ] Setup React + Vite
- [ ] Autenticación con Google + formulario propio
- [ ] Dashboard — estado financiero en tiempo real
- [ ] Vista de tarjetas y utilización
- [ ] Vista de cuentas y bolsillos
- [ ] Historial de transacciones con filtros y paginación
- [ ] Vista de facturas recurrentes
- [ ] Registro manual de transacciones
- [ ] Vista de deudas fijas y plan de pagos
- [ ] Notificaciones push (alertas de utilización y facturas)
- [ ] Diseño responsive — mobile first
- [ ] PWA — installable en Android

---

## Fase 5 — Polish y producción
- [ ] Liquibase para migraciones en producción
- [ ] GitHub Actions CD (deploy automático en merge a main)
- [ ] Deploy backend en Railway
- [ ] Deploy frontend en Railway o Vercel
- [ ] Subdominio finsight.jbyanx.dev apuntando al frontend
- [ ] Variables de entorno en Railway
- [ ] Monitoreo básico de errores
- [ ] README profesional con badges y demo
- [ ] Code coverage report en CI
- [ ] Hacer repo público
- [ ] Agregar a jbyanx.dev en proyectos destacados

---

## Fase 6 — Asistente financiero inteligente
- [ ] Registro de gastos operativos asociados a fuentes de ingreso
- [ ] Cálculo de ingreso neto real por turno/semana/mes
- [ ] Recomendaciones de compras inteligentes según contexto actual
- [ ] Sugerencias de optimización de gastos operativos
- [ ] Proyección de ahorro alcanzable según ritmo actual
- [ ] Aprendizaje del perfil — sugerencias de actualización de estimados
- [ ] Modo "sin deudas" — redirige inteligencia hacia metas y crecimiento

---

## Backlog — ideas futuras
- Registro y login con 2FA
- Categorización automática de gastos con IA
- Soporte multi-moneda
- Exportar reportes a PDF
- Recordatorios por correo (Spring Mail)
- Versión móvil con React Native
- Integración con APIs bancarias abiertas
