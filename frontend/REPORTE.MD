# Reporte Finsight (front)

Lenguaje: espanol neutro con tuteo. Sin color decorativo (solo --safe/--warn/
--danger). Mobile first dentro del contenedor de 480px. Montos con clase .amount.
Todas las peticiones pasan por el cliente `api` (nada de fetch directo).

---

## Auditoria de documentacion (2026-07-16)

Se re-sincronizo la documentacion del proyecto contra el codigo real (solo docs,
sin tocar codigo):
- `docs/roadmap.md`: reescrito con checkboxes precisos (hecho vs pendiente),
  desglose de pendientes (UI / backend / deuda tecnica) y nueva seccion
  "Decisiones de dominio pendientes".
- `README.md`: seccion "Implementation status" con los modulos reales.
- `docs/architecture.md`: nota de drift del esquema DBML (falta `interest_rate`
  en `fixed_debts`; campos derivados no son columnas).

Hallazgo: el pago de deuda solo reduce `remainingBalance` tanto en web como en el
bot — ninguno descuenta de una fuente (efectivo/cuenta). La fuente de verdad del
estado del proyecto es ahora `docs/roadmap.md`.

---

## Modulo DEUDAS (pantalla /debts)

**Archivo nuevo:** `src/pages/Debts.tsx`

Una deuda fija (credito) se paga en abonos. El eje del diseno: la deuda es lo
CONTRARIO de una tarjeta. `progressPercentage` mide cuanto se ha PAGADO
(paidAmount / totalAmount); llegar al 100% es BUENO (deuda saldada). Por eso la
barra es verde y celebra el avance — NUNCA el codigo de peligro del 30%.

**Lista de deudas:**
- Cada tarjeta: institucion + descripcion, barra de PROGRESO verde
  (`DebtProgressBar`, modelada como la meta de ahorro `GoalBar`: `--safe`, sin
  umbral de peligro ni color de alerta), saldo restante (`remainingBalance`)
  DESTACADO ("Te falta", 28px) — es lo que el usuario quiere ver — y pagado/total
  de contexto.
- Si `paidOff`: estado claro y positivo. Chip "✓ Saldada" (`--safe`/`--safe-soft`)
  y el titular pasa a "¡Pagaste todo!" en verde. Se oculta el boton de pago.
- Estado vacio claro.

**Registrar pago (patron de Savings deposit/withdraw):**
- `DebtPaymentForm` con input de MONTO ("¿Cuanto quieres abonar?"), no un total.
- `POST /api/v1/debts/{debtId}/payments` devuelve la deuda ACTUALIZADA, asi que se
  usa directo (`upsert`) sin re-fetch adicional.
- Boton "Registrar pago" en estado ACTIVO (relleno --ink) mientras esta abierto;
  el resto se atenua. Mismo patron ya aplicado en Savings y Cards.

**Alta / edicion (formulario unico, patron CardForm):**
- `DebtForm` con prop `embedded`: como card suelta en el alta, embebido dentro de
  la tarjeta en la edicion (con el boton Editar activo).
- En edicion, `institution`, `totalAmount` y `remainingBalance` son texto fijo
  (`ReadOnly`), no inputs — el backend no los deja cambiar. `description` e
  `interestRate` son opcionales.
- Validacion que espeja el backend: `totalAmount` y `monthlyPayment` >= 0.01,
  `paymentDueDay` entre 1 y 31, `remainingBalance` >= 0, `amount` de pago > 0.

**Eliminar / errores:**
- Eliminar con `ConfirmDialog` (tone danger).
- Errores con `InlineAlert` + `errorMessage()` (el mensaje del backend, humanizado).

**Contratos usados (exactos, verificados):**
- `GET    /api/v1/debts` -> `FixedDebt[]`.
- `POST   /api/v1/debts` body `{ institution, description, totalAmount,
  monthlyPayment, interestRate, paymentDueDay, remainingBalance }` -> `FixedDebt`.
- `PATCH  /api/v1/debts/{debtId}` body `{ description, monthlyPayment,
  interestRate, paymentDueDay }` (institution/totalAmount/remainingBalance NO se
  editan) -> `FixedDebt`.
- `POST   /api/v1/debts/{debtId}/payments` body `{ amount }` -> `FixedDebt`.
- `DELETE /api/v1/debts/{debtId}`.

## Tipos y humanizacion

- `src/types.ts`: interfaz `FixedDebt` (id, institution, description, totalAmount,
  monthlyPayment, interestRate, paymentDueDay, remainingBalance, paidAmount,
  progressPercentage, paidOff, createdAt).
- `src/lib/errorMessage.ts`: nuevo patron
  `"Payment amount exceeds remaining balance"` -> "El pago supera lo que te falta
  de la deuda."

## Integracion

- `src/App.tsx`: ruta protegida `/debts`.
- `src/components/BottomNav.tsx`: "Deudas" como 5º destino con icono propio.
  Queda un `// TODO: evaluar menu "Mas" al superar 5 destinos` (aun no construido:
  en movil 5 items empieza a saturar).
- `src/pages/Dashboard.tsx`: la fila "Deuda" (`totalRemainingDebt`) ahora enlaza a
  `/debts` (prop `to` opcional en `Row`, que se rinde como `Link`).

## Verificacion

- `tsc -b`: en verde (sin errores).
- `oxlint src`: 0 warnings, 0 errors.
