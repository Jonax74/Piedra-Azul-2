# Publica las imagenes del stack en Docker Hub.
# Requiere: docker login
# Uso: .\docker\push-images.ps1

param(
    [string]$User = $env:DOCKERHUB_USER,
    [string]$Tag = $env:IMAGE_TAG
)

if (-not $User) { $User = "piedrazul" }
if (-not $Tag) { $Tag = "latest" }

$services = @(
    "api-gateway",
    "usuarios-service",
    "personas-service",
    "citas-service",
    "notifications-service"
)

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

foreach ($service in $services) {
    $image = "${User}/${service}:${Tag}"
    Write-Host "Building $image ..."
    docker build -t $image "./$service"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "Pushing $image ..."
    docker push $image
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

Write-Host "Imagenes publicadas bajo ${User}/*:${Tag}"
