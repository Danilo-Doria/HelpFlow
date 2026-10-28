#!/usr/bin/env bash
# =============================================================================
# smoke-test.sh - Verificación de punta a punta de HelpFlow
#
# Uso:   ./smoke-test.sh | tee resultado.txt
# Requiere: curl, jq, y (para las secciones 10 y 11) docker.
# Recomendado: base de datos limpia ->  docker compose down -v && docker compose up --build -d
#
# Variables opcionales: API, DISPATCH, ADMIN_USER, ADMIN_PASS,
#                       DB_CONTAINER, DISPATCH_CONTAINER, SKIP_DOCKER=1
# =============================================================================

API="${API:-http://localhost:8080}"
DISPATCH="${DISPATCH:-http://localhost:8081}"
ADMIN_USER="${ADMIN_USER:-admin@gmail.com}"
ADMIN_PASS="${ADMIN_PASS:-admin123}"
DB_CONTAINER="${DB_CONTAINER:-helpflow-db}"
DISPATCH_CONTAINER="${DISPATCH_CONTAINER:-helpflow-dispatch}"
SKIP_DOCKER="${SKIP_DOCKER:-0}"

S=$(date +%s)
PASS=0
FAIL=0
TMP=$(mktemp)
trap 'rm -f "$TMP"' EXIT

command -v curl >/dev/null || { echo "Falta curl"; exit 1; }
command -v jq   >/dev/null || { echo "Falta jq (sudo apt install jq)"; exit 1; }

DOCKER_OK=0
if [ "$SKIP_DOCKER" != "1" ] && docker ps >/dev/null 2>&1; then DOCKER_OK=1; fi

# ---------------------------------------------------------------- utilidades --
req() { # METODO URL [TOKEN] [JSON]  -> deja CODE y BODY
  local method="$1" url="$2" token="$3" data="$4"
  local args=(-s -o "$TMP" -w '%{http_code}' -X "$method" "$url")
  [ -n "$token" ] && args+=(-H "Authorization: Bearer $token")
  [ -n "$data" ]  && args+=(-H "Content-Type: application/json" -d "$data")
  CODE=$(curl "${args[@]}")
  BODY=$(cat "$TMP")
}

ok()  { PASS=$((PASS+1)); printf '  [PASS] %s\n' "$1"; }
bad() { FAIL=$((FAIL+1)); printf '  [FAIL] %s\n         %s\n' "$1" "$2"; }

check() { # NOMBRE ESPERADO METODO URL TOKEN JSON
  local name="$1" expected="$2"; shift 2
  req "$@"
  if [ "$CODE" = "$expected" ]; then ok "$name (HTTP $CODE)"
  else bad "$name" "esperado HTTP $expected, obtuvo $CODE :: $(echo "$BODY" | head -c 300)"; fi
}

expect() { # NOMBRE EXPR_JQ ESPERADO   (sobre el último BODY)
  local got
  got=$(echo "$BODY" | jq -r "$2" 2>/dev/null)
  if [ "$got" = "$3" ]; then ok "$1"
  else bad "$1" "esperado '$3', obtuvo '$got'"; fi
}

assert_eq() { # NOMBRE ESPERADO OBTENIDO
  if [ "$2" = "$3" ]; then ok "$1"; else bad "$1" "esperado '$2', obtuvo '$3'"; fi
}

section() { printf '\n== %s ==\n' "$1"; }

login() { # USER PASS -> token
  curl -s -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | jq -r '.token'
}

ticket() { # CATEGORIA PRIORIDAD SEDE -> JSON
  printf '{"titulo":"Prueba %s","descripcion":"Ticket de prueba automatizada","categoria":"%s","prioridad":"%s","sede":"%s","activoAfectado":"EQ-%s"}' \
    "$1" "$1" "$2" "$3" "$S"
}

sql() { docker exec -i "$DB_CONTAINER" psql -U helpflow -d helpflow -qAt -c "$1"; }

post_dispatch() { # JSON
  curl -s -X POST "$DISPATCH/technician-dispatch" -H 'Content-Type: application/json' -d "$1"
}

