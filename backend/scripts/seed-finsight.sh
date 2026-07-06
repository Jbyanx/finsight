#!/usr/bin/env bash
#
# seed-finsight.sh — Puebla el perfil financiero completo del usuario logueado.
#
# REQUISITOS:
#   - jq instalado:  sudo apt install jq
#   - Backend corriendo con ddl-auto=update (para que persista).
#   - Un access token válido (login OAuth). Lo pasás como argumento o variable.
#
# USO:
#   ./seed-finsight.sh "TU_ACCESS_TOKEN"
#   (o export TOKEN=... y luego ./seed-finsight.sh)
#
# Encadena IDs solo: crea cuenta de ahorro -> usa su id para el bolsillo, etc.
# Mete transacciones en las tarjetas para que la utilización NO quede en 0%.

set -euo pipefail

BASE="${BASE_URL:-http://localhost:8080}"
TOKEN="${1:-${TOKEN:-}}"

if [ -z "$TOKEN" ]; then
  echo "ERROR: falta el access token."
  echo "Uso: ./seed-finsight.sh \"TU_TOKEN\"   o   export TOKEN=... && ./seed-finsight.sh"
  exit 1
fi

AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"

# Helper: POST y devuelve el body. Aborta si el HTTP no es 2xx.
post() {
  local path="$1"; local body="$2"
  local resp code
  resp=$(curl -s -w "\n%{http_code}" -X POST "$BASE$path" -H "$AUTH" -H "$CT" -d "$body")
  code=$(echo "$resp" | tail -n1)
  body=$(echo "$resp" | sed '$d')
  if [[ ! "$code" =~ ^2 ]]; then
    echo "  !! POST $path devolvió $code: $body" >&2
    echo ""
    return 1
  fi
  echo "$body"
}

patch() {
  local path="$1"; local body="$2"
  curl -s -X PATCH "$BASE$path" -H "$AUTH" -H "$CT" -d "$body" >/dev/null
}

echo "== Verificando token (GET /me) =="
ME=$(curl -s "$BASE/api/v1/auth/me" -H "$AUTH")
echo "Usuario: $ME"
echo ""

# ----------------------------------------------------------------------
# 1. TARJETAS
# ----------------------------------------------------------------------
echo "== Tarjetas =="
NU=$(post /api/v1/cards '{"institution":"Nu","nickname":"Nu","creditLimit":400000,"cutOffDay":3,"paymentDueDay":23}')
NU_ID=$(echo "$NU" | jq -r '.id')
echo "  Nu: $NU_ID"

RAPPI=$(post /api/v1/cards '{"institution":"RappiCard","nickname":"Rappi","creditLimit":2600000,"cutOffDay":28,"paymentDueDay":10}')
RAPPI_ID=$(echo "$RAPPI" | jq -r '.id')
echo "  Rappi: $RAPPI_ID"

# ----------------------------------------------------------------------
# 2. TRANSACCIONES (para generar utilización real)
#    Nu: 130000 de 400000 -> ~32% (alerta roja, >30%)
#    Rappi: 400000 de 2600000 -> ~15% (safe)
# ----------------------------------------------------------------------
echo "== Transacciones (generan utilización) =="
post /api/v1/transactions "{\"description\":\"Mercado\",\"amount\":130000,\"category\":\"Comida\",\"originType\":\"CREDIT_CARD\",\"creditCardId\":\"$NU_ID\",\"installments\":1}" >/dev/null
echo "  Nu +130000"
post /api/v1/transactions "{\"description\":\"Ropa\",\"amount\":400000,\"category\":\"Compras\",\"originType\":\"CREDIT_CARD\",\"creditCardId\":\"$RAPPI_ID\",\"installments\":3}" >/dev/null
echo "  Rappi +400000 (3 cuotas)"

# Transacción compartida (deuda de la tía en Rappi)
post /api/v1/transactions "{\"description\":\"Compra tía\",\"amount\":200000,\"category\":\"Compartido\",\"originType\":\"CREDIT_CARD\",\"creditCardId\":\"$RAPPI_ID\",\"shared\":true,\"sharedWithName\":\"Tía\",\"sharedAmount\":200000,\"installments\":1}" >/dev/null
echo "  Rappi +200000 (compartida con Tía)"

