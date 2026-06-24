#!/usr/bin/env bash
#
# setup-fase3.sh — Fusiona Bot + IA en Fase 3, renumera todos los milestones
# y crea los 11 issues de Fase 3.
#
# REQUISITOS:
#   - gh CLI autenticado con permiso de ESCRITURA sobre el repo.
#       Verificá:  gh api repos/Jbyanx/finsight --jq .permissions
#       Si push != true:  gh auth switch   (a la cuenta Jbyanx)
#   - Correr desde cualquier lado (usa --repo).
#
# RENUMERACIÓN DE MILESTONES (renombra por título; los números internos no cambian):
#   "Fase 3 - Bot de Telegram"  -> "Fase 3 - Bot de Telegram + IA"
#   "Fase 4 - Spring AI"        -> "Fase 4 - React PWA"            (contenido de la vieja 5)
#   "Fase 5 - React PWA"        -> "Fase 5 - Polish y producción"  (contenido de la vieja 6)
#   "Fase 6 - Polish y producción" -> se CIERRA (subió a la 5)
#
# NO toca el issue cerrado de TelegramLink. NO crea issue de jacoco (ya hecho).

set -euo pipefail

# ----------------------------------------------------------------------
# CONFIG
# ----------------------------------------------------------------------
REPO="Jbyanx/finsight"

# Títulos ACTUALES en GitHub (revisá que coincidan EXACTO, incluido el guion)
CUR_F3="Fase 3 - Bot de Telegram"
CUR_F4="Fase 4 - Spring AI"
CUR_F5="Fase 5 - React PWA"
CUR_F6="Fase 6 - Polish y producción"

# Títulos NUEVOS
NEW_F3="Fase 3 - Bot de Telegram + IA"
NEW_F4="Fase 4 - React PWA"
NEW_F5="Fase 5 - Polish y producción"

MS_TARGET="$NEW_F3"   # milestone donde caen los issues nuevos

# ----------------------------------------------------------------------
# Helper: renombrar / actualizar un milestone por título
#   $1 = título actual, $2 = título nuevo, $3 = descripción, $4 = state (open|closed)
# ----------------------------------------------------------------------
patch_milestone() {
  local cur="$1"; local new="$2"; local desc="$3"; local state="$4"
  local num
  num=$(gh api "repos/$REPO/milestones?state=all" \
        --jq ".[] | select(.title==\"$cur\") | .number" || true)
  if [ -z "${num:-}" ]; then
    echo "!! No encontré milestone '$cur' — revisá el título exacto."
    return 1
  fi
  gh api -X PATCH "repos/$REPO/milestones/$num" \
    -f title="$new" -f description="$desc" -f state="$state" >/dev/null
  echo ">> Milestone #$num: '$cur' -> '$new' ($state)"
}

echo "== Renumerando milestones =="
# IMPORTANTE: el orden importa para no chocar títulos duplicados temporalmente.
# La vieja F6 se cierra primero (libera el nombre 'Polish'), luego F5->Polish, F4->PWA, F3->Bot+IA.
patch_milestone "$CUR_F6" "$CUR_F6" "Absorbido: su contenido sube a Fase 5." "closed"
patch_milestone "$CUR_F5" "$NEW_F5" "Liquibase, CD, deploy Railway, repo público, monitoreo." "open"
patch_milestone "$CUR_F4" "$NEW_F4" "Dashboard, vistas, notificaciones push, mobile first, PWA." "open"
patch_milestone "$CUR_F3" "$NEW_F3" "Bot de Telegram (long-polling) + Spring AI/Gemini. Registro y consultas por mensaje natural." "open"

# ----------------------------------------------------------------------
# Labels (idempotente)
# ----------------------------------------------------------------------
echo "== Labels =="
gh label create "telegram" --color "229ED9" --description "Bot de Telegram"   --force --repo "$REPO" || true
gh label create "ai"       --color "8E44AD" --description "Spring AI / Gemini" --force --repo "$REPO" || true
gh label create "test"     --color "2ECC71" --description "Tests"              --force --repo "$REPO" || true

# ----------------------------------------------------------------------
# Crear issues
# ----------------------------------------------------------------------
create() {
  local title="$1"; local body="$2"; local labels="$3"
  echo ">> $title"
  gh issue create --repo "$REPO" --title "$title" --body "$body" \
    --milestone "$MS_TARGET" --label "$labels"
}

echo "== Issues de Fase 3 =="