# =============================================================================
section "1. SALUD Y TECHNICIAN-DISPATCH-SERVICE (puerto 8081)"
# =============================================================================
check "Dispatch /actuator/health" 200 GET "$DISPATCH/actuator/health" ""
check "Soporte  /actuator/health (público)" 200 GET "$API/actuator/health" ""
expect "Soporte health = UP" .status UP

D1=$(post_dispatch '{"ticketId":125,"categoria":"RED","prioridad":"ALTA"}')
D2=$(post_dispatch '{"ticketId":125,"categoria":"RED","prioridad":"ALTA"}')
echo "      respuesta: $D1"
assert_eq "Despacho determinista: misma combinación, misma respuesta" "$D1" "$D2"

CATS=(HARDWARE SOFTWARE RED ACCESOS OTRO)
PRIOS=(BAJA MEDIA ALTA CRITICA)
TECS=()
CLASIF_MAL=0
i=0
for c in "${CATS[@]}"; do
  for p in "${PRIOS[@]}"; do
    i=$((i+1))
    r=$(post_dispatch "{\"ticketId\":$i,\"categoria\":\"$c\",\"prioridad\":\"$p\"}")
    h=$(echo "$r" | jq -r '.tiempoEstimadoHoras')
    d=$(echo "$r" | jq -r '.disponibilidad')
    esperado=$(jq -rn --argjson h "$h" 'if $h<=2 then "ALTA" elif $h<=8 then "MEDIA" else "BAJA" end')
    [ "$d" = "$esperado" ] || CLASIF_MAL=$((CLASIF_MAL+1))
    TECS+=("$(echo "$r" | jq -r '.tecnicoId')")
  done
done
assert_eq "Clasificación 0-2h ALTA / 3-8h MEDIA / >8h BAJA (20 combinaciones)" 0 "$CLASIF_MAL"
DISTINTOS=$(printf '%s\n' "${TECS[@]}" | sort -u | wc -l)
if [ "$DISTINTOS" -ge 2 ]; then ok "Combinaciones distintas producen técnicos distintos ($DISTINTOS técnicos)"
else bad "Combinaciones distintas producen técnicos distintos" "solo $DISTINTOS técnico(s)"; fi

req POST "$DISPATCH/technician-dispatch" "" '{}'
assert_eq "Dispatch rechaza cuerpo vacío (400)" 400 "$CODE"
req POST "$DISPATCH/technician-dispatch" "" '{"ticketId":1,"categoria":"XYZ","prioridad":"ALTA"}'
assert_eq "Dispatch rechaza categoría inválida (400)" 400 "$CODE"

# =============================================================================
section "2. AUTENTICACION Y VALIDACIONES"
# =============================================================================
ANA_MAIL="ana$S@helpflow.com"
REG_ANA='{"documento":"D'$S'a","nombre":"Ana Díaz","correo":"'$ANA_MAIL'","sede":"BARRANQUILLA","area":"Finanzas","password":"Clave12345"}'
check "Registro de usuario" 201 POST "$API/auth/register" "" "$REG_ANA"
ANA_ID=$(echo "$BODY" | jq -r .id)
check "Registro con documento duplicado" 409 POST "$API/auth/register" "" \
  '{"documento":"D'$S'a","nombre":"Otro","correo":"otro'$S'@helpflow.com","sede":"BOGOTA","area":"TI","password":"Clave12345"}'
check "Registro con correo duplicado" 409 POST "$API/auth/register" "" \
  '{"documento":"D'$S'z","nombre":"Otro","correo":"'$ANA_MAIL'","sede":"BOGOTA","area":"TI","password":"Clave12345"}'
check "Registro con cuerpo vacío (validación)" 400 POST "$API/auth/register" "" '{}'
expect "  -> ProblemDetail incluye lista 'errors'" '.errors|length>0' true
check "Registro con correo inválido" 400 POST "$API/auth/register" "" \
  '{"documento":"D'$S'm","nombre":"X","correo":"no-es-correo","sede":"A","area":"B","password":"Clave12345"}'
check "Registro con contraseña corta" 400 POST "$API/auth/register" "" \
  '{"documento":"D'$S'p","nombre":"X","correo":"p'$S'@helpflow.com","sede":"A","area":"B","password":"123"}'

