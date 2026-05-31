# Finsight — Roadmap

## Estado actual: Fase 1 — Base sólida

---

## Fase 1 — Base sólida
- [ ] Estructura del monorepo
- [ ] Docker Compose local (PostgreSQL + MongoDB + Redis)
- [ ] Setup Spring Boot con dependencias base
- [ ] Configuración de ambientes (dev, prod)
- [ ] Variables de entorno con `.env.example`
- [ ] OAuth2 con Google
- [ ] JWT para el bot
- [ ] Modelo de datos completo
- [ ] Migraciones iniciales con Liquibase
- [ ] GitHub Actions CI básico (build + test en cada PR)
- [ ] GitHub Projects configurado con tablero Kanban

---

## Fase 2 — Core financiero
- [ ] CRUD de usuarios
- [ ] CRUD de tarjetas de crédito
- [ ] CRUD de cuentas de ahorro
- [ ] CRUD de bolsillos
- [ ] Registro de transacciones
- [ ] Cálculo de utilización de tarjetas en tiempo real
- [ ] Separación de gastos propios vs compartidos
- [ ] Alertas de utilización (25% y 30%)
- [ ] Plan de pagos post-corte
- [ ] CRUD de deudas fijas
- [ ] Pipeline de ahorro hacia metas
- [ ] Tests unitarios de servicios (mínimo 80% coverage)
- [ ] Tests de integración de endpoints principales

---

## Fase 3 — Bot de Telegram
- [ ] Setup del bot con BotFather
- [ ] Integración Spring Boot + Telegram API
- [ ] Comando: registrar transacción por mensaje natural
- [ ] Comando: consultar estado de tarjeta
- [ ] Comando: crear cuenta o bolsillo
- [ ] Comando: consultar resumen financiero
- [ ] Comando: registrar ingreso
- [ ] Autenticación del usuario via Telegram
- [ ] Tests del bot

---

## Fase 4 — Spring AI + Gemini
- [ ] Setup Spring AI
- [ ] Integración con Gemini API
- [ ] Contexto financiero del usuario como prompt
- [ ] "¿Con qué tarjeta pago hoy?" → respuesta inteligente
- [ ] Resumen mensual generado por IA
- [ ] Recomendaciones pre-corte automáticas
- [ ] Historial de conversaciones en MongoDB
- [ ] Tests de integración con IA

---

## Fase 5 — React PWA
- [ ] Setup React + Vite
- [ ] Autenticación con Google
- [ ] Dashboard — estado financiero en tiempo real
- [ ] Vista de tarjetas y utilización
- [ ] Vista de cuentas y bolsillos
- [ ] Historial de transacciones con filtros
- [ ] Registro manual de transacciones
- [ ] Vista de deudas fijas y plan de pagos
- [ ] Notificaciones push (alertas de utilización)
- [ ] Diseño responsive — mobile first
- [ ] PWA — installable en Android

---

## Fase 6 — Polish y producción
- [ ] Flyway para migraciones en producción
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

## Fase 7 — Asistente financiero inteligente
- [ ] Registro de gastos fijos mensuales (arriendo, servicios, etc)
- [ ] Registro de gastos operativos recurrentes (gasolina semanal, merienda por turno)
- [ ] Asociar costos a fuentes de ingreso ("cada turno cuesta X en gasolina y merienda")
- [ ] Cálculo de ingreso neto real por turno/semana/mes
- [ ] Recomendaciones de compras inteligentes según contexto actual
- [ ] "Tienes 3 turnos esta semana, presupuesta X para gasolina y Y para merienda"
- [ ] Sugerencias de optimización de gastos operativos
- [ ] Proyección de ahorro alcanzable según ritmo actual
- [ ] Modo "sin deudas" — redirige toda la inteligencia hacia metas y crecimiento

---

## Backlog — ideas futuras
- Categorización automática de gastos con IA
- Soporte multi-moneda
- Exportar reportes a PDF
- Recordatorios por correo además de Telegram
- Versión móvil con React Native
- Integración con APIs bancarias abiertas