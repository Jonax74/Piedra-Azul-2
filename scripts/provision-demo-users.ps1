param(
    [string]$Password = "Piedrazul123!"
)

$ErrorActionPreference = "Stop"

function Get-EnvValue([string]$Name) {
    $line = Get-Content .env | Where-Object { $_ -match "^$Name=" } | Select-Object -First 1
    if (-not $line) {
        throw "Falta $Name en .env"
    }

    return ($line -replace "^$Name=", "")
}

$keycloakUrl = "http://localhost:$((Get-EnvValue 'KEYCLOAK_PORT'))"
$adminUsername = Get-EnvValue 'KEYCLOAK_ADMIN'
$adminPassword = Get-EnvValue 'KEYCLOAK_ADMIN_PASSWORD'
$realm = "piedrazul"

$tokenResponse = Invoke-RestMethod -Method Post `
    -Uri "$keycloakUrl/realms/master/protocol/openid-connect/token" `
    -Body @{
        username = $adminUsername
        password = $adminPassword
        grant_type = "password"
        client_id = "admin-cli"
    }
$headers = @{ Authorization = "Bearer $($tokenResponse.access_token)" }

$demoUsers = @(
    @{ Username = "admin.prueba"; FirstName = "Admin"; LastName = "Prueba"; Email = "admin@piedrazul.local"; Role = "ADMIN" },
    @{ Username = "agendador.prueba"; FirstName = "Agendador"; LastName = "Prueba"; Email = "agendador@piedrazul.local"; Role = "AGENDADOR" },
    @{ Username = "medico.prueba"; FirstName = "Medico"; LastName = "Prueba"; Email = "medico@piedrazul.local"; Role = "MEDICO" },
    @{ Username = "medico.carlos"; FirstName = "Carlos"; LastName = "Mora"; Email = "carlos@piedrazul.local"; Role = "MEDICO" },
    @{ Username = "paciente.prueba"; FirstName = "Paciente"; LastName = "Prueba"; Email = "paciente@piedrazul.local"; Role = "PACIENTE" },
    @{ Username = "paciente.ana"; FirstName = "Ana"; LastName = "Torres"; Email = "ana@piedrazul.local"; Role = "PACIENTE" }
)

foreach ($demoUser in $demoUsers) {
    $encodedUsername = [Uri]::EscapeDataString($demoUser.Username)
    $users = Invoke-RestMethod -Headers $headers `
        -Uri "$keycloakUrl/admin/realms/$realm/users?username=$encodedUsername"
    $user = $users | Where-Object { $_.username -eq $demoUser.Username } | Select-Object -First 1

    if (-not $user) {
        Invoke-RestMethod -Method Post -Headers $headers `
            -Uri "$keycloakUrl/admin/realms/$realm/users" `
            -ContentType "application/json" `
            -Body (@{
                username = $demoUser.Username
                firstName = $demoUser.FirstName
                lastName = $demoUser.LastName
                email = $demoUser.Email
                enabled = $true
                emailVerified = $true
            } | ConvertTo-Json)

        $users = Invoke-RestMethod -Headers $headers `
            -Uri "$keycloakUrl/admin/realms/$realm/users?username=$encodedUsername"
        $user = $users | Where-Object { $_.username -eq $demoUser.Username } | Select-Object -First 1
    }

    Invoke-RestMethod -Method Put -Headers $headers `
        -Uri "$keycloakUrl/admin/realms/$realm/users/$($user.id)" `
        -ContentType "application/json" `
        -Body (@{
            username = $demoUser.Username
            firstName = $demoUser.FirstName
            lastName = $demoUser.LastName
            email = $demoUser.Email
            enabled = $true
            emailVerified = $true
            requiredActions = @()
        } | ConvertTo-Json)

    Invoke-RestMethod -Method Put -Headers $headers `
        -Uri "$keycloakUrl/admin/realms/$realm/users/$($user.id)/reset-password" `
        -ContentType "application/json" `
        -Body (@{
            type = "password"
            value = $Password
            temporary = $false
        } | ConvertTo-Json)

    $role = Invoke-RestMethod -Headers $headers `
        -Uri "$keycloakUrl/admin/realms/$realm/roles/$($demoUser.Role)"
    $rolePayload = ConvertTo-Json -InputObject ([array]$role) -Depth 5
    Invoke-RestMethod -Method Post -Headers $headers `
        -Uri "$keycloakUrl/admin/realms/$realm/users/$($user.id)/role-mappings/realm" `
        -ContentType "application/json" `
        -Body $rolePayload

    $sql = "UPDATE usuario SET keycloak_user_id = '$($user.id)' WHERE username = '$($demoUser.Username)';"
    docker compose exec -T postgres psql -U (Get-EnvValue 'POSTGRES_USER') -d (Get-EnvValue 'POSTGRES_DB') -c $sql | Out-Null
    Write-Host "Configurado: $($demoUser.Username) [$($demoUser.Role)]"
}

Write-Host "Contrasena de prueba para todas las cuentas: $Password"