check "Login correcto" 200 POST "$API/auth/login" "" '{"username":"'$ANA_MAIL'","password":"Clave12345"}'
expect "  -> tipo de token Bearer" .tipo Bearer
ANA=$(echo "$BODY" | jq -r .token)
check "Login con contraseña incorrecta" 401 POST "$API/auth/login" "" '{"username":"'$ANA_MAIL'","password":"mala"}'
expect "  -> título Credenciales inválidas" .title "Credenciales inválidas"
check "Login con usuario inexistente" 401 POST "$API/auth/login" "" '{"username":"nadie@helpflow.com","password":"Clave12345"}'
check "Ruta protegida sin token" 401 GET "$API/tickets" ""
check "Ruta protegida con token basura" 401 GET "$API/tickets" "esto.no.es.un.jwt"

ADMIN=$(login "$ADMIN_USER" "$ADMIN_PASS")
if [ -n "$ADMIN" ] && [ "$ADMIN" != "null" ]; then ok "Login del Admin inicial"
else bad "Login del Admin inicial" "no se obtuvo token (revisa ADMIN_USER/ADMIN_PASS)"; fi

LUIS_MAIL="luis$S@helpflow.com"
check "Registro de segundo usuario (Luis)" 201 POST "$API/auth/register" "" \
  '{"documento":"D'$S'b","nombre":"Luis Mora","correo":"'$LUIS_MAIL'","sede":"BOGOTA","area":"Ventas","password":"Clave12345"}'
LUIS_ID=$(echo "$BODY" | jq -r .id)
LUIS=$(login "$LUIS_MAIL" "Clave12345")

# =============================================================================
section "3. ADMIN: TECNICOS Y USUARIOS"
# =============================================================================
PEDRO_MAIL="pedro$S@helpflow.com"
REG_PEDRO='{"documento":"T'$S'","nombre":"Pedro Salas","salario":3400000,"fechaAfiliacion":"2024-03-01","especialidad":"RED","sede":"BARRANQUILLA","cargaMaxima":5,"username":"'$PEDRO_MAIL'","password":"Clave12345"}'
check "Admin crea técnico con credencial" 201 POST "$API/tecnicos" "$ADMIN" "$REG_PEDRO"
TEC_ID=$(echo "$BODY" | jq -r .id)
check "Técnico con documento duplicado" 409 POST "$API/tecnicos" "$ADMIN" \
  '{"documento":"T'$S'","nombre":"Dup","salario":1000000,"fechaAfiliacion":"2024-03-01","especialidad":"RED","sede":"X","cargaMaxima":3,"username":"dup'$S'@helpflow.com","password":"Clave12345"}'
check "Técnico con cuerpo vacío (validación)" 400 POST "$API/tecnicos" "$ADMIN" '{}'
check "Admin lista técnicos" 200 GET "$API/tecnicos" "$ADMIN"
expect "  -> incluye los 5 del seed + el nuevo" 'length>=6' true
check "Admin consulta técnico por id" 200 GET "$API/tecnicos/$TEC_ID" "$ADMIN"
check "Técnico inexistente" 404 GET "$API/tecnicos/999999" "$ADMIN"
check "Admin edita perfil de soporte del técnico" 200 PUT "$API/tecnicos/$TEC_ID" "$ADMIN" \
  '{"nombre":"Pedro Salas M","salario":3500000,"especialidad":"RED","sede":"BARRANQUILLA","cargaMaxima":6}'
expect "  -> carga máxima actualizada" .cargaMaxima 6
check "Admin lista usuarios" 200 GET "$API/usuarios" "$ADMIN"
check "Admin consulta usuario por id" 200 GET "$API/usuarios/$ANA_ID" "$ADMIN"
check "Admin edita usuario" 200 PUT "$API/usuarios/$ANA_ID" "$ADMIN" \
  '{"nombre":"Ana Díaz","sede":"BARRANQUILLA","area":"Contabilidad"}'
