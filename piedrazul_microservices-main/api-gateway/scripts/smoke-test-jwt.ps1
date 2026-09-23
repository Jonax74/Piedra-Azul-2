# ============================================================================
# Smoke test del API Gateway con JWT de Keycloak (Fase 1 del sprint).
#
# Que valida:
#   1. Acceso publico a /actuator/health (sin token).
#   2. Rechazo (401) de rutas /api/** sin token (excepto rutas declaradas publicas).
#   3. Aceptacion (2xx/4xx de negocio, no 401/403) con token valido.
#   4. Rechazo (403) cuando el rol del token no autoriza la ruta.
#   5. Rutas declaradas publicas (POST /api/usuarios, /api/auth/**) no exigen token.
#
# Pre-requisitos:
#   - Keycloak corriendo en http://localhost:8080 con realm 'piedrazul'.
#   - API Gateway corriendo en http://localhost:8085.
#   - Los 4 usuarios de prueba creados en Keycloak:
#       admin.test, agendador.test, paciente.test, medico.test
#     con la MISMA contrasena (configurable via parametro o variable de entorno).
#   - Microservicios downstream NO necesitan estar arriba: cuando estan caidos el
#     gateway devuelve 503/504, lo cual ocurre DESPUES de la autenticacion y
#     autorizacion. Por eso aqui solo importa distinguir 401/403 vs el resto.
#
# Uso:
#   .\scripts\smoke-test-jwt.ps1                           # default: Abc123*
#   .\scripts\smoke-test-jwt.ps1 -Password 'MiPass.123'    # override por parametro
#   $env:PIEDRAZUL_TEST_PASSWORD='MiPass.123'; .\scripts\smoke-test-jwt.ps1
# ============================================================================

param(
    [string]$Password = $(if ($env:PIEDRAZUL_TEST_PASSWORD) { $env:PIEDRAZUL_TEST_PASSWORD } else { 'Abc123*' }),
    [string]$KeycloakUrl = 'http://localhost:8080',
    [string]$GatewayUrl  = 'http://localhost:8085',
    [string]$Realm       = 'piedrazul',
    [string]$ClientId    = 'piedrazul-frontend'
)

$ErrorActionPreference = 'Stop'

# --- helpers ---------------------------------------------------------------

function Get-Token {
    param([string]$Username)

    $body = @{
        grant_type = 'password'
        client_id  = $ClientId
        username   = $Username
        password   = $Password
        scope      = 'openid'
    }
    try {
        $resp = Invoke-RestMethod `
            -Method POST `
            -Uri "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token" `
            -ContentType 'application/x-www-form-urlencoded' `
            -Body $body
        return $resp.access_token
    } catch {
        Write-Host ("  [FAIL] No se pudo obtener token para '{0}': {1}" -f $Username, $_.Exception.Message) -ForegroundColor Red
        Write-Host ("         Verifica que el usuario existe en Keycloak y la contrasena es: {0}" -f $Password) -ForegroundColor Yellow
        throw
    }
}

