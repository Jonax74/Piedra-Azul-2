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

$realmConfiguration = Invoke-RestMethod -Headers $headers `
    -Uri "$keycloakUrl/admin/realms/$realm"
$realmConfiguration | Add-Member -MemberType NoteProperty `
    -Name loginTheme -Value "piedrazul" -Force
$realmConfiguration | Add-Member -MemberType NoteProperty `
    -Name internationalizationEnabled -Value $true -Force
$realmConfiguration | Add-Member -MemberType NoteProperty `
    -Name defaultLocale -Value "es" -Force
$realmConfiguration | Add-Member -MemberType NoteProperty `
    -Name supportedLocales -Value @("es") -Force

Invoke-RestMethod -Method Put -Headers $headers `
    -Uri "$keycloakUrl/admin/realms/$realm" `
    -ContentType "application/json" `
    -Body ($realmConfiguration | ConvertTo-Json -Depth 100)

Write-Host "Tema Piedrazul activado para el realm $realm."