expect "  -> área actualizada" .area Contabilidad
check "Usuario normal NO puede listar técnicos" 403 GET "$API/tecnicos" "$ANA"
expect "  -> título Acceso denegado" .title "Acceso denegado"
check "Usuario normal NO puede listar usuarios" 403 GET "$API/usuarios" "$ANA"

TEC=$(login "$PEDRO_MAIL" "Clave12345")
if [ -n "$TEC" ] && [ "$TEC" != "null" ]; then ok "Login del técnico recién creado"
else bad "Login del técnico recién creado" "sin token"; fi

# =============================================================================
section "4. TICKETS: CREACION, ASIGNACION AUTOMATICA Y PROPIEDAD"
# =============================================================================
check "Usuario crea ticket RED / ALTA" 201 POST "$API/tickets" "$ANA" "$(ticket RED ALTA BARRANQUILLA)"
T1=$(echo "$BODY" | jq -r .id)
echo "      ticket $T1 -> estado=$(echo "$BODY" | jq -r .estado) técnico=$(echo "$BODY" | jq -r .tecnicoNombre) prioridad=$(echo "$BODY" | jq -r .prioridad) escalado=$(echo "$BODY" | jq -r .escalado)"
expect "Ticket quedó ASIGNADO (integración con dispatch)" .estado ASIGNADO
expect "Ticket tiene técnico asignado" '.tecnicoId != null' true
expect "Fecha límite de SLA calculada" '.fechaLimiteSla != null' true
check "Historial: creación + asignación" 200 GET "$API/tickets/$T1/historial" "$ANA"
expect "  -> al menos 2 registros" 'length>=2' true

check "Admin NO puede crear tickets" 403 POST "$API/tickets" "$ADMIN" "$(ticket RED ALTA BARRANQUILLA)"
check "Técnico NO puede crear tickets" 403 POST "$API/tickets" "$TEC" "$(ticket RED ALTA BARRANQUILLA)"
check "Ticket con cuerpo vacío (validación)" 400 POST "$API/tickets" "$ANA" '{}'
check "Ticket con categoría inexistente" 400 POST "$API/tickets" "$ANA" \
  '{"titulo":"x","descripcion":"x","categoria":"XYZ","prioridad":"ALTA","sede":"A"}'

check "Dueño consulta su ticket" 200 GET "$API/tickets/$T1" "$ANA"
check "Otro usuario NO ve ticket ajeno" 403 GET "$API/tickets/$T1" "$LUIS"
expect "  -> título Operación no permitida" .title "Operación no permitida"
check "Otro usuario NO ve historial ajeno" 403 GET "$API/tickets/$T1/historial" "$LUIS"
check "Luis lista sus tickets (ninguno)" 200 GET "$API/tickets" "$LUIS"
expect "  -> lista vacía" 'length' 0
check "Ana lista sus tickets" 200 GET "$API/tickets" "$ANA"
expect "  -> contiene el suyo" 'length>=1' true
check "Admin lista todos" 200 GET "$API/tickets" "$ADMIN"
check "Ticket inexistente" 404 GET "$API/tickets/999999" "$ADMIN"

# =============================================================================
section "5. REASIGNACION Y REGLAS DE ASIGNACION (ADMIN)"
# =============================================================================
req GET "$API/tickets/$T1" "$ADMIN"
ACTUAL=$(echo "$BODY" | jq -r .tecnicoId)
if [ "$ACTUAL" = "$TEC_ID" ]; then
  req PUT "$API/tickets/$T1/asignacion" "$ADMIN" '{"tecnicoId":1,"observacion":"preparacion"}'
fi

check "Técnico NO ve ticket que no es suyo" 403 GET "$API/tickets/$T1" "$TEC"
check "Técnico NO puede cambiar estado de ticket ajeno" 403 PATCH "$API/tickets/$T1/estado" "$TEC" \
  '{"estado":"EN_PROGRESO","observacion":"intento"}'
check "Usuario NO puede reasignar" 403 PUT "$API/tickets/$T1/asignacion" "$ANA" '{"tecnicoId":'$TEC_ID'}'
check "Admin reasigna a Pedro (RED, activo)" 200 PUT "$API/tickets/$T1/asignacion" "$ADMIN" \
  '{"tecnicoId":'$TEC_ID',"observacion":"Reasignación de prueba"}'
