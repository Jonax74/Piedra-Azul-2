# Piedrazul: sistema de citas médicas

Aplicación web para administrar la agenda de Piedrazul. Los pacientes pueden
registrarse y gestionar sus citas; el personal consulta agendas y administra
profesionales y parámetros del sistema según su rol.

## Funcionalidades

- Registro de pacientes e inicio de sesión con Keycloak.
- Gestión de médicos, terapistas, especialidades y disponibilidades.
- Consulta de fechas y horarios disponibles y reserva de citas.
- Consulta, confirmación, cancelación y reagendamiento de citas.
- Validación de transiciones de estado y prevención de reservas duplicadas.
- Consulta de agendas por profesional y fecha, con historial de citas por
paciente.
- Exportación de agendas a CSV para los perfiles autorizados.
- Configuración de la ventana de agendamiento y otros parámetros del sistema.

### Acceso por rol

| Rol | Funciones principales |
| --- | --- |
| `PACIENTE` | Agendar, consultar, confirmar, cancelar o reagendar sus citas. |
| `MEDICO` | Consultar su agenda y gestionar su disponibilidad. |
| `AGENDADOR` | Consultar agendas, buscar pacientes y gestionar citas. |
| `ADMIN` | Administrar médicos, disponibilidades y configuración del sistema. |

El backend también aplica permisos a las operaciones; ocultar una ruta en el
frontend no reemplaza la autorización del servidor.

## Tecnologías y arquitectura

- **Frontend:** Angular, TypeScript y Bootstrap.
- **Backend:** Django y Django REST Framework.
- **Base de datos:** PostgreSQL.
- **Autenticación:** Keycloak y tokens JWT.
- **Entorno local:** Docker Compose para PostgreSQL y Keycloak.

El backend actual está organizado como un monolito modular: `persons` contiene
personas, pacientes, médicos y disponibilidades; `appointments` contiene citas,
agendas y configuración; `users` gestiona usuarios y registro; `notifications`
contiene notificaciones. El frontend Angular consume la API REST.

El modelo de citas utiliza estados como `PROGRAMADA`, `CONFIRMADA`,
`CANCELADA`, `REAGENDADA`, `ATENDIDA` y `NO_ASISTIDA`. Las transiciones se
validan en el backend. La gestión del estado de la cita implementa el patrón
State.

### Estructura principal

```text
backend/                 API Django y lógica del dominio
  appointments/           Citas, agenda, estados y configuración
  persons/                Pacientes, médicos y disponibilidades
  users/                  Usuarios y registro
  config/                 Configuración y rutas de la API
frontend-app/             Aplicación Angular
docker/                   Inicialización de PostgreSQL y tema de Keycloak
scripts/                  Scripts locales para usuarios demo y tema
docker-compose.yml        PostgreSQL y Keycloak para desarrollo
```

## Requisitos para desarrollo local

- Windows con PowerShell (los comandos siguientes usan PowerShell).
- Git.
- Python y `pip`.
- Node.js y npm, compatibles con las dependencias de `frontend-app`.
- Docker Desktop con Docker Compose.

## Configuración e inicio

### 1. Crear los archivos de entorno

Desde la raíz del repositorio, crea los archivos locales a partir de sus
ejemplos. Hazlo solo si todavía no existen:

```powershell
Copy-Item .env.example .env
Copy-Item backend\.env.example backend\.env
```

El `.env` de la raíz configura los contenedores de Docker. `backend\.env`
configura Django. Asegúrate de que `DB_NAME`, `DB_USER` y `DB_PASSWORD` en
`backend\.env` coincidan con `POSTGRES_DB`, `POSTGRES_USER` y
`POSTGRES_PASSWORD` del `.env` de la raíz. Los puertos de PostgreSQL también
deben coincidir (`DB_PORT` y `POSTGRES_PORT`).

Django carga `backend\.env` antes que el `.env` raíz. Si una variable está en
ambos archivos, prevalece la de `backend\.env`. Si no están definidos los
valores `DB_*`, Django puede tomar como respaldo las variables `POSTGRES_*`.
Los ejemplos contienen credenciales únicamente para desarrollo local; no las
uses en producción ni publiques archivos `.env`.

