# Estado de Finsight

> Documento vivo. Se actualiza al final de cada sesión de trabajo.
> Sirve para retomar el proyecto sin depender de la memoria de nadie.
>
> **Última actualización:** 2026-07-18

---

## Cómo se trabaja

1. `git checkout develop && git pull`
2. Rama desde develop por issue: `feature/NN-descripcion` o `fix/NN-descripcion`
3. Commits separados por intención (no mezclar fix de comportamiento con limpieza)
4. PR a develop con `closes #NN`
5. Volver a develop actualizado antes de la siguiente tarea

**Regla de oro:** verificar los contratos en Swagger antes de tipar nada en el
front. No asumir nombres de campos.

**Contrato que se olvida seguido:** el refresh token va en el header
`X-Refresh-Token`, sin body.

---

## Hecho

### Backend (Spring Boot 3.5 / Java 21) — 221 tests
- **Auth:** OAuth2 Google + login/registro local. JWT con access de 15 min y
  refresh de 30 días con *rotation*. Multi-dispositivo, `/sessions`, `/logout-all`.
- **Dominios con CRUD:** tarjetas, cuentas de ahorro (con bolsillos), deudas,
  efectivo, transacciones, facturas, ingresos, gastos operativos.
- **Modelo de ahorros tipo Nu:** el `balance` de la cuenta es el total; los
  bolsillos *reservan* parte de ese total (no suman aparte).
  `disponible = balance − Σ(bolsillos activos)`. Endpoints `deposit` y `withdraw`
  que mueven plata entre disponible y reservado sin alterar el total.
- **Dinero de terceros:** `shared`/`sharedOwnerName` viven en el **bolsillo**, no
  en la cuenta. `ownBalance` excluye lo ajeno. Una transacción puede pagarse desde
  un bolsillo compartido (`pocket_id` + `third_party`) sin afectar el patrimonio
  del usuario.
- **Pago de deuda con fuente real:** `applyPayment` acepta `originType`,
  `savingsAccountId` y `pocketId` **opcionales**. Con fuente, delega en
  `TransactionService.create()` (que ya valida disponible y maneja bolsillos) y
  deja la operación trazable como `Transaction`. Solo CASH y SAVINGS.
  La fuente se descuenta **antes** de tocar la deuda, en la misma `@Transactional`.
- **Bot de Telegram** (long polling, hilo único): consultas en lenguaje natural y
  registro de gastos/pagos con *preview → botón de confirmación → ejecución en
  código*. El modelo nunca ejecuta una escritura directamente.
- **Vinculación de cuenta:** códigos de 6 caracteres con expiración, 1:1 estricto
  usuario↔chat, `/unlink` que purga también la conversación de Mongo.
- **Infra:** Liquibase (schema versionado, `ddl-auto=validate`), Swagger con
  candado JWT, CI en GitHub Actions, Dockerfile multi-stage.
- **Errores HTTP correctos:** las reglas de negocio devuelven 400
  (`InsufficientFundsException`, `BusinessRuleException`); el 401 quedó reservado
  para autenticación real. Verificado: no queda ningún caso de regla de negocio
  devolviendo 401.

### Frontend (React 18 + TS + Vite, PWA mobile-first)
- Login local, registro y OAuth Google. Cliente HTTP con **un solo refresh en
  vuelo** (`refreshOnce`) para no quemar el refresh token con peticiones paralelas.
- Dashboard: dinero usado como titular, % como contexto, disponible visible.
- Pantallas con CRUD completo: tarjetas, ahorros (cuentas + bolsillos + agregar/
  retirar), deudas (con abonos).
- Vinculación de Telegram con deep link (`telegram.me`, porque `t.me` falla en
  algunos DNS).
- Componentes transversales: `ConfirmDialog`, `InlineAlert`, `errorMessage`,
  `UtilizationBar` (la marca del 30%), `GoalBar` (progreso de meta).
- Sistema de diseño: sin color decorativo — el único color es el de la salud
  financiera. Montos siempre en mono con cifras tabulares.

---

## Pendiente

### Inmediato (front)
- [ ] **Traducir los mensajes de error del backend** en `errorMessage.ts`. Ahora
      que las reglas de negocio llegan bien a la UI (antes se perdían en el
      refresh), el usuario ve textos en inglés: saldo disponible insuficiente,
      saldo de bolsillo insuficiente, cupo excedido, pagar deuda con tarjeta,
      falta `savingsAccountId`, bolsillo inactivo, `pocketId` con origen inválido,
      factura ya pagada, depósito/retiro fuera de rango.
- [ ] **Selector de fuente en el pago de deuda** (el backend ya lo soporta).
- [ ] Verificar que los errores 400/409 se muestren con su mensaje y no con un
      genérico.

### Inmediato (bot)
- [ ] `DebtWriteTools`: entender "pagué 500 mil de la moto **desde Nequi**" y pasar
      la fuente al service.
- [ ] `DebtWriteTools` sigue con **voz argentina** ("Sugerí", "¿Confirmás?") y
      montos crudos sin `Money.format()`. Alinear con el resto.