expect "  -> técnico asignado es Pedro" .tecnicoId "$TEC_ID"
check "Reasignar al mismo técnico" 422 PUT "$API/tickets/$T1/asignacion" "$ADMIN" '{"tecnicoId":'$TEC_ID'}'
check "Reasignar a técnico de otra especialidad (SOFTWARE)" 422 PUT "$API/tickets/$T1/asignacion" "$ADMIN" '{"tecnicoId":2}'
check "Reasignar a técnico inexistente" 404 PUT "$API/tickets/$T1/asignacion" "$ADMIN" '{"tecnicoId":999999}'
check "Reasignación sin tecnicoId (validación)" 400 PUT "$API/tickets/$T1/asignacion" "$ADMIN" '{}'

# =============================================================================
section "6. CICLO DE VIDA Y TRANSICIONES DE ESTADO"
# =============================================================================
check "Técnico ve su ticket asignado" 200 GET "$API/tickets/$T1" "$TEC"
check "Técnico lista solo sus tickets" 200 GET "$API/tickets" "$TEC"
expect "  -> todos son de Pedro" "[.[].tecnicoId] | unique == [$TEC_ID]" true
check "Usuario NO puede pasar a EN_PROGRESO (rol)" 403 PATCH "$API/tickets/$T1/estado" "$ANA" \
  '{"estado":"EN_PROGRESO","observacion":"no me toca"}'
check "Técnico NO puede cerrar (rol)" 403 PATCH "$API/tickets/$T1/estado" "$TEC" \
  '{"estado":"CERRADO","observacion":"no me toca"}'
check "Cambio de estado sin observación (validación)" 400 PATCH "$API/tickets/$T1/estado" "$TEC" '{"estado":"EN_PROGRESO"}'
check "Técnico: ASIGNADO -> EN_PROGRESO" 200 PATCH "$API/tickets/$T1/estado" "$TEC" \
  '{"estado":"EN_PROGRESO","observacion":"Revisando el switch"}'
expect "  -> estado EN_PROGRESO" .estado EN_PROGRESO
expect "  -> registra primera respuesta" '.fechaPrimeraRespuesta != null' true
check "Técnico: EN_PROGRESO -> RESUELTO" 200 PATCH "$API/tickets/$T1/estado" "$TEC" \
  '{"estado":"RESUELTO","observacion":"Switch reemplazado"}'
expect "  -> registra fecha de resolución" '.fechaResolucion != null' true
check "Usuario: RESUELTO -> CERRADO" 200 PATCH "$API/tickets/$T1/estado" "$ANA" \
  '{"estado":"CERRADO","observacion":"Confirmado, funciona"}'
expect "  -> estado CERRADO" .estado CERRADO
check "Admin: CERRADO -> EN_PROGRESO (transición inválida)" 409 PATCH "$API/tickets/$T1/estado" "$ADMIN" \
  '{"estado":"EN_PROGRESO","observacion":"reabrir"}'
expect "  -> título Transición de estado inválida" .title "Transición de estado inválida"
check "No se puede reasignar un ticket CERRADO" 409 PUT "$API/tickets/$T1/asignacion" "$ADMIN" '{"tecnicoId":1}'
check "Historial completo del ciclo de vida" 200 GET "$API/tickets/$T1/historial" "$ADMIN"
expect "  -> al menos 5 registros" 'length>=5' true

# =============================================================================
section "7. CANCELACION Y USUARIO INACTIVO"
# =============================================================================
check "Usuario crea otro ticket" 201 POST "$API/tickets" "$ANA" "$(ticket SOFTWARE BAJA BARRANQUILLA)"
T2=$(echo "$BODY" | jq -r .id)
check "Usuario cancela su ticket" 200 PATCH "$API/tickets/$T2/estado" "$ANA" '{"estado":"CANCELADO","observacion":"Ya no lo necesito"}'
expect "  -> estado CANCELADO" .estado CANCELADO
check "Cancelar un ticket ya CANCELADO" 409 PATCH "$API/tickets/$T2/estado" "$ANA" '{"estado":"CANCELADO","observacion":"otra vez"}'

