# Laboratorio local de OWASP ZAP

Este laboratorio es exclusivo para pruebas autorizadas en `localhost`. Usa un
realm de Keycloak y una base PostgreSQL separados de los datos habituales. No
apuntes ZAP a producción ni a sistemas de terceros.

## Preparación

Requisitos: Docker Compose, PowerShell 7, Python del entorno virtual del
backend, dependencias de Django instaladas y Node.js/npm. Configura los
archivos locales `.env` y `backend/.env` a partir de sus ejemplos si aún no
existen; nunca compartas esos archivos ni pongas contraseñas reales en ellos.

Instala el generador OpenAPI en el entorno virtual del backend:

```powershell
cd backend
.\venv\Scripts\python.exe -m pip install -r requirements.txt
cd ..
```

Inicializa servicios, crea el realm `piedrazul-zap`, los usuarios sintéticos,
la base aislada `piedra_zap_db`, datos de prueba y un respaldo previo:

```powershell
.\scripts\prepare-zap-lab.ps1
```

El script pide una contraseña temporal para las cuentas. No la imprime ni la
guarda en el repositorio. Genera el respaldo en `backups/zap/`, ruta excluida
de Git. Si la base de pruebas ya existe, el script la conserva y actualiza sus
migraciones y datos sintéticos; no borra bases de datos.

## Levantar la aplicación

En una terminal PowerShell, inicia Django apuntando explícitamente a la base
aislada y al issuer del realm de pruebas:

```powershell
$env:DB_NAME = "piedra_zap_db"
$env:DB_HOST = "localhost"
$env:DB_PORT = "5432"
$env:KEYCLOAK_ISSUER = "http://localhost:8080/realms/piedrazul-zap"
$env:DJANGO_DEBUG = "true"
$env:DJANGO_ALLOWED_HOSTS = "localhost,127.0.0.1"
cd backend
python manage.py runserver 127.0.0.1:8000
```

En otra terminal:

```powershell
cd frontend-app
npm run start:zap
```

Comprueba que Angular responda en `http://localhost:4200`, Django en
`http://localhost:8000` y Keycloak en `http://localhost:8080`. El esquema
OpenAPI y Swagger UI están disponibles únicamente con `DJANGO_DEBUG=true`:

- `http://localhost:8000/api/schema/`
- `http://localhost:8000/api/docs/`

## Cuentas y datos sintéticos

Todas las cuentas comparten la contraseña temporal elegida durante la
preparación:

| Usuario | Rol |
|---|---|
| `paciente1` | `PACIENTE` |
| `paciente2` | `PACIENTE` |
| `agendador1` | `AGENDADOR` |
| `admin1` | `ADMIN` |
| `medico1` | `MEDICO` |

La base incluye personas, perfiles, disponibilidad semanal y una cita de
ejemplo. No contiene datos reales.

## Preparación de ZAP

1. Abre ZAP y limita el contexto/alcance a `http://localhost:4200` y
   `http://localhost:8000`. No incluyas dominios externos.
2. Importa `http://localhost:8000/api/schema/` mediante la función de importar
   OpenAPI. Configura la autenticación para el realm `piedrazul-zap` y prueba
   con cuentas sintéticas; no guardes sus tokens en el repositorio.
3. Antes de ejecutar un análisis activo, verifica que el respaldo `.dump` de
   `backups/zap/` existe. Un análisis activo puede crear o modificar datos
   sintéticos.
4. Incluye al menos estas dos comprobaciones OWASP Top 10 en el informe:
   - **A01: Broken Access Control**: compara, con cuentas de distintos roles,
     el acceso a operaciones y recursos de citas que pertenecen a otro
     paciente. Registra el código HTTP y el rol usado.
   - **A05: Security Misconfiguration**: revisa las alertas pasivas de ZAP
     sobre cabeceras, contenido mixto, cookies y respuestas de error; valida
     cada alerta antes de reportarla.
5. Guarda el informe de ZAP fuera del repositorio o en `zap-reports/`, que está
   ignorado por Git. En el informe breve incluye fecha, alcance local, versión
   de ZAP, alertas verificadas, severidad, evidencia resumida y limitaciones.

Para restaurar el respaldo en la base aislada, usa el archivo concreto creado
por el script y ejecuta:

```powershell
docker compose exec -T postgres pg_restore -U piedra_admin -d piedra_zap_db --clean --if-exists < .\backups\zap\ARCHIVO.dump
```

La restauración limpia únicamente objetos de `piedra_zap_db`; confirma el
nombre del archivo y destino antes de ejecutarla.
