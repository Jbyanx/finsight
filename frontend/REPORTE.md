# Reporte — normalización de español y estado del front

Fecha: 2026-07-14

## 1. Qué se hizo

Barrido de voseo y léxico rioplatense sobre todo `frontend/src` + `index.html`.
Once correcciones en seis archivos, tanto en texto visible como en comentarios.
El proyecto queda en **español neutro con tuteo** (`tienes`, `puedes`, `registra`).

| Archivo | Antes | Después |
| --- | --- | --- |
| `src/pages/Login.tsx` | `Sabé con qué pagar hoy.` | `Sabes con qué pagar hoy.` |
| `src/pages/Login.tsx` | `Registrá un gasto` | `Registra un gasto` |
| `src/pages/Login.tsx` | `Al entrar aceptás` | `Al entrar aceptas` |
| `src/pages/Dashboard.tsx` | `no tenés tarjetas. Agregá una` | `no tienes tarjetas. Agrega una` |
| `src/pages/AuthCallback.tsx` | `inicio de sesion` | `inicio de sesión` (faltaba la tilde) |
| `src/lib/api.ts` | `usás` / `refrescá` (comentarios) | `usas` / `refresca` |
| `src/lib/money.ts`, `src/components/UtilizationBar.tsx`, `src/pages/AuthCallback.tsx` | `acá` (comentarios) | `aquí` |

### Decisión de redacción

El titular del login era `Sabé con qué pagar hoy.` (imperativo voseante). El
imperativo tuteante equivalente —`Sabe con qué pagar hoy`— suena forzado en
español neutro, así que se pasó a declarativo: **`Sabes con qué pagar hoy.`**
Mantiene la promesa del producto y suena natural. Alternativa si se prefiere el
imperativo: `Descubre con qué pagar hoy.`

### Falso positivo

`Te estás acercando al 30%` (`Dashboard.tsx`) **no** es voseo: es tuteo correcto
(`tú estás`). Se dejó como estaba.

## 2. Verificación

```
grep de voseo   → sin coincidencias
tsc -b          → OK
oxlint src      → 10 archivos, 0 errores, 0 advertencias
```

## 3. Base existente (no reinventar)

Estos módulos ya existen y funcionan. Todo lo nuevo debe apoyarse en ellos.

- **`src/lib/api.ts`** — cliente HTTP. Implementa *refresh token rotation* con un
  único refresh en vuelo (`refreshOnce()`): si varias peticiones dan 401 a la vez,
  todas se cuelgan de la misma promesa y se reintentan con el token nuevo. Expone
  `api.get/post/patch/del`, `tokens` (localStorage), `ApiError` y `setOnSessionLost`.
  **Ninguna petición debe usar `fetch` directamente.**
- **`src/auth/AuthContext.tsx`** — `useAuth()` expone `user`, `loading`, `logout`, `reload`.
- **`src/lib/money.ts`** — `money()` (formato `$200.000`, es-CO) y `healthOf()`.
- **`src/components/UtilizationBar.tsx`** — la marca del 30%, elemento firma.
- **`src/index.css`** — sistema de diseño. Sin color decorativo: solo `--safe`,
  `--warn`, `--danger`. Layout móvil de 480px. Los montos siempre con `.amount`.

## 4. Estado del router

`App.tsx` solo tiene tres rutas: `/login`, `/auth/callback` y `/` (Dashboard).

Los tipos de `src/types.ts` ya cubren `SavingsAccount`, `Pocket`, `CreditCard` y
`FinancialSummary`, pero **no hay pantalla para ahorros ni bolsillos**, y el
mensaje vacío del dashboard (`Agrega una`) todavía no lleva a ningún lado.

## 5. Siguiente paso sugerido

1. **Cuentas de ahorro + bolsillos** — los DTOs existen y no tienen ruta.
2. **Alta/edición de tarjetas** — para que el estado vacío del dashboard
   tenga destino.