check "Admin desactiva a Luis" 200 PATCH "$API/usuarios/$LUIS_ID/estado" "$ADMIN" '{"estado":"INACTIVO"}'
check "Usuario INACTIVO no puede crear tickets" 422 POST "$API/tickets" "$LUIS" "$(ticket RED BAJA BOGOTA)"
expect "  -> título Regla de negocio incumplida" .title "Regla de negocio incumplida"
check "Usuario INACTIVO no puede iniciar sesión" 401 POST "$API/auth/login" "" '{"username":"'$LUIS_MAIL'","password":"Clave12345"}'
check "Admin reactiva a Luis" 200 PATCH "$API/usuarios/$LUIS_ID/estado" "$ADMIN" '{"estado":"ACTIVO"}'
check "Luis vuelve a iniciar sesión" 200 POST "$API/auth/login" "" '{"username":"'$LUIS_MAIL'","password":"Clave12345"}'

# =============================================================================
section "8. CARGA MAXIMA Y TECNICO INACTIVO"
# =============================================================================
check "Admin crea técnico ACCESOS con carga máxima 1" 201 POST "$API/tecnicos" "$ADMIN" \
  '{"documento":"U'$S'","nombre":"Tecnico Uno","salario":3000000,"fechaAfiliacion":"2024-05-01","especialidad":"ACCESOS","sede":"MEDELLIN","cargaMaxima":1,"username":"uno'$S'@helpflow.com","password":"Clave12345"}'
UNO_ID=$(echo "$BODY" | jq -r .id)
check "Ticket ACCESOS A" 201 POST "$API/tickets" "$ANA" "$(ticket ACCESOS BAJA MEDELLIN)"
TA=$(echo "$BODY" | jq -r .id)
check "Ticket ACCESOS B" 201 POST "$API/tickets" "$ANA" "$(ticket ACCESOS BAJA MEDELLIN)"
TB=$(echo "$BODY" | jq -r .id)
check "Admin asigna A al técnico (queda en su carga máxima)" 200 PUT "$API/tickets/$TA/asignacion" "$ADMIN" '{"tecnicoId":'$UNO_ID'}'
check "Asignar B supera la carga máxima" 422 PUT "$API/tickets/$TB/asignacion" "$ADMIN" '{"tecnicoId":'$UNO_ID'}'
expect "  -> el detalle menciona la carga máxima" '.detail|contains("carga máxima")' true
check "Admin desactiva al técnico" 200 PATCH "$API/tecnicos/$UNO_ID/estado" "$ADMIN" '{"estado":"INACTIVO"}'
check "No se asigna a un técnico INACTIVO" 422 PUT "$API/tickets/$TB/asignacion" "$ADMIN" '{"tecnicoId":'$UNO_ID'}'
check "Técnico INACTIVO no puede iniciar sesión" 401 POST "$API/auth/login" "" '{"username":"uno'$S'@helpflow.com","password":"Clave12345"}'

# =============================================================================
section "9. CONFIGURACION DE SLA (ADMIN)"
# =============================================================================
check "Admin consulta SLA" 200 GET "$API/sla" "$ADMIN"
expect "  -> 4 prioridades" length 4
check "Usuario NO puede consultar SLA" 403 GET "$API/sla" "$ANA"
check "SLA incoherente (ALTA 100h > MEDIA)" 422 PUT "$API/sla/ALTA" "$ADMIN" '{"horas":100}'
check "SLA con horas inválidas (0)" 400 PUT "$API/sla/ALTA" "$ADMIN" '{"horas":0}'
check "Admin cambia SLA de ALTA a 6h" 200 PUT "$API/sla/ALTA" "$ADMIN" '{"horas":6}'
check "Ticket ALTA usa el SLA nuevo" 201 POST "$API/tickets" "$ANA" "$(ticket HARDWARE ALTA BOGOTA)"
HORAS=$(echo "$BODY" | jq -r '((.fechaLimiteSla|sub("\\.[0-9]+";"")|fromdateiso8601) - (.fechaCreacion|sub("\\.[0-9]+";"")|fromdateiso8601))/3600')
assert_eq "  -> fecha límite = creación + 6h" 6 "$HORAS"
check "Admin restaura SLA de ALTA a 8h" 200 PUT "$API/sla/ALTA" "$ADMIN" '{"horas":8}'