### 2. Iniciar PostgreSQL y Keycloak

Desde la raíz del proyecto:

```powershell
docker compose up -d
docker compose ps
```

Compose inicia PostgreSQL en el puerto indicado por `POSTGRES_PORT` y Keycloak
en el puerto indicado por `KEYCLOAK_PORT` (por defecto, `8080`). PostgreSQL
crea la base de datos de Keycloak usando `docker/postgres/init-databases.sql`
al inicializar un volumen de datos por primera vez. Compose solo inicia estos
servicios de infraestructura; Django y Angular se ejecutan por separado con
los pasos siguientes.

El contenedor **no crea automáticamente el realm de la aplicación**. En la
consola de administración de Keycloak (`http://localhost:8080/admin/`):

1. Crea el realm `piedrazul`.
2. Crea un cliente OpenID Connect llamado `piedrazul-frontend`, con
   autenticación de cliente desactivada y flujo estándar activado. Agrega
   `http://localhost:4200/*` como URI de redirección válida y
   `http://localhost:4200` como origen web.
3. Crea los roles de realm `ADMIN`, `AGENDADOR`, `MEDICO` y `PACIENTE`.

El realm, cliente e issuer deben coincidir con `KEYCLOAK_REALM`,
`KEYCLOAK_CLIENT_ID` y `KEYCLOAK_ISSUER` del `backend\.env` y con la
configuración del frontend en `frontend-app/src/environments/environment.ts`.
Para abrir la consola se usan las credenciales de administración configuradas
por `KEYCLOAK_ADMIN` y `KEYCLOAK_ADMIN_PASSWORD` en `.env`.

### 3. Instalar dependencias y arrancar el backend

Desde la raíz, crea un entorno virtual e instala las dependencias:

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r backend\requirements.txt
```

Aplica las migraciones y arranca el servidor:

```powershell
Set-Location backend
..\.venv\Scripts\python.exe manage.py migrate
..\.venv\Scripts\python.exe manage.py runserver
```

El backend queda disponible en `http://localhost:8000/`. Si ya activaste el
entorno virtual desde la raíz, puedes usar `python manage.py migrate` y
`python manage.py runserver` desde `backend`.

### 4. Instalar dependencias y arrancar el frontend

En otra terminal, desde la raíz del repositorio:

```powershell
Set-Location frontend-app
npm ci
npm start
```

Abre `http://localhost:4200/`. La configuración de desarrollo del frontend
apunta a la API en `http://localhost:8000/api` y a Keycloak en
`http://localhost:8080`.

## Keycloak para desarrollo

### Usuarios demo

Con PostgreSQL y Keycloak levantados, y el realm `piedrazul` y sus roles ya
creados, ejecuta desde la raíz:

```powershell
.\scripts\provision-demo-users.ps1
```

El script crea o actualiza usuarios de prueba, asigna sus roles y sincroniza
sus identificadores con PostgreSQL. La contraseña predeterminada de estas
cuentas es `Piedrazul123!`; es solo para desarrollo. Las cuentas
`medico.carlos` y `paciente.ana` se vinculan a sus personas. El script permite
indicar otra contraseña con el parámetro `-Password`.

Los nombres de usuario de prueba son `admin.prueba`, `agendador.prueba`,
`medico.prueba`, `medico.carlos`, `paciente.prueba` y `paciente.ana`.

### Tema de inicio de sesión

El tema de Piedrazul está en `docker/keycloak/themes`. Después de iniciar los
servicios, recrea Keycloak para montar los archivos del tema:

```powershell
docker compose up -d --force-recreate keycloak
.\scripts\configure-keycloak-theme.ps1
```

El segundo comando activa el tema en el realm `piedrazul`; requiere que el realm
exista y que las credenciales de administración de `.env` sean válidas.

### Registro público de pacientes

