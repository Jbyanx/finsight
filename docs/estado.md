ESTADO DE FINSIGHT — 18/07/2026

HECHO:
- Backend completo: auth (OAuth + local, JWT refresh rotation), CRUD de
  tarjetas/ahorros/deudas/cash, bot Telegram con registro por lenguaje natural
  + confirmación, Liquibase, Swagger, 217 tests, CI.
- Front: login/registro/Google, dashboard, tarjetas, ahorros (bolsillos +
  deposit/withdraw), deudas, vinculación Telegram. Componentes ConfirmDialog,
  InlineAlert, errorMessage. Estados activos en botones.
- Modelo de dominio: bolsillos que particionan el balance (tipo Nu), dinero de
  terceros que no cuenta como propio.

PENDIENTES (retomar con: rama por issue desde develop, PR con closes #N):
1. Reorganizar nav (menú "Más"; está en 5 ítems con un TODO en BottomNav).
2. UI de módulos sin pantalla: ingresos, gastos operativos, facturas.
3. DECISIÓN DE DOMINIO: pago de deuda desde la web debe descontar de una fuente
   (cuenta/efectivo), como ya hace el bot (filosofía B). Requiere cambio backend.
4. DECISIÓN DE DOMINIO: deudas informales sin cuota (préstamo familiar).
   monthlyPayment hoy es obligatorio; evaluar hacerlo opcional.
5. Issues Fase 3 abiertos (NO están hechos, verificado): #56 resumen mensual +
   pre-corte, #57 alertas de facturas próximas (scheduled), #58 cobertura jacoco.
6. Deploy: hosting en evaluación (Railway descartado; Fly/Azure rebotaron por
   cuenta). Bases ya configuradas: Neon (PG), Atlas (Mongo), Upstash (Redis).
7. Hardening futuro: optimistic locking (@Version) para escrituras web+bot
   concurrentes; migrar refresh token a cookie httpOnly.

CONTRATO CLAVE que suelo olvidar: el refresh va en header X-Refresh-Token, sin body.
