$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendPath = Join-Path $repoRoot "backend"
$rootEnvPath = Join-Path $repoRoot ".env"
$zapDatabase = "piedra_zap_db"
$realm = "piedrazul-zap"
$clientId = "piedrazul-zap-frontend"
$baseUrl = "http://localhost:8080"
$environmentNames = @(
    "DB_NAME",
    "DB_USER",
    "DB_PASSWORD",
    "DB_HOST",
    "DB_PORT",
    "KEYCLOAK_ISSUER",
    "DJANGO_DEBUG",
    "DJANGO_ALLOWED_HOSTS",
    "ZAP_USERNAME",
    "ZAP_KEYCLOAK_USER_ID"
)
$originalEnvironment = @{}
foreach ($name in $environmentNames) {
    $originalEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
}

function Get-EnvValue([string]$Path, [string]$Name) {
    if (-not (Test-Path $Path)) {
        throw "No existe $Path. Copia el archivo de ejemplo correspondiente y configúralo primero."
    }

    $line = Get-Content $Path | Where-Object { $_ -match "^$Name=" } | Select-Object -First 1
    if (-not $line) {
        throw "Falta $Name en $Path."
    }

    return ($line -replace "^$Name=", "").Trim().Trim('"').Trim("'")
}

function Invoke-AdminJson(
    [string]$Method,
    [string]$Path,
    [object]$Body = $null
) {
    $parameters = @{
        Method = $Method
        Uri = "$baseUrl$Path"
        Headers = $script:adminHeaders
        ErrorAction = "Stop"
    }
    if ($null -ne $Body) {
        $parameters.ContentType = "application/json"
        $parameters.Body = ConvertTo-Json -InputObject $Body -Depth 20 -Compress
    }
    Invoke-RestMethod @parameters
}

function Get-AdminResourceOrNull([string]$Path) {
    try {
        return Invoke-AdminJson -Method Get -Path $Path
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 404) {
            return $null
        }
        throw
    }
}