# ----------------------------------------------------------------------
# 3. EFECTIVO
# ----------------------------------------------------------------------
echo "== Efectivo =="
patch /api/v1/cash '{"amount":110000,"operation":"ADD"}'
echo "  +110000"

# ----------------------------------------------------------------------
# 4. CUENTAS DE AHORRO + BOLSILLOS
# ----------------------------------------------------------------------
echo "== Cuentas de ahorro =="
NUCUENTA=$(post /api/v1/savings '{"institution":"Nu","nickname":"Nu Cuenta","balance":300000,"shared":false}')
NUCUENTA_ID=$(echo "$NUCUENTA" | jq -r '.id')
echo "  Nu Cuenta: $NUCUENTA_ID"

# Rapicuenta marcada como compartida (ahorro de la tía)
RAPICUENTA=$(post /api/v1/savings '{"institution":"Rappi","nickname":"Rapicuenta","balance":500000,"shared":true,"sharedOwnerName":"Tía"}')
RAPICUENTA_ID=$(echo "$RAPICUENTA" | jq -r '.id')
echo "  Rapicuenta (compartida): $RAPICUENTA_ID"

echo "== Bolsillos =="
post "/api/v1/savings/$NUCUENTA_ID/pockets" '{"name":"Matrícula U","balance":150000,"goalAmount":1900000}' >/dev/null
echo "  Matrícula U (meta 1900000)"

# ----------------------------------------------------------------------
# 5. FUENTES DE INGRESO + GASTOS OPERATIVOS
# ----------------------------------------------------------------------
echo "== Fuentes de ingreso =="
TURNO=$(post /api/v1/income/sources '{"description":"Turno hotel","type":"VARIABLE","estimatedAmount":60000,"occurrencesPerMonth":14,"frequency":"PER_SHIFT"}')
TURNO_ID=$(echo "$TURNO" | jq -r '.id')
echo "  Turno hotel: $TURNO_ID"

echo "== Gastos operativos (asociados al turno) =="
post /api/v1/income/expenses "{\"description\":\"Gasolina NKD\",\"frequency\":\"PER_SHIFT\",\"occurrencesPerMonth\":14,\"estimatedAmount\":20000,\"incomeSourceId\":\"$TURNO_ID\"}" >/dev/null
echo "  Gasolina NKD"
post /api/v1/income/expenses "{\"description\":\"Merienda\",\"frequency\":\"PER_SHIFT\",\"occurrencesPerMonth\":14,\"estimatedAmount\":3000,\"incomeSourceId\":\"$TURNO_ID\"}" >/dev/null
echo "  Merienda"

echo "== Registro de ingreso real (un turno cobrado a efectivo) =="
post /api/v1/income/entries "{\"amount\":60000,\"description\":\"Turno noche\",\"incomeSourceId\":\"$TURNO_ID\",\"destinationType\":\"CASH\"}" >/dev/null
echo "  +60000 a efectivo"

# ----------------------------------------------------------------------
# 6. DEUDA FIJA (moto)
# ----------------------------------------------------------------------
echo "== Deuda fija =="
post /api/v1/debts '{"institution":"Banco de Bogotá","description":"Moto NKD","totalAmount":3000000,"monthlyPayment":250000,"paymentDueDay":15,"remainingBalance":1200000}' >/dev/null
echo "  Moto NKD (restante 1200000)"

# ----------------------------------------------------------------------
# 7. FACTURA RECURRENTE (próxima a vencer)
#    OJO: dueDay debe caer en los próximos 7 días para verla como "próxima".
#    Ajustá DUE_DAY al día que corresponda cuando corras esto.
# ----------------------------------------------------------------------
echo "== Factura recurrente =="
DUE_DAY="${DUE_DAY:-$(date -d '+3 days' +%-d)}"  # 3 días desde hoy por defecto
post /api/v1/invoices "{\"description\":\"Plan telefonía\",\"amount\":45000,\"frequency\":\"MONTHLY\",\"dueDay\":$DUE_DAY,\"paymentMethod\":\"CASH\",\"own\":true}" >/dev/null
echo "  Plan telefonía (vence día $DUE_DAY)"

echo ""
echo "== SIEMBRA COMPLETA =="
echo "Verificá con: GET $BASE/api/v1/financial/summary"