La ruta **Regístrate** permite crear una cuenta de paciente. Para completar el
registro, el backend necesita conectividad con PostgreSQL y Keycloak y
credenciales administrativas configuradas localmente. En producción, usa una
cuenta de servicio con los permisos mínimos necesarios y protege sus
credenciales.

## Rutas principales del frontend

Las rutas protegidas también comprueban roles antes de habilitar sus vistas:

| Ruta | Acceso |
| --- | --- |
| `/login` | Inicio de sesión |
| `/registro` | Registro de pacientes |
| `/dashboard` | Usuarios autenticados |
| `/agenda` | `ADMIN`, `AGENDADOR`, `MEDICO` |
| `/agendar-cita` | `PACIENTE`, `AGENDADOR` |
| `/mis-citas` | `PACIENTE` |
| `/medicos` | `ADMIN` |
| `/disponibilidad` | `ADMIN`, `MEDICO` |
| `/configuracion` | `ADMIN` |

## API principal

La API tiene como prefijo local `http://localhost:8000/api/`. Las operaciones
protegidas requieren un token Bearer de Keycloak.

| Recurso | Rutas principales |
| --- | --- |
| Sesión y perfil | `GET /me/`, `GET /me/profile/`, `POST /registro/` |
| Personas y pacientes | `/personas/`, `/pacientes/` |
| Médicos y especialidades | `/medicos/`, `/especialidades/` |
| Disponibilidades | `/disponibilidades/`, `/medico-disponibilidades/` |
| Citas | `/citas/`, `/citas/{id}/` |
| Disponibilidad para agendar | `/citas/disponibles/fechas/`, `/citas/disponibles/` |
| Agenda e historial | `/citas/agenda/`, `/citas/agenda/paciente/`, `/citas/medico/agenda/` |
| Exportaciones | `/citas/agenda/exportar/`, `/citas/medico/agenda/exportar/` |
| Configuración | `/configuracion/` |

Las vistas de agenda, fechas disponibles, historial y exportación aceptan
parámetros de consulta según el filtro correspondiente; valida parámetros y
permisos en la implementación antes de integrar un cliente nuevo.

## Pruebas y compilación

### Backend

Desde `backend`, con el entorno virtual activo y PostgreSQL configurado:

```powershell
python manage.py check
python manage.py test
```

### Frontend

Desde `frontend-app`:

```powershell
npm test -- --watch=false
npm run build
```

## Solución de problemas

- **`fe_sendauth: no password supplied`:** comprueba que los archivos `.env`
  existan, que `DB_PASSWORD` tenga valor y que coincida con la contraseña
  configurada para PostgreSQL.
- **Cambiar el `.env` no corrige la contraseña existente:** PostgreSQL conserva
  las credenciales iniciales en su volumen de datos. Actualiza la contraseña
  del usuario de PostgreSQL para que coincida con la configuración; cambiar o
  borrar el volumen puede eliminar datos y no es una operación de reparación
  rutinaria.
- **`You must set settings.ALLOWED_HOSTS if DEBUG is False`:** configura
  `DJANGO_ALLOWED_HOSTS` en `backend\.env`. Para desarrollo local, el valor de
  ejemplo incluye `localhost`, `127.0.0.1` y `[::1]`.
- **Keycloak no autentica o devuelve error de realm:** verifica que el realm
  `piedrazul`, el cliente `piedrazul-frontend`, sus URI permitidos y los roles
  requeridos existan y coincidan con la configuración de ambos proyectos.
- **El puerto ya está en uso:** revisa los puertos configurados en `.env` y que
  no haya otro servicio usando PostgreSQL (`5432`), Keycloak (`8080`),
  Django (`8000`) o Angular (`4200`).

## Notas de seguridad

- No publiques `.env`, contraseñas, tokens ni claves privadas.
- Los valores de ejemplo y las cuentas demo son solo para desarrollo.
- No despliegues con `DJANGO_DEBUG=True` ni con la clave secreta de ejemplo.
- Configura `DJANGO_SECRET_KEY`, `DJANGO_ALLOWED_HOSTS`, las credenciales de la
  base de datos y la cuenta de servicio de Keycloak según el entorno.
