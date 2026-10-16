#!/usr/bin/env bash
set -Eeuo pipefail

API_URL="${API_URL:-http://localhost:8080}"
API_URL="${API_URL%/}"

fail() {
  printf 'ERRO: %s\n' "$1" >&2
  exit 1
}

command -v curl >/dev/null 2>&1 || fail "curl é necessário."
command -v jq >/dev/null 2>&1 || fail "jq é necessário. Instale-o e tente novamente."

if [[ -z "${ADMIN_EMAIL:-}" ]]; then
  read -r -p "E-mail da conta ADMIN: " ADMIN_EMAIL
fi
if [[ -z "${ADMIN_PASSWORD:-}" ]]; then
  read -r -s -p "Senha da conta ADMIN: " ADMIN_PASSWORD
  printf '\n'
fi
[[ -n "$ADMIN_EMAIL" && -n "$ADMIN_PASSWORD" ]] || fail "Informe as credenciais da conta ADMIN."

HTTP_CODE=""
HTTP_BODY=""

request() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"
  local response
  local -a args=(--silent --show-error --request "$method" "$API_URL$path"
    --header "Accept: application/json")

  if [[ -n "$token" ]]; then
    args+=(--header "Authorization: Bearer $token")
  fi
  if [[ -n "$body" ]]; then
    args+=(--header "Content-Type: application/json" --data "$body")
  fi

  response="$(curl "${args[@]}" --write-out $'\n%{http_code}')" \
    || fail "Não consegui acessar $API_URL. Confira se a API está rodando."
  HTTP_CODE="${response##*$'\n'}"
  HTTP_BODY="${response%$'\n'*}"
}

expect_status() {
  local expected="$1"
  local action="$2"
  if [[ "$HTTP_CODE" != "$expected" ]]; then
    printf 'ERRO: %s; esperado HTTP %s, recebido HTTP %s.\n' \
      "$action" "$expected" "$HTTP_CODE" >&2
    if jq -e . >/dev/null 2>&1 <<<"$HTTP_BODY"; then
      jq 'del(.token, .accessToken)' <<<"$HTTP_BODY" >&2
    else
      printf '%s\n' "$HTTP_BODY" >&2
    fi
    exit 1
  fi
}

json_value() {
  local filter="$1"
  local label="$2"
  jq -er "$filter" <<<"$HTTP_BODY" \
    || fail "A resposta da API não trouxe $label."
}

printf '1/6 Entrando como ADMIN...\n'
login_body="$(jq -n --arg email "$ADMIN_EMAIL" --arg senha "$ADMIN_PASSWORD" \
  '{email: $email, senha: $senha}')"
request POST /auth/login "" "$login_body"
expect_status 200 "login ADMIN"
admin_token="$(json_value '.token' 'o token ADMIN')"
admin_role="$(json_value '.role' 'a função da conta')"
[[ "$admin_role" == "ADMIN" ]] || fail "A conta autenticada tem função '$admin_role', não ADMIN."

stamp="$(date +%s)"
suffix="${stamp}-${BASHPID}"
client_email="smoke-${suffix}@example.com"
client_name="Cliente Smoke ${BASHPID}"
barber_phone="1${stamp: -6}$(printf '%03d' "$((BASHPID % 1000))")"
appointment_date="$(date -d '+1 day 10:00' +%Y-%m-%dT%H:%M:%S)"
case "$(date -d "$appointment_date" +%u)" in
  6) appointment_date="$(date -d "$appointment_date + 2 days" +%Y-%m-%dT%H:%M:%S)" ;;
  7) appointment_date="$(date -d "$appointment_date + 1 day" +%Y-%m-%dT%H:%M:%S)" ;;
esac

printf '2/6 Cadastrando cliente de teste...\n'
client_body="$(jq -n --arg email "$client_email" --arg senha "Smoke-${stamp}-Senha" \
  --arg nome "$client_name" '{email: $email, senha: $senha, nome: $nome}')"
request POST /auth/cadastro "" "$client_body"
expect_status 201 "cadastro do cliente"
client_token="$(json_value '.token' 'o token do cliente')"

printf '3/6 Consultando o perfil do cliente...\n'
request GET /clientes/me "$client_token"
expect_status 200 "consulta do perfil do cliente"
client_id="$(json_value '.id' 'o ID do cliente')"

printf '4/6 Cadastrando barbeiro de teste...\n'
barber_body="$(jq -n --arg telefone "$barber_phone" \
  '{nome: "Barbeiro Smoke", telefone: $telefone, especialidade: "Corte masculino"}')"
request POST /barbeiros "$admin_token" "$barber_body"
expect_status 200 "cadastro do barbeiro"
barber_id="$(json_value '.id' 'o ID do barbeiro')"

printf '5/6 Criando agendamento...\n'
appointment_body="$(jq -n --argjson barbeiroId "$barber_id" \
  --argjson clienteId "$client_id" --arg dataHora "$appointment_date" \
  '{barbeiroId: $barbeiroId, clienteId: $clienteId, dataHora: $dataHora}')"
request POST /agendamentos "$admin_token" "$appointment_body"
expect_status 201 "criação do agendamento"
appointment_id="$(json_value '.id' 'o ID do agendamento')"

printf '6/6 Confirmando o agendamento pela conta do cliente...\n'
request GET /agendamentos/me "$client_token"
expect_status 200 "consulta dos agendamentos do cliente"
jq -e --argjson id "$appointment_id" 'any(.[]; .id == $id)' \
  >/dev/null <<<"$HTTP_BODY" \
  || fail "O agendamento criado não apareceu na agenda do cliente."

printf '\nSUCESSO: fluxo completo passou.\n'
printf 'Cliente: %s (ID %s)\n' "$client_email" "$client_id"
printf 'Barbeiro: ID %s\n' "$barber_id"
printf 'Agendamento: ID %s em %s\n' "$appointment_id" "$appointment_date"
printf 'Os registros de teste foram mantidos no banco.\n'
