# Reporte Finsight (front)

Lenguaje: espanol neutro con tuteo. Sin color decorativo (solo --safe/--warn/
--danger). Mobile first dentro del contenedor de 480px. Montos con clase .amount.
Todas las peticiones pasan por el cliente `api` (nada de fetch directo).

---

## 1. Navegacion inferior (bottom nav)

**Archivo nuevo:** `src/components/BottomNav.tsx`

- Barra fija al fondo, centrada dentro del ancho de 480px, respetando
  `env(safe-area-inset-bottom)`.
- Un espaciador reserva el alto de la barra para que el contenido no quede tapado.
- Tres destinos: Inicio (`/`), Tarjetas (`/cards`), Telegram (`/telegram`).
- Destino activo en `--ink`, el resto en `--muted` (via `NavLink`, con `end` en
  la raiz para no marcar Inicio en todas las rutas).
- Iconos en SVG inline (`currentColor`), sin librerias de iconos.
- Se monta dentro de `Protected` en `App.tsx`, por lo que aparece solo en las
  pantallas autenticadas y nunca en `/login` ni `/register`.

## 2. Pantalla de tarjetas `/cards`

**Archivo nuevo:** `src/pages/Cards.tsx`

- Ruta protegida `/cards` agregada en `App.tsx`.
- Lista las tarjetas reusando `UtilizationBar` (la marca del 30%) tal cual.
- Boton "Agregar tarjeta" que despliega un formulario en linea (dentro de una
  `.card`), reusando el componente `Field` de `Login.tsx`.
- Estado vacio con mensaje claro para el usuario nuevo.
- Manejo de `ApiError` mostrando el mensaje del backend.
- Al crear con exito, la lista se refresca en memoria (sin recargar la pagina):
  la tarjeta devuelta se agrega al estado.

**Validacion en el front (espeja `CreditCardRequest.java`):**
- `institution`: requerido, no vacio.
- `nickname`: opcional (se envia "" si va vacio).
- `creditLimit`: numero >= 0.01.
- `cutOffDay` y `paymentDueDay`: enteros entre 1 y 31.
- El boton de guardar queda deshabilitado hasta que todo cumple.

**Contrato usado (verificado en backend):**
- `GET  /api/v1/cards` -> `CreditCard[]`.
- `POST /api/v1/cards` body `{ institution, nickname, creditLimit, cutOffDay,
  paymentDueDay }` -> `CreditCard` completo.

## Cambios en pantallas existentes

- `src/App.tsx`: rutas protegidas `/cards`; `BottomNav` dentro de `Protected`.
- `src/pages/Dashboard.tsx`: el estado vacio "Todavia no tienes tarjetas. Agrega
  una" ahora es un enlace a `/cards`.

## Nota

En `src/pages/Telegram.tsx` la base del enlace se movio a la constante
`TELEGRAM_BASE` (`https://telegram.me`, porque `t.me` falla en algunas
redes/DNS) y el `telegramUrl` ahora la reutiliza, dejando `tsc` y `oxlint` en
verde.

## Verificacion

- `tsc -b`: en verde (sin errores).
- `oxlint src`: 0 warnings, 0 errors.