create "feat(telegram): integrar telegrambots y registrar el bot (long-polling)" \
"## Qué
Agregar \`telegrambots-spring-boot-starter\`, registrar el bot en **long-polling** y recibir updates.

## Alcance
- Dependencia en pom + verificar compatibilidad con Spring Boot 3.5.14.
- Bean del bot con token/username desde env (ya en .env.example).
- Handler base con \`/start\` y \`/help\`. Sin IA, sin linking aún.

## Cómo probarlo
Mandar /help y recibir respuesta." \
"telegram"

create "feat(telegram): completar vinculación de cuenta vía /start <code>" \
"## Qué
Cerrar el linking a medias: el handler toma el código de \`/start <code>\`, llama a \`TelegramLinkService.linkAccount(code, chatId)\` y bloquea chats no vinculados.

## Ya existe (no recrear)
TelegramLink, LinkCode, TelegramLinkService(+impl), repos, DTOs, endpoint /link-code.

## Seguridad
userId SIEMPRE desde \`resolveUserId(chatId)\`, nunca del mensaje.

## Depende de
Bot long-polling." \
"telegram"

create "chore(ai): integrar Spring AI + Gemini y configurar ChatClient" \
"## Qué
Spring AI con Gemini + un \`ChatClient\` inyectable.

## Alcance
- Dependencia Spring AI (verificar versión compatible con Boot 3.5.14).
- \`GEMINI_API_KEY\` (ya en .env.example).
- Smoke test: prompt simple devuelve respuesta." \
"ai"

create "feat(ai): definir tools de function calling sobre los services existentes" \
"## Qué
@Tool que envuelven los services de Fase 2 para que el modelo las invoque.

## Alcance
- Tools: registrar transacción, ingreso, factura, pagar deuda, consultar tarjeta, crear cuenta/bolsillo, resumen.
- **Crítico:** userId resuelto desde chatId e inyectado por contexto, NUNCA argumento del LLM.

## Depende de
ChatClient." \
"ai"

create "feat(ai): construir contexto financiero del usuario como system prompt" \
"## Qué
Inyectar el estado financiero real en el prompt de sistema.

## Alcance
- \`FinancialService.getSummary\` (utilización, ingreso neto, deudas).
- Reglas de dominio en el prompt (utilización <30%, no contar ahorro compartido).

## Depende de
Tools." \
"ai"

create "feat(telegram): orquestar mensaje natural -> ChatClient con tools -> respuesta" \
"## Qué
Pegamento: mensaje -> resolver userId -> ChatClient con tools + contexto -> responder.

## Nota
Aquí 'reviven' los comandos del roadmap porque los maneja function calling.

## Depende de
Tools + contexto." \
"telegram,ai"

create "feat(ai): confirmación explícita antes de toda mutación" \
"## Qué
Regla de dominio: nunca modificar datos sin confirmación del usuario.

## Alcance
- Tools de escritura pasan por confirmación; las de lectura no.

## Depende de
Orquestación." \
"ai"

create "feat(telegram): persistir historial de conversación en MongoDB" \
"## Qué
Historial de mensajes + memoria del ChatClient.

## Alcance
- Colección \`conversationsBot\` (Mongo ya en el stack).
- Memoria por chat para contexto multivuelta.

## Depende de
Orquestación." \
"telegram,ai"

create "feat(ai): resumen mensual y recomendaciones pre-corte (scheduled)" \
"## Qué
Resumen mensual proactivo + recomendaciones antes del corte.

## Alcance
- Scheduler (patrón de \`TokenCleanupScheduler\`).
- Push al chatId. Pago al mayor plazo sin intereses, tarjeta de menor utilización.

## Depende de
Orquestación + contexto." \
"ai"

create "feat(ai): alertas inteligentes de facturas próximas (scheduled)" \
"## Qué
Aviso antes del vencimiento de facturas, distinguiendo propias de cobrables a terceros.

## Alcance
- Scheduler de facturas próximas + mensaje proactivo.

## Depende de
Orquestación." \
"ai"

create "test: suite de Fase 3 (bot + IA) y subir umbral de jacoco" \
"## Qué
Cobertura de los nuevos dominios y subir el gate.

## Alcance
- Unit tests de bot handlers y tools; integración donde aplique.
- Subir \`<minimum>\` de jacoco 0.60 -> 0.65 al cerrar el milestone.

## Nota
Config de jacoco (lombok.config + exclusiones) ya hecha antes de este milestone." \
"test,telegram,ai"

echo ""
echo "== LISTO =="
echo "Issues creados en '$MS_TARGET'. Anotá los números que imprimió gh arriba."
echo "Convención: número de rama = número de issue."