Push-Location $repoRoot
try {
    $postgresUser = Get-EnvValue $rootEnvPath "POSTGRES_USER"
    $postgresPassword = Get-EnvValue $rootEnvPath "POSTGRES_PASSWORD"
    $postgresPort = Get-EnvValue $rootEnvPath "POSTGRES_PORT"
    $adminUsername = Get-EnvValue $rootEnvPath "KEYCLOAK_ADMIN"
    $adminPassword = Get-EnvValue $rootEnvPath "KEYCLOAK_ADMIN_PASSWORD"

    docker compose up -d postgres keycloak
    if ($LASTEXITCODE -ne 0) {
        throw "No se pudieron iniciar PostgreSQL y Keycloak."
    }

    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        try {
            $null = Invoke-WebRequest -Uri "$baseUrl/realms/master" -TimeoutSec 2
            $ready = $true
            break
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    if (-not $ready) {
        throw "Keycloak no respondió en $baseUrl después de 120 segundos."
    }

    $token = Invoke-RestMethod -Method Post `
        -Uri "$baseUrl/realms/master/protocol/openid-connect/token" `
        -Body @{
            username = $adminUsername
            password = $adminPassword
            grant_type = "password"
            client_id = "admin-cli"
        }
    $script:adminHeaders = @{ Authorization = "Bearer $($token.access_token)" }

    if ($null -eq (Get-AdminResourceOrNull "/admin/realms/$realm")) {
        Invoke-AdminJson -Method Post -Path "/admin/realms" -Body @{
            realm = $realm
            enabled = $true
            displayName = "Piedrazul - laboratorio ZAP"
            registrationAllowed = $false
            resetPasswordAllowed = $false
            loginWithEmailAllowed = $true
        }
    }

    $clients = Invoke-AdminJson -Method Get -Path "/admin/realms/$realm/clients?clientId=$clientId"
    if (-not ($clients | Where-Object { $_.clientId -eq $clientId })) {
        Invoke-AdminJson -Method Post -Path "/admin/realms/$realm/clients" -Body @{
            clientId = $clientId
            name = "Piedrazul ZAP local"
            enabled = $true
            protocol = "openid-connect"
            publicClient = $true
            standardFlowEnabled = $true
            directAccessGrantsEnabled = $false
            implicitFlowEnabled = $false
            redirectUris = @("http://localhost:4200/*")
            webOrigins = @("http://localhost:4200")
            attributes = @{ "pkce.code.challenge.method" = "S256" }
        }
    }

    $roles = @("PACIENTE", "AGENDADOR", "ADMIN", "MEDICO")
    $realmRoles = Invoke-AdminJson -Method Get -Path "/admin/realms/$realm/roles"
    foreach ($roleName in $roles) {
        if (-not ($realmRoles | Where-Object { $_.name -eq $roleName })) {
            Invoke-AdminJson -Method Post -Path "/admin/realms/$realm/roles" -Body @{
                name = $roleName
                description = "Rol local de pruebas ZAP: $roleName"
            }
        }
    }

    $passwordSecure = Read-Host "Contraseña temporal común para los usuarios de prueba (mínimo 12 caracteres)" -AsSecureString
    $password = [System.Net.NetworkCredential]::new("", $passwordSecure).Password
    if ($password.Length -lt 12) {
        throw "La contraseña de pruebas debe tener al menos 12 caracteres."
    }

    $testUsers = @(
        @{ Username = "paciente1"; Role = "PACIENTE"; FirstName = "Ana"; LastName = "Prueba"; Email = "paciente1@piedrazul.test" },
        @{ Username = "paciente2"; Role = "PACIENTE"; FirstName = "Luis"; LastName = "Prueba"; Email = "paciente2@piedrazul.test" },
        @{ Username = "agendador1"; Role = "AGENDADOR"; FirstName = "Alex"; LastName = "Agendador"; Email = "agendador1@piedrazul.test" },
        @{ Username = "admin1"; Role = "ADMIN"; FirstName = "Andrea"; LastName = "Admin"; Email = "admin1@piedrazul.test" },
        @{ Username = "medico1"; Role = "MEDICO"; FirstName = "Carla"; LastName = "Medica"; Email = "medico1@piedrazul.test" }
    )

    $provisionedUsers = @()
    foreach ($testUser in $testUsers) {
        $encodedUsername = [Uri]::EscapeDataString($testUser.Username)
        $foundUsers = Invoke-AdminJson -Method Get `
            -Path "/admin/realms/$realm/users?username=$encodedUsername"
        $user = $foundUsers | Where-Object { $_.username -eq $testUser.Username } | Select-Object -First 1
        $userPayload = @{
            username = $testUser.Username
            firstName = $testUser.FirstName
            lastName = $testUser.LastName
            email = $testUser.Email
            enabled = $true
            emailVerified = $true
            requiredActions = @()
        }

        if ($null -eq $user) {
            Invoke-AdminJson -Method Post -Path "/admin/realms/$realm/users" -Body $userPayload
            $foundUsers = Invoke-AdminJson -Method Get `
                -Path "/admin/realms/$realm/users?username=$encodedUsername"
            $user = $foundUsers | Where-Object { $_.username -eq $testUser.Username } | Select-Object -First 1
        } else {
            Invoke-AdminJson -Method Put -Path "/admin/realms/$realm/users/$($user.id)" -Body $userPayload
        }

        Invoke-AdminJson -Method Put `
            -Path "/admin/realms/$realm/users/$($user.id)/reset-password" `
            -Body @{ type = "password"; value = $password; temporary = $false }

        $role = Invoke-AdminJson -Method Get -Path "/admin/realms/$realm/roles/$($testUser.Role)"
        Invoke-AdminJson -Method Post `
            -Path "/admin/realms/$realm/users/$($user.id)/role-mappings/realm" `
            -Body @($role)
        $provisionedUsers += @{
            Username = $testUser.Username
            KeycloakUserId = $user.id
        }
        Write-Host "Usuario listo: $($testUser.Username) [$($testUser.Role)]"
    }

    $databaseExists = (
        & docker compose exec -T postgres psql `
            -U $postgresUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$zapDatabase'" |
            Out-String
    ).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "No fue posible consultar las bases de datos del contenedor PostgreSQL."
    }
    if ($databaseExists -notmatch "(?m)^1$") {
        docker compose exec -T postgres createdb -U $postgresUser $zapDatabase
        if ($LASTEXITCODE -ne 0) {
            throw "No se pudo crear la base aislada $zapDatabase."
        }
    }

    $env:DB_NAME = $zapDatabase
    $env:DB_USER = $postgresUser
    $env:DB_PASSWORD = $postgresPassword
    $env:DB_HOST = "localhost"
    $env:DB_PORT = $postgresPort
    $env:KEYCLOAK_ISSUER = "$baseUrl/realms/$realm"
    $env:DJANGO_DEBUG = "true"
    $env:DJANGO_ALLOWED_HOSTS = "localhost,127.0.0.1"
    Push-Location $backendPath
    try {
        & .\venv\Scripts\python.exe manage.py migrate
        if ($LASTEXITCODE -ne 0) {
            throw "No se pudieron aplicar las migraciones a $zapDatabase."
        }
        & .\venv\Scripts\python.exe manage.py seed_zap_data
        if ($LASTEXITCODE -ne 0) {
            throw "No se pudieron crear los datos sintéticos en $zapDatabase."
        }
    } finally {
        Pop-Location
    }

    foreach ($user in $provisionedUsers) {
        $env:ZAP_USERNAME = $user.Username
        $env:ZAP_KEYCLOAK_USER_ID = $user.KeycloakUserId
        Push-Location $backendPath
        try {
            & .\venv\Scripts\python.exe manage.py shell -c 'import os; from users.models import Usuario; updated = Usuario.objects.filter(username=os.environ["ZAP_USERNAME"]).update(keycloak_user_id=os.environ["ZAP_KEYCLOAK_USER_ID"]); assert updated == 1'
            if ($LASTEXITCODE -ne 0) {
                throw "No se pudo vincular $($user.Username) con su identidad de Keycloak."
            }
        } finally {
            Pop-Location
            Remove-Item Env:\ZAP_USERNAME, Env:\ZAP_KEYCLOAK_USER_ID -ErrorAction SilentlyContinue
        }
    }

    $backupDirectory = Join-Path $repoRoot "backups\zap"
    New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
    $backupPath = Join-Path $backupDirectory "piedra_zap_db_before_scan_$(Get-Date -Format 'yyyyMMdd-HHmmss').dump"
    docker compose exec -T postgres pg_dump `
        -U $postgresUser -d $zapDatabase --format=custom --no-owner --no-acl > $backupPath
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $backupPath) -or (Get-Item $backupPath).Length -eq 0) {
        throw "No se pudo crear el respaldo previo al análisis activo."
    }

    Write-Host ""
    Write-Host "Laboratorio listo. Realm: $realm; base: $zapDatabase."
    Write-Host "Respaldo previo al escaneo: $backupPath"
    Write-Host "Para iniciar Django y Angular, sigue las instrucciones de docs\OWASP-ZAP-LAB.md."
} finally {
    Pop-Location
    foreach ($name in $environmentNames) {
        if ($null -eq $originalEnvironment[$name]) {
            Remove-Item "Env:\$name" -ErrorAction SilentlyContinue
        } else {
            Set-Item "Env:\$name" $originalEnvironment[$name]
        }
    }
}
