<<<<<<< Updated upstream
=======
# Piedra Azul

Sistema web para la gestion de pacientes, profesionales, disponibilidades y
citas medicas. El proyecto esta compuesto por:

- **Backend:** Django y Django REST Framework.
- **Frontend:** Angular.
- **Base de datos:** PostgreSQL.
- **Autenticacion:** Keycloak mediante OpenID Connect.
- **Infraestructura local:** Docker Compose.

## Requisitos

Instala las siguientes herramientas antes de iniciar el proyecto:

- Docker Desktop con Docker Compose.
- Python y `pip`.
- Node.js y `npm`.
- PowerShell.

Comprueba las instalaciones:

```powershell
docker --version
docker compose version
python --version
node --version
npm.cmd --version
```

## Configuracion inicial

Desde la raiz del repositorio, crea el archivo `.env` a partir de
`.env.example`:

```powershell
Copy-Item .env.example .env
```

Los valores locales incluidos son:

```env
POSTGRES_DB=piedra_db
POSTGRES_USER=piedra_admin
POSTGRES_PASSWORD=piedra_password
POSTGRES_PORT=5432

KEYCLOAK_DB=keycloak_db
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin123
KEYCLOAK_PORT=8080
```

El backend usa las variables `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_HOST` y
`DB_PORT`. Crea `backend/.env` con esta configuracion:

```env
DB_NAME=piedra_db
DB_USER=piedra_admin
DB_PASSWORD=piedra_password
DB_HOST=localhost
DB_PORT=5432
DJANGO_DEBUG=True
DJANGO_SECRET_KEY=clave-local-insegura
```

## Levantar PostgreSQL y Keycloak

Ejecuta desde la raiz:

```powershell
docker compose up -d
docker compose ps
```

Los servicios quedan disponibles en:

- PostgreSQL: `localhost:5432`
- Keycloak: `http://localhost:8080`

Para detenerlos:

```powershell
docker compose down
```

Para eliminar tambien el volumen local de PostgreSQL y empezar desde cero:

```powershell
docker compose down -v
```

Este ultimo comando elimina los datos locales de la base de datos.

## Inicializar datos demo

El archivo `docker/postgres/init-databases.sql` crea la base de datos de
Keycloak durante la primera inicializacion del volumen. Los datos funcionales
de demostracion se cargan por separado.

Con PostgreSQL y Keycloak activos, ejecuta:

```powershell
Get-Content .\docker\postgres\seed-demo.sql | docker compose exec -T postgres psql -U piedra_admin -d piedra_db
```

El script es idempotente para los registros demo principales y puede ejecutarse
nuevamente si es necesario.

## Configurar usuarios de Keycloak

El realm utilizado por la aplicacion es `piedrazul`. Para crear o actualizar
las cuentas demo, ejecuta:

```powershell
.\scripts\provision-demo-users.ps1
```

La contrasena predeterminada del script es `Piedrazul123!`. Tambien se puede
definir otra contrasena:

```powershell
.\scripts\provision-demo-users.ps1 -Password "NuevaClave123!"
```

El script crea las cuentas, asigna roles, relaciona el usuario de Keycloak con
el usuario de PostgreSQL y sincroniza los identificadores.

### Usuarios demo

| Usuario | Rol | Contrasena predeterminada |
| --- | --- | --- |
| `admin.prueba` | `ADMIN` | `Piedrazul123!` |
| `agendador.prueba` | `AGENDADOR` | `Piedrazul123!` |
| `medico.prueba` | `MEDICO` | `Piedrazul123!` |
| `medico.carlos` | `MEDICO` | `Piedrazul123!` |
| `paciente.prueba` | `PACIENTE` | `Piedrazul123!` |
| `paciente.ana` | `PACIENTE` | `Piedrazul123!` |

Los roles controlan las funciones visibles en el frontend:

- `PACIENTE`: agendar cita y consultar sus citas.
- `AGENDADOR`: agendar y consultar la agenda general.
- `MEDICO`: consultar disponibilidad.
- `ADMIN`: acceder a agenda, medicos, disponibilidades y configuracion.

Para crear manualmente otro usuario, primero crealo en Keycloak dentro del
realm `piedrazul`, asignale un rol y luego registra o sincroniza el usuario en
la tabla `usuario` de PostgreSQL. La contrasena siempre se administra en
Keycloak, no en la tabla `usuario`.

## Ejecutar el backend

Desde la raiz del repositorio:

```powershell
Push-Location backend
python -m venv venv
.\venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python manage.py migrate
python manage.py runserver 127.0.0.1:8000
Pop-Location
```

Si el entorno virtual ya existe, basta con activarlo e iniciar Django:

```powershell
Push-Location backend
.\venv\Scripts\Activate.ps1
python manage.py runserver 127.0.0.1:8000
Pop-Location
```

La API queda disponible en `http://127.0.0.1:8000/api/`.

Rutas principales:

- `GET /api/me/`
- `GET /api/me/profile/`
- `GET /api/personas/`
- `GET /api/pacientes/`
- `GET /api/medicos/`
- `GET /api/citas/`
- `GET /api/agenda/`
- `GET /api/disponibilidades/`

## Ejecutar el frontend

En otra terminal, desde la raiz:

```powershell
Push-Location frontend-app
npm.cmd install
npm.cmd start
Pop-Location
```

Abre `http://localhost:4200` en el navegador. El frontend usa por defecto:

- API: `http://127.0.0.1:8000/api`
- Keycloak: `http://localhost:8080`
- Realm: `piedrazul`
- Cliente: `piedrazul-frontend`

## Funcionalidades principales

- Inicio de sesion mediante Keycloak.
- Dashboard segun el rol del usuario.
- Consulta y creacion de pacientes y profesionales.
- Configuracion de disponibilidades de lunes a viernes.
- Agendamiento de citas en horarios disponibles.
- Consulta de citas propias para pacientes.
- Agenda general para administradores y agendadores.
- Ordenamiento de citas por fecha y hora.
- Ordenamiento de disponibilidades por dia y hora.
- Validacion de fechas, horas, intervalos, cruces de horarios y permisos.

## Pruebas y compilacion

Backend:

```powershell
Push-Location backend
python manage.py test
Pop-Location
```

Frontend:

```powershell
Push-Location frontend-app
npm.cmd run build
npm.cmd test -- --watch=false
Pop-Location
```

El build genera los archivos en `frontend-app/dist/`. Las pruebas frontend
requieren que los servicios usados por los componentes esten simulados o que
el backend este disponible en `127.0.0.1:8000`.

## Solucion de problemas

### No conecta a PostgreSQL

Comprueba que el contenedor este saludable:

```powershell
docker compose ps
docker compose logs postgres
```

Verifica que el puerto `5432` no este ocupado por otra instalacion de
PostgreSQL.

### Keycloak no permite iniciar sesion

Confirma que el contenedor este activo y que el usuario pertenezca al realm
`piedrazul`, no al realm `master`. La URL de administracion es
`http://localhost:8080`.

### El usuario entra pero no ve funciones

El rol debe estar asignado en Keycloak y debe coincidir exactamente con el
nombre esperado por la aplicacion: `ADMIN`, `AGENDADOR`, `MEDICO` o `PACIENTE`.
Despues de asignar un rol, cierra sesion y vuelve a ingresar para obtener un
token actualizado.

### El frontend no puede llamar a la API

Inicia Django en el puerto `8000` y verifica que el frontend este usando la
URL configurada en `frontend-app/src/environments/environment.ts`.

## Estructura del proyecto

```text
backend/       API Django, modelos, serializers y autenticacion
docker/        Scripts SQL de inicializacion y datos demo
frontend-app/  Aplicacion Angular
scripts/       Automatizacion de usuarios demo en Keycloak
```

### Easter egg añadido :D
>>>>>>> Stashed changes
