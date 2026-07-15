# Reporte — Dominio Savings: validación de bolsillos + operaciones deposit/withdraw

## Contexto

En el modelo tipo Nu/Nequi, el `balance` de una `SavingsAccount` es el dinero
**total** de la cuenta. Un bolsillo (`Pocket`) **reserva** parte de ese total; no
crea dinero nuevo. El cálculo correcto ya vivía en la entidad:

```
getAvailableBalance() = balance - suma(bolsillos activos)
```

Esos métodos de cálculo (`getAvailableBalance`, `getOwnBalance`, `getSharedTotal`)
**no se tocaron**.

## Bug corregido

`createPocket` no validaba nada: se podía crear un bolsillo de $300.000 en una
cuenta de $100.000, dejando los números inconsistentes.

## Cambios realizados

### 1. Validación al crear bolsillo
`SavingsAccountServiceImpl.createPocket` ahora rechaza un saldo inicial que supere
`account.getAvailableBalance()`, lanzando `UnauthorizedException` (el mismo tipo que
el service ya usaba para reglas de negocio, p. ej. en `patchPocket`). El mensaje
incluye el disponible real.

### 2. Endpoints depositar / retirar
Experiencia tipo Nu: el usuario piensa en "agregar $50.000", no en "dejar $X en total".

| Método | Ruta | Regla |
|--------|------|-------|
| `POST` | `/api/v1/savings/{accountId}/pockets/{pocketId}/deposit` | `amount > 0` y `amount <= disponible de la cuenta` |
| `POST` | `/api/v1/savings/{accountId}/pockets/{pocketId}/withdraw` | `amount > 0` y `amount <= saldo del bolsillo` |

Body: `{ "amount": <BigDecimal> }`. Ambos devuelven el `Pocket` actualizado.

**Invariante clave:** ni deposit ni withdraw modifican `account.balance`. Solo mueven
plata entre "disponible" y "reservado en bolsillo"; el total de la cuenta no cambia
al operar bolsillos.

### 3. DTO del body
`PocketAmountRequest`: un único campo `amount` con `@NotNull` y
`@DecimalMin(value = "0", inclusive = false)` (equivalente a `amount > 0`), siguiendo
el estilo de los DTOs existentes.

### 4. Multi-tenant
Todo scoped por `userId`, vía `findAccountBelongingToUser` +
`pocketRepository.findByIdAndSavingsAccountId`, igual que el resto del service.

### 5. Swagger / OpenAPI
El proyecto no usa anotaciones `@Operation`/`@Tag` en los controllers; springdoc
auto-genera la documentación. Los endpoints nuevos quedan documentados
automáticamente, sin necesidad de anotar nada.

## Archivos

| Archivo | Cambio |
|---------|--------|
| `domain/savings/dto/PocketAmountRequest.java` | **Nuevo** — DTO del body |
| `domain/savings/service/SavingsAccountService.java` | Firmas `depositToPocket` / `withdrawFromPocket` |
| `domain/savings/service/impl/SavingsAccountServiceImpl.java` | Validación en `createPocket` + implementación deposit/withdraw |
| `domain/savings/controller/SavingsAccountController.java` | Endpoints `/deposit` y `/withdraw` |
| `test/.../SavingsAccountServiceImplTest.java` | 6 tests nuevos |

## Tests

Se agregaron 6 tests siguiendo el estilo del dominio (Mockito + `ReflectionTestUtils`):

- Crear bolsillo que excede el disponible → rechazado.
- Depositar que excede el disponible → rechazado.
- Retirar más que el saldo del bolsillo → rechazado.
- Depositar feliz → el disponible se ajusta y `account.balance` NO cambia.
- Retirar feliz → el disponible se ajusta y `account.balance` NO cambia.
- Caso completo: cuenta $1.000.000, bolsillo "Viaje", verificando en cada paso que
  `disponible = balance - suma(bolsillos)`.

> Nota de testing: el `Pocket` creado por `createPocket` queda con `active=false` bajo
> mocks porque `@PrePersist` no se ejecuta sin persistencia real (en la base sí se marca
> `active=true`). En el test de ciclo completo se setea explícitamente para simular ese
> comportamiento de JPA.

**Resultado:** `./mvnw test` en verde — 215 tests, 0 fallos.