# =============================================================================
section "10. ESCALAMIENTO AUTOMATICO Y TICKETS VENCIDOS"
# =============================================================================
check "Usuario NO puede disparar el escalamiento" 403 POST "$API/tickets/escalamiento" "$ANA"
if [ "$DOCKER_OK" = "1" ]; then
  check "Ticket para escalar (BAJA, OTRO)" 201 POST "$API/tickets" "$ANA" "$(ticket OTRO BAJA BOGOTA)"
  TE=$(echo "$BODY" | jq -r .id)
  check "Ticket para vencer (BAJA, OTRO)" 201 POST "$API/tickets" "$ANA" "$(ticket OTRO BAJA BOGOTA)"
  TV=$(echo "$BODY" | jq -r .id)
  # TE vence en 30 min (dentro del umbral de 2h); TV ya venció hace 1 hora
  sql "update tickets set fecha_creacion = now() - interval '1 hour', fecha_limite_sla = now() + interval '30 minutes' where id = $TE" >/dev/null
  sql "update tickets set fecha_creacion = now() - interval '10 hours', fecha_limite_sla = now() - interval '1 hour' where id = $TV" >/dev/null

  check "Admin dispara el escalamiento" 200 POST "$API/tickets/escalamiento" "$ADMIN"
  echo "      respuesta: $BODY (puede ser 0 si el job automático ya lo hizo)"
  check "Consulta ticket próximo a vencer" 200 GET "$API/tickets/$TE" "$ADMIN"
  expect "  -> quedó escalado" .escalado true
  expect "  -> prioridad BAJA subió a MEDIA" .prioridad MEDIA
  check "Historial del ticket escalado" 200 GET "$API/tickets/$TE/historial" "$ADMIN"
  expect "  -> último registro lo hizo 'sistema'" '.[-1].realizadoPor' sistema
  check "Consulta ticket ya vencido" 200 GET "$API/tickets/$TV" "$ADMIN"
  expect "  -> también quedó escalado" .escalado true
  check "Historial del ticket vencido" 200 GET "$API/tickets/$TV/historial" "$ADMIN"
  expect "  -> el motivo indica SLA vencido" '.[-1].observacion|contains("SLA vencido")' true
  check "Segundo escalamiento no repite" 200 POST "$API/tickets/escalamiento" "$ADMIN"
  expect "  -> 0 escalados" .escalados 0
else
  echo "  (omitida: docker no disponible o SKIP_DOCKER=1; necesita acceso a la BD para envejecer tickets)"
fi

# =============================================================================
section "11. RESILIENCIA: DESPACHO CAIDO Y ASIGNACION MANUAL"
# =============================================================================
if [ "$DOCKER_OK" = "1" ]; then
  docker stop "$DISPATCH_CONTAINER" >/dev/null
  check "Con el despacho caído el ticket igual se crea" 201 POST "$API/tickets" "$ANA" "$(ticket RED MEDIA BARRANQUILLA)"
  expect "  -> queda ABIERTO sin asignar" .estado ABIERTO
  TD=$(echo "$BODY" | jq -r .id)
  check "Transición inválida ABIERTO -> RESUELTO" 409 PATCH "$API/tickets/$TD/estado" "$ADMIN" \
    '{"estado":"RESUELTO","observacion":"salto invalido"}'
  check "Admin asigna manualmente el ticket ABIERTO" 200 PUT "$API/tickets/$TD/asignacion" "$ADMIN" \
    '{"tecnicoId":'$TEC_ID',"observacion":"Asignación manual por caída del despacho"}'
  expect "  -> pasa a ASIGNADO" .estado ASIGNADO

  docker start "$DISPATCH_CONTAINER" >/dev/null
  for _ in $(seq 1 40); do curl -sf "$DISPATCH/actuator/health" >/dev/null && break; sleep 1; done
  check "Despacho restaurado: nuevo ticket se asigna solo" 201 POST "$API/tickets" "$ANA" "$(ticket RED MEDIA BARRANQUILLA)"
  expect "  -> estado ASIGNADO" .estado ASIGNADO