### UI de módulos que ya tienen endpoints
- [ ] Ingresos y gastos operativos
- [ ] Facturas recurrentes
- [ ] Historial de movimientos por cuenta/bolsillo (botón de reloj)

### Navegación
- [ ] Reorganizar el nav: está en 5 destinos y satura en móvil. Evaluar un quinto
      ítem "Más" con los módulos secundarios. Hay un `// TODO` en `BottomNav.tsx`.

### Decisiones de dominio abiertas
- [ ] **Deudas informales sin cuota mensual.** Hoy `monthlyPayment` es obligatorio
      (mínimo 0.01), pero un préstamo familiar ("le debo a mi tía") no tiene cuota
      fija. Evaluar hacer `monthlyPayment` e `interestRate` opcionales.

### Issues abiertos de Fase 3 (verificado: NO están hechos)
- [ ] #56 — resumen mensual por IA + recomendaciones pre-corte (scheduled)
- [ ] #57 — alertas inteligentes de facturas próximas (scheduled)
- [ ] #58 — suite de Fase 3 y subir umbral de jacoco

> El único `@Scheduled` que existe es el de limpieza de tokens. No hay ningún
> envío proactivo del bot.

### Deploy
- [ ] **Hosting sin resolver.** Railway pide plan pago; Render no tiene MongoDB y
      duerme la instancia (mata el long polling del bot); Fly marcó la cuenta como
      *high risk* y su Postgres gestionado arranca en 38 USD; Azure for Students se
      quedó sin crédito.
- [x] Bases de datos ya configuradas y gratuitas: **Neon** (PostgreSQL),
      **MongoDB Atlas** (M0), **Upstash** (Redis).
- [ ] Falta solo el *compute*. Candidatos a evaluar: Koyeb, Northflank.
- [ ] Requisito no negociable: la instancia **no puede dormirse**, o el bot
      (long polling) deja de responder.
- [ ] Al desplegar: bot nuevo en BotFather (dos instancias con el mismo token se
      roban los mensajes), `JWT_SECRET` nuevo, y agregar la redirect URI de
      producción en Google Cloud Console.

### Deuda técnica
- [ ] **Jerarquía de excepciones de dominio.** Hoy cada excepción es una
      `RuntimeException` suelta con su handler propio. Una `DomainException` base
      con su `HttpStatus` asociado dejaría el status junto a la excepción — que es
      exactamente la causa raíz del bug del 401.
- [ ] **Optimistic locking (`@Version`)** en `Cash`, `CreditCard` y
      `SavingsAccount`. Web y bot pueden escribir el mismo `userId` a la vez.
      Mitigado hoy porque las escrituras son `@Transactional` y se revalida al
      ejecutar, pero el *lost update* sigue siendo posible.
- [ ] **`spring.jpa.open-in-view=true`** (por defecto). Enmascara
      `LazyInitializationException` en desarrollo y los hace explotar solo desde
      el hilo del bot (donde no hay request). Desactivarlo **en su propia rama**:
      puede destapar otros lazy loads dormidos.
- [ ] **`fixed_debts.interest_rate` es `numeric(38,2)`** — le falta
      `precision`/`scale` en la entidad. Corregir en un changeset nuevo, nunca
      editando el `001-initial-schema.xml`.
- [ ] **Refresh token en `localStorage`.** Lo estándar para un SPA, pero lo más
      seguro sería cookie `httpOnly` + access token solo en memoria. Migrar si la
      app crece a más usuarios.
- [ ] Prueba de contrato que verifique que ningún endpoint de negocio devuelve 401
      salvo por token ausente o inválido (evita la regresión de raíz).

### Backlog (post v1)
- [ ] Voucher por foto: subir la imagen de la factura a un bucket y asociarla a la
      transacción (`voucherUrl` nullable).
- [ ] Retención de vouchers: borrar imágenes de compras de 1 cuota tras 6 meses;
      conservar las de N cuotas hasta que terminen. Evaluar 1 año si se agrega
      soporte para declaración de renta (DIAN).
- [ ] Personalidad del asistente configurable por usuario.
- [ ] Notificaciones push en la PWA.

---

## Decisiones de dominio ya tomadas (no volver a discutir)

| Decisión | Razón |
|---|---|
| El `balance` de una cuenta es el **total**; los bolsillos particionan, no suman | Es como funciona Nequi/Nu en la vida real |
| `shared` vive en el **bolsillo**, no en la cuenta | Una cuenta propia puede alojar plata de la familia; marcar la cuenta entera borraba también el dinero propio |
| La fuente del pago de deuda es **opcional** | A veces paga un tercero desde una cuenta que no está en la app; el sistema no debe fallar por eso |
| Solo CASH y SAVINGS pagan deudas | Pagar deuda con tarjeta mueve la deuda de lugar en vez de saldarla |
| El pago con fuente **genera una `Transaction`** | Sin rastro del movimiento, la plata "desaparece" y se pierde la confianza |
| Las escrituras del bot pasan **siempre** por confirmación con botón | El modelo propone; el código ejecuta. Nunca al revés |
| El DELETE es *soft delete* | Las transacciones asociadas se conservan |
| Recurso ajeno devuelve **404**, no 403 | No filtrar la existencia de recursos de otros usuarios |
