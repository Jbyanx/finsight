# Reporte Finsight (front)

Lenguaje: espanol neutro con tuteo. Sin color decorativo (solo --safe/--warn/
--danger). Mobile first dentro del contenedor de 480px. Montos con clase .amount.
Todas las peticiones pasan por el cliente `api` (nada de fetch directo).

---

## 1. FIX critico: el refresh de token estaba roto

**Archivo:** `src/lib/api.ts`

El backend espera el refresh token en un HEADER, no en el body. Verificado en
`AuthController#refresh`:

```java
@PostMapping("/refresh")
public ResponseEntity<AuthResponse> refresh(
        @RequestHeader("X-Refresh-Token") String refreshToken, ...)
```

`refreshTokens()` lo enviaba en el body `{ refreshToken }` -> el backend respondia
500 -> el usuario quedaba deslogueado en cada vencimiento del access token.

- Ahora se envia el token en el header `X-Refresh-Token`, con **body vacio**.
- Se quito el `Content-Type: application/json` (ya no hay cuerpo).
- No cambia la logica de "un solo refresh en vuelo" (rotacion de token intacta).

**Contrato usado (verificado en backend):**
- `POST /api/v1/auth/refresh` header `X-Refresh-Token: <token>`, sin body ->
  `AuthResponse`.

## 2. FIX: la rueda del mouse alteraba los inputs numericos

**Archivo:** `src/pages/Login.tsx` (componente compartido `Field`)

En `<input type="number">`, al hacer scroll por la pantalla con el input enfocado
se cambiaban saldos sin querer. Se suelta el foco antes de que la rueda actue:

```tsx
onWheel={type === 'number' ? (e) => e.currentTarget.blur() : undefined}
```

Como `Field` es el input compartido de toda la app, el arreglo cubre todos los
campos numericos (cuentas, bolsillos, tarjetas, montos).

## 3. FEATURE: agregar / retirar de bolsillos (experiencia Nu)

**Archivo:** `src/pages/Savings.tsx`

Dentro de cada bolsillo, dos acciones **Agregar** y **Retirar** que abren un input
de **MONTO** (no un total: el usuario piensa "agrego $50.000"), via el nuevo
componente `PocketAmountForm`.

- Submit deshabilitado si el monto es `<= 0` (o no numerico).
- Al confirmar se llama al endpoint y **despues** se re-fetchea la cuenta con
  `GET /api/v1/savings/{accountId}` (`refreshAccount` -> upsert): el endpoint de
  bolsillo devuelve solo el `Pocket`, no los agregados de la cuenta (`ownBalance`,
  `availableBalance`, `sharedTotal`), asi que hay que recargar la cuenta para
  refrescar el disponible.
- El error del backend se muestra tal cual via `ApiError` (ej. "no tienes tanto
  disponible" al depositar, o saldo insuficiente al retirar).
- Solo una operacion abierta a la vez por bolsillo; los botones se deshabilitan
  mientras hay una en curso.

**Contratos usados (verificados en backend):**
- `POST /api/v1/savings/{accountId}/pockets/{pocketId}/deposit` body
  `{ amount: number }` -> `Pocket`. El backend exige `amount <= disponible` de la
  cuenta.
- `POST /api/v1/savings/{accountId}/pockets/{pocketId}/withdraw` body
  `{ amount: number }` -> `Pocket`. El backend exige `amount <= saldo` del
  bolsillo.
- Ninguno cambia el balance total de la cuenta; solo mueven dinero entre el
  disponible y el bolsillo.
- `PocketAmountRequest`: campo `amount` (BigDecimal, `> 0`).

## Integracion de la pantalla de ahorros

- `src/App.tsx`: ruta protegida `/savings`.
- `src/components/BottomNav.tsx`: cuarto destino "Ahorros" (`/savings`) con icono
  SVG inline.
- `src/types.ts`: `shared` y `sharedOwnerName` agregados a `SavingsAccount` (el
  PATCH de cuenta los necesita).

## Verificacion

- `tsc -b`: en verde (sin errores).
- `oxlint src`: 0 warnings, 0 errors.
