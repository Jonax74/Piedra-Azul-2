# Brief de construcción para IA o agente

## 1. Rol del agente

Actuar como arquitecto/desarrollador DevOps para transformar el estado actual del repositorio en CI/CD funcional y despliegue cloud académico.

El agente debe:

- Trabajar contra el código real.
- Ejecutar y verificar cambios.
- Crear infraestructura reproducible.
- No inventar decisiones de proveedor.
- Documentar resultados y pendientes.
- Mantener seguridad y trazabilidad.

## 2. Documentos fuente

Leer en este orden:

1. `docs/ESTADO_ACTUAL_PROYECTO.md`
2. `docs/DISENO_CI_CD.md`
3. `docs/DISENO_DESPLIEGUE_CLOUD.md`
4. `Guia_Diagramas_Arquitectura_CI_CD_Despliegue.md`
5. `EntregablesTercerCorte2026.md`
6. `README.MD`
7. `docker-compose.yml`

## 3. Restricciones

- Rama principal de trabajo: `develop`.
- No subir secretos reales.
- No publicar PostgreSQL ni administración Keycloak.
- No usar credenciales demo en producción.
- No ejecutar servicios de pago.
- No usar `runserver` o `ng serve` en producción.
- No usar `start-dev` en producción.
- No suponer que proveedor o dominio están elegidos.
- No reemplazar la arquitectura sin ADR y aprobación.
- No borrar datos persistentes durante rollback.

## 4. Estado inicial

El proyecto ya tiene:

- Angular 22.
- Django 6.
- Keycloak 26.
- PostgreSQL 16.
- Dockerfiles básicos.
- Docker Compose local.
- Pruebas backend y frontend.

El proyecto aún no tiene:

- GitHub Actions.
- Dockerfiles de producción validados.
- Health endpoints de operación.
- IaC cloud.
- Secretos seguros.
- Regiones/dominios.
- Registro de despliegue.
- Respaldo restaurable.

## 5. Fases de ejecución

### Fase A: validación local

Tareas:

1. Ejecutar Compose desde cero.
2. Ejecutar pruebas backend.
3. Ejecutar build y pruebas frontend.
4. Registrar fallos reales.
5. Identificar versión efectiva de Dockerfiles.

Criterio:

- El estado local arranca.
- Los fallos quedan separados entre entorno y código.

### Fase B: preparación de producción

Tareas:

1. Crear Dockerfile backend de producción.
2. Crear Dockerfile frontend de producción.
3. Configurar Gunicorn o servidor WSGI equivalente.
4. Cambiar Angular SSR para no usar `ng serve`.
5. Añadir health check.
6. Convertir configuración por variables.
7. Añadir CORS por dominio.

Criterio:

- Las imágenes pueden ejecutarse sin bind mounts.
- Las imágenes no contienen `.env` ni secretos.

### Fase C: CI

Tareas:

1. Crear workflow de pull request.
2. Crear workflow de build.
3. Crear workflow de despliegue.
4. Configurar concurrencia.
5. Configurar protección de rama.
6. Añadir escaneo básico.
7. Añadir ZAP pasivo dentro de alcance.

Criterio:

- Un PR no puede fusionarse si falla.
- Los artefactos se etiquetan por SHA.
- El despliegue usa secrets desde GitHub.

### Fase D: selección cloud

Tareas:

1. Investigar cuotas vigentes.
2. Comparar al menos dos proveedores.
3. Verificar HTTPS, PostgreSQL, contenedores y registro.
4. Verificar que no se requiera tarjeta.
5. Crear ADR de decisión.

Criterio:

- Se registra una opción verificable.
- No se presupone una API de proveedor.

### Fase E: despliegue

Tareas:

1. Crear cuenta o recursos cloud solo con autorización.
2. Crear IaC versionado.
3. Crear dominios o URLs.
4. Configurar TLS.
5. Configurar secretos.
6. Desplegar PostgreSQL.
7. Desplegar Keycloak.
8. Ejecutar migraciones.
9. Desplegar backend.
10. Desplegar frontend.

Criterio:

- Las URLs reales funcionan.
- El issuer coincide con el dominio público.
- PostgreSQL no es público.

### Fase F: verificación y rollback

Tareas:

1. Ejecutar smoke test.
2. Ejecutar ZAP dentro de alcance.
3. Ejecutar una restauración de respaldo.
4. Simular health check fallido.
5. Ejecutar rollback.
6. Documentar evidencias.

Criterio:

- El despliegue queda demostrable.
- El rollback preserva datos.

## 6. Comandos iniciales

```powershell
docker compose config
docker compose up --build -d
docker compose ps

docker compose exec backend python manage.py check
docker compose exec backend python manage.py makemigrations --check --dry-run
docker compose exec backend python manage.py test

docker compose run --rm frontend npm ci
docker compose exec frontend npm run build -- --configuration production
docker compose exec frontend npm test -- --watch=false
```

No registrar los resultados como exitosos hasta ejecutarlos.

## 7. Archivos esperados

```text
.github/workflows/ci.yml
.github/workflows/deploy.yml
backend/Dockerfile
backend/Dockerfile.prod
frontend-app/Dockerfile
frontend-app/Dockerfile.prod
infra/
  README.md
  provider-adr.md
  main.*
scripts/
  smoke-test.ps1
  backup-postgres.ps1
  restore-postgres.ps1
docs/
  ESTADO_ACTUAL_PROYECTO.md
  DISENO_CI_CD.md
  DISENO_DESPLIEGUE_CLOUD.md
  BRIEF_CONSTRUCCION_AGENT_AI.md
```

Estos nombres son recomendaciones; el agente debe adaptarlos al proveedor elegido sin duplicar soluciones.

## 8. Prompt recomendado para continuar

Usar el siguiente prompt con otra IA o agente:

> Implementa por fases el diseño documentado en `docs/BRIEF_CONSTRUCCION_AGENT_AI.md`. Comienza por validar el estado local sin modificar archivos. Después crea una baseline de CI, luego prepara imágenes de producción, selecciona proveedor cloud solo después de comprobar cuotas vigentes sin tarjeta y crea IaC. Ejecuta los tests reales y reporta errores con evidencia. No agregues secretos al repositorio. En cada fase, detente si una decisión cambia funcionalidad o arquitectura y solicita aprobación.