function Invoke-GatewayRequest {
    param(
        [string]$Method,
        [string]$Path,
        [string]$Token = $null
    )
    $headers = @{}
    if ($Token) { $headers['Authorization'] = "Bearer $Token" }

    try {
        $resp = Invoke-WebRequest `
            -Method $Method `
            -Uri "$GatewayUrl$Path" `
            -Headers $headers `
            -SkipHttpErrorCheck `
            -ErrorAction Stop
        return [int]$resp.StatusCode
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        return 0
    }
}

function Assert-Status {
    param(
        [string]$Description,
        [int]$Actual,
        [int[]]$ExpectedAny
    )
    if ($ExpectedAny -contains $Actual) {
        Write-Host ("  [OK]   {0,-60} -> {1}" -f $Description, $Actual) -ForegroundColor Green
    } else {
        Write-Host ("  [FAIL] {0,-60} -> {1} (esperado: {2})" -f $Description, $Actual, ($ExpectedAny -join '|')) -ForegroundColor Red
        $script:failures++
    }
}

# --- ejecucion -------------------------------------------------------------

$script:failures = 0

Write-Host ''
Write-Host "Gateway: $GatewayUrl"
Write-Host "Keycloak: $KeycloakUrl  (realm=$Realm, client=$ClientId)"
Write-Host ''
Write-Host '=== 1) Acceso publico (actuator) ===' -ForegroundColor Cyan
Assert-Status -Description 'GET /actuator/health (sin token)' `
              -Actual (Invoke-GatewayRequest -Method GET -Path '/actuator/health') `
              -ExpectedAny @(200)

# Cuando el rol es correcto, el gateway hace forward y la respuesta depende del
# microservicio downstream: si esta arriba devuelve 200/4xx de negocio, si esta
# abajo devuelve 503/504. En todos los casos NO debe ser 401 ni 403.
$nonRejectStatuses = @(200, 201, 204, 400, 404, 405, 415, 422, 500, 503, 504)

Write-Host ''
Write-Host '=== 2) Rutas protegidas sin token -> 401 ===' -ForegroundColor Cyan
Assert-Status -Description 'GET /api/usuarios (sin token)' `
              -Actual (Invoke-GatewayRequest -Method GET -Path '/api/usuarios') `
              -ExpectedAny @(401)
Assert-Status -Description 'GET /api/citas (sin token)' `
              -Actual (Invoke-GatewayRequest -Method GET -Path '/api/citas') `
              -ExpectedAny @(401)
Assert-Status -Description 'GET /api/configuracion (sin token)' `
              -Actual (Invoke-GatewayRequest -Method GET -Path '/api/configuracion') `
              -ExpectedAny @(401)

Write-Host ''
Write-Host '=== 3) Rutas declaradas publicas no exigen token ===' -ForegroundColor Cyan
# POST /api/usuarios es autoservicio de registro (paridad con sistema actual).
Assert-Status -Description 'POST /api/usuarios (sin token, autoservicio) -> no 401' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/usuarios') `
              -ExpectedAny $nonRejectStatuses
# /api/auth/** sigue publico (temporal Fase 1, eliminar al cerrar Fase 3).
Assert-Status -Description 'POST /api/auth/login (sin token, legacy) -> no 401' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/auth/login') `
              -ExpectedAny $nonRejectStatuses

Write-Host ''
Write-Host '=== 4) Obtencion de tokens por rol ===' -ForegroundColor Cyan
$tokenAdmin     = Get-Token -Username 'admin.test'
$tokenAgendador = Get-Token -Username 'agendador.test'
$tokenPaciente  = Get-Token -Username 'paciente.test'
$tokenMedico    = Get-Token -Username 'medico.test'
Write-Host '  [OK]   admin.test, agendador.test, paciente.test, medico.test obtuvieron token' -ForegroundColor Green

Write-Host ''
Write-Host '=== 5) Autorizacion por rol (gateway acepta o rechaza con 403) ===' -ForegroundColor Cyan

# Lectura abierta a cualquier autenticado
Assert-Status -Description 'GET /api/usuarios (PACIENTE) -> no 401/403' `
              -Actual (Invoke-GatewayRequest -Method GET -Path '/api/usuarios' -Token $tokenPaciente) `
              -ExpectedAny $nonRejectStatuses

# Configuracion solo ADMINISTRADOR
Assert-Status -Description 'PUT /api/configuracion (ADMIN) -> no 401/403' `
              -Actual (Invoke-GatewayRequest -Method PUT -Path '/api/configuracion' -Token $tokenAdmin) `
              -ExpectedAny $nonRejectStatuses
Assert-Status -Description 'PUT /api/configuracion (PACIENTE) -> 403' `
              -Actual (Invoke-GatewayRequest -Method PUT -Path '/api/configuracion' -Token $tokenPaciente) `
              -ExpectedAny @(403)

# Disponibilidad: ADMINISTRADOR o MEDICO_TERAPISTA
Assert-Status -Description 'POST /api/disponibilidad (MEDICO) -> no 401/403' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/disponibilidad' -Token $tokenMedico) `
              -ExpectedAny $nonRejectStatuses
Assert-Status -Description 'POST /api/disponibilidad (PACIENTE) -> 403' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/disponibilidad' -Token $tokenPaciente) `
              -ExpectedAny @(403)

# Medicos: escritura solo ADMINISTRADOR
Assert-Status -Description 'POST /api/medicos (ADMIN) -> no 401/403' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/medicos' -Token $tokenAdmin) `
              -ExpectedAny $nonRejectStatuses
Assert-Status -Description 'POST /api/medicos (AGENDADOR) -> 403' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/medicos' -Token $tokenAgendador) `
              -ExpectedAny @(403)

# Personas/Pacientes: escritura ADMINISTRADOR o AGENDADOR
Assert-Status -Description 'POST /api/pacientes (AGENDADOR) -> no 401/403' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/pacientes' -Token $tokenAgendador) `
              -ExpectedAny $nonRejectStatuses
Assert-Status -Description 'POST /api/pacientes (PACIENTE) -> 403' `
              -Actual (Invoke-GatewayRequest -Method POST -Path '/api/pacientes' -Token $tokenPaciente) `
              -ExpectedAny @(403)

# Citas: cualquier autenticado pasa la autorizacion en el gateway
Assert-Status -Description 'GET /api/citas (PACIENTE) -> no 401/403' `
              -Actual (Invoke-GatewayRequest -Method GET -Path '/api/citas' -Token $tokenPaciente) `
              -ExpectedAny $nonRejectStatuses

Write-Host ''
if ($script:failures -eq 0) {
    Write-Host 'TODOS LOS SMOKE TESTS PASARON' -ForegroundColor Green
    exit 0
} else {
    Write-Host ("$script:failures FALLO(S). Revisa el detalle arriba.") -ForegroundColor Red
    exit 1
}