else
  echo "  (omitida: docker no disponible o SKIP_DOCKER=1)"
fi

# =============================================================================
section "12. OBSERVABILIDAD (ACTUATOR / MICROMETER / PROMETHEUS)"
# =============================================================================
check "/actuator/info público" 200 GET "$API/actuator/info" ""
expect "  -> nombre de la aplicación" '.app.name' support-ticket-service
check "/actuator/metrics sin token" 401 GET "$API/actuator/metrics" ""
check "/actuator/metrics con usuario normal" 403 GET "$API/actuator/metrics" "$ANA"
check "/actuator/metrics con Admin" 200 GET "$API/actuator/metrics" "$ADMIN"
check "Métrica por categoría (RED)" 200 GET "$API/actuator/metrics/helpflow.tickets.created?tag=categoria:RED" "$ADMIN"
check "/actuator/prometheus con Admin" 200 GET "$API/actuator/prometheus" "$ADMIN"
PROM="$BODY"
for m in helpflow_tickets_created_total \
         helpflow_tickets_first_response_seconds_count \
         helpflow_tickets_resolution_seconds_count \
         helpflow_tickets_vencidos \
         helpflow_tickets_escalated_total \
         helpflow_auth_failures_total \
         helpflow_access_denied_total; do
  if echo "$PROM" | grep -q "^$m"; then ok "Métrica $m presente"
  else bad "Métrica $m presente" "no aparece en /actuator/prometheus"; fi
done
VENC=$(echo "$PROM" | grep '^helpflow_tickets_vencidos' | awk '{print $2}')
if [ "$DOCKER_OK" = "1" ]; then
  if awk -v v="${VENC:-0}" 'BEGIN{exit !(v>=1)}'; then ok "Tickets vencidos según SLA >= 1 (valor: $VENC)"
  else bad "Tickets vencidos según SLA >= 1" "valor: ${VENC:-ausente}"; fi
fi
echo "$PROM" | grep '^helpflow_' > evidencia-metricas.txt
echo "      métricas guardadas en evidencia-metricas.txt ($(wc -l < evidencia-metricas.txt) líneas)"

# =============================================================================
section "13. ERRORES ESTANDAR: PROBLEMDETAIL (RFC 7807) Y TRACEID"
# =============================================================================
PD=$(curl -s -X POST "$API/auth/login" -H 'X-Trace-Id: prueba-trace-0001' \
  -H 'Content-Type: application/json' -d '{"username":"x@helpflow.com","password":"mala"}')
BODY="$PD"
expect "ProblemDetail trae type,title,status,detail,instance,timestamp,traceId" \
  '[.type,.title,.status,.detail,.instance,.timestamp,.traceId]|all(. != null)' true
expect "El traceId enviado en X-Trace-Id se respeta" .traceId prueba-trace-0001
HDR=$(curl -si "$API/tickets" | tr -d '\r' | grep -i '^x-trace-id:')
if [ -n "$HDR" ]; then ok "La respuesta incluye header X-Trace-Id"; else bad "Header X-Trace-Id" "ausente"; fi
req GET "$API/tickets/abc" "$ADMIN"
assert_eq "Id no numérico responde 400" 400 "$CODE"

# =============================================================================
section "MUESTRAS PARA EVIDENCIA"
# =============================================================================
echo "Historial del ticket $T1 (ciclo de vida completo):"
req GET "$API/tickets/$T1/historial" "$ADMIN"
echo "$BODY" | jq -r '.[] | "  \(.fecha)  \(.estadoAnterior // "-") -> \(.estadoNuevo)  [\(.realizadoPor)]  \(.observacion)"'
echo
echo "Ejemplo de ProblemDetail:"
echo "$PD" | jq .
echo
echo "Líneas de métricas helpflow_*:"
head -n 40 evidencia-metricas.txt

# =============================================================================
printf '\n==================================================\n'
printf ' RESULTADO: %d correctas, %d fallidas\n' "$PASS" "$FAIL"
printf '==================================================\n'
[ "$FAIL" -eq 0 ]