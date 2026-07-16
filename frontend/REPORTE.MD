# Reporte Finsight (front)

Lenguaje: espanol neutro con tuteo. Sin color decorativo (solo --safe/--warn/
--danger). Mobile first dentro del contenedor de 480px. Montos con clase .amount.
Todas las peticiones pasan por el cliente `api` (nada de fetch directo).

---

## Desglose de la cabecera de cada cuenta de ahorro

**Archivo:** `src/pages/Savings.tsx`

**Problema:** la tarjeta mostraba "Tuyo" (`ownBalance`) y el balance total, pero
no el **Disponible** (`availableBalance`), que es la cifra que el usuario necesita:
cuanto le queda libre despues de lo apartado en bolsillos. Una cuenta de
$1.000.000 con $300.000 en bolsillos seguia mostrando $1.000.000 arriba y el
usuario no veia a donde fue su plata.

**Solucion:** la cabecera muestra ahora el desglose completo con jerarquia clara:

- **Disponible** (`availableBalance`) — DESTACADO, cifra protagonista (28px, peso
  800). Lo que queda libre, ni apartado en bolsillos ni de terceros.
- **Balance total** (`balance`) — linea de contexto.
- **Apartado en bolsillos** = `balance - availableBalance`, calculado en el front.
  Solo se muestra si es `> 0`.
- **De terceros** (`sharedTotal`) — atenuado, solo si es `> 0`.

Se agrego el helper `InfoRow` (rotulo + monto, con `muted` opcional) para las
lineas de contexto.

**Invariante en pantalla:** por construccion `Disponible + Apartado en bolsillos =
Balance total`, ya que `apartado = balance - availableBalance`. Cuadra siempre.

Los campos usados ya venian tipados en `src/types.ts` (`SavingsAccount`):
`balance`, `ownBalance`, `sharedTotal`, `availableBalance`.

## Refresco tras deposit / withdraw

El usuario reportaba que los numeros no se refrescaban tras agregar/retirar. La
causa raiz era la misma que arriba: `deposit`/`withdraw` mueven dinero **entre el
disponible y un bolsillo**, sin tocar `balance`, `ownBalance` ni `sharedTotal`.
La cabecera mostraba justamente esos campos invariantes, asi que el re-fetch si
re-renderizaba, pero las cifras visibles no cambiaban.

- El re-fetch (`refreshAccount` -> `GET /api/v1/savings/{accountId}` -> `upsert`)
  ya era correcto: reemplaza la cuenta por `id` en un array nuevo, forzando el
  re-render. No se toco.
- Al mostrar ahora `availableBalance` (que si cambia) y `Apartado en bolsillos`,
  la actualizacion es visible de inmediato.

No se toco el cliente `api` ni la logica de deposit/withdraw.

## Verificacion

- `tsc -b`: en verde (sin errores).
- `oxlint src`: 0 warnings, 0 errors.
