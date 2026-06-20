# Finsight — Conventions

## Git

### Branches
- El número en la rama corresponde al número del ISSUE, no del PR
- Los PRs tienen números propios que no se usan en nombres de ramas
```
main          → producción, protegida
develop       → integración, protegida
feature/14-configurar-ambientes
bugfix/15-calculo-utilizacion
hotfix/16-token-expiry
```

### Commits — Conventional Commits
```
feat(tarjeta): agregar endpoint de creación
fix(auth): corregir expiración de token JWT
docs(readme): actualizar instrucciones
test(transaccion): agregar tests unitarios
refactor(usuario): extraer lógica de validación
chore(deps): actualizar dependencias
perf(cache): optimizar TTL de Redis
ci(actions): agregar step de coverage
```
Formato: `tipo(scope): descripción en minúscula, imperativo, sin punto final`

### Pull Requests
- Título igual al commit principal
- Descripción: qué hace, por qué, cómo probarlo
- `Closes #42` en la descripción para cerrar el issue automáticamente
- Mínimo un reviewer (en este caso, auto-review con checklist)
- No mergear con tests fallando

---

## Java — Backend

### Nombrado

```java
// Paquetes — lowercase por dominio
com.jbyanx.finsight.domain.tarjeta
com.jbyanx.finsight.domain.transaccion

// Clases — PascalCase
TarjetaService
TransaccionRepository

// Variables y métodos — camelCase
private String numeroTarjeta;
public BigDecimal calcularUtilizacion()

// Constantes — UPPER_SNAKE_CASE
public static final int UTILIZACION_IDEAL = 30;
public static final int ALERTA_AMARILLA = 25;
```

### Clases por dominio

```java
// Entidad — nombre del dominio sin sufijo
Tarjeta.java
Transaccion.java
Usuario.java

// DTOs — sufijo según dirección
TarjetaDTO.java               // genérico
CrearTarjetaRequest.java      // entrada
EstadoTarjetaResponse.java    // salida

// Servicios — sufijo Service
TarjetaService.java
UtilizacionService.java

// Repositorios — sufijo Repository
TarjetaRepository.java

// Controladores — sufijo Controller
TarjetaController.java

// Excepciones — sufijo Exception
TarjetaNotFoundException.java
UtilizacionExcedidaException.java
```

### Estructura de paquetes

```
com.jbyanx.finsight/
├── domain/
│   ├── usuario/
│   ├── tarjeta/
│   ├── transaccion/
│   ├── cuenta/
│   ├── bolsillo/
│   └── deuda/
├── infrastructure/
│   ├── config/
│   ├── security/
│   └── external/
│       ├── gemini/
│       └── telegram/
└── shared/
    ├── exception/
    └── util/
```

### Endpoints — REST

```
GET    /api/v1/tarjetas
POST   /api/v1/tarjetas
GET    /api/v1/tarjetas/{id}
PUT    /api/v1/tarjetas/{id}
DELETE /api/v1/tarjetas/{id}
GET    /api/v1/tarjetas/{id}/transacciones
POST   /api/v1/tarjetas/{id}/transacciones
GET    /api/v1/usuarios/{id}/estado-financiero
```
Reglas: kebab-case, plural, sustantivos, versionados con `/api/v1/`

### Tests

```java
// Nombre — dado_cuando_entonces
@Test
void dadaTarjetaExistente_cuandoCalcularUtilizacion_entoncesRetornaValorCorrecto();

// O en inglés
@Test
void givenExistingTarjeta_whenCalculateUtilizacion_thenReturnsCorrectValue();
```

- Un test por comportamiento, no por método
- Mínimo 80% de coverage en servicios
- Tests unitarios para servicios
- Tests de integración para controladores

---

## Variables de entorno

### Formato

```bash
# UPPER_SNAKE_CASE con prefijo del servicio
DB_HOST=localhost
DB_PORT=5432
DB_NAME=finsight
DB_USERNAME=finsight_user
DB_PASSWORD=

MONGODB_URI=
REDIS_HOST=localhost
REDIS_PORT=6379

JWT_SECRET=
JWT_EXPIRATION_MS=86400000

GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

GEMINI_API_KEY=
TELEGRAM_BOT_TOKEN=
TELEGRAM_BOT_USERNAME=
```

### Reglas
- Nunca hardcodear valores en el código
- Nunca subir `.env` al repo
- `.env.example` siempre actualizado con cada variable nueva
- En Railway: configurar como variables de entorno del servicio
- En GitHub Actions: configurar como GitHub Secrets

---

## Coverage mínimo por fase
- Fase 1: 0.40 ✅ alcanzado
- Fase 2: 0.35 ✅ alcanzado (services + integration tests de Card, Debt, Transaction, Financial)
- Fase 3+: meta 0.60, subir incrementalmente
---

## React — Frontend

### Nombrado

```
Componentes — PascalCase
  TarjetaCard.jsx
  EstadoFinanciero.jsx
  BolsilloProgress.jsx

Hooks — camelCase con prefijo use
  useTarjetas.js
  useTransacciones.js
  useEstadoFinanciero.js

Carpetas — kebab-case
  tarjeta-card/
  estado-financiero/

Variables de entorno
  VITE_API_URL=http://localhost:8080
  VITE_GOOGLE_CLIENT_ID=
```

---

## Base de datos

### PostgreSQL — tablas

```sql
-- snake_case, plural
usuarios
tarjetas_credito
cuentas_ahorro
bolsillos
transacciones
deudas_fijas
gastos_operativos
```

### MongoDB — colecciones

```
camelCase, plural
  conversacionesBot
  sugerenciasIA
  logsTransacciones
```

### Redis — keys

```
prefijo:identificador
  tarjeta:estado:{userId}:{tarjetaId}
  usuario:sesion:{userId}
  usuario:perfil:{userId}
```

---

## Seguridad
- Nunca loggear datos financieros en producción
- Nunca retornar stack traces al cliente
- Todo endpoint protegido excepto `/auth/**` y `/health`
- El sistema nunca modifica datos del usuario sin confirmación explícita
- Los datos de un usuario nunca son accesibles por otro usuario

---

## Docker

```
Nombres de servicios — kebab-case
  finsight-postgres
  finsight-mongodb
  finsight-redis
  finsight-backend
  finsight-frontend
```
