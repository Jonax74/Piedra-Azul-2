## Inicio rápido para desarrollo

El proyecto se ejecuta con Docker Compose para que todos los integrantes usen
las mismas versiones de PostgreSQL, Keycloak, Django y Angular. No es necesario
instalar Python, Node.js ni las dependencias del proyecto en el equipo anfitrión.

### Requisitos

- Git.
- Docker Desktop con Docker Compose v2 (en Windows, usar contenedores Linux).
- PowerShell para los scripts de apoyo incluidos en `scripts/`.

### Servicios del entorno local

`docker-compose.yml` levanta la aplicación completa. No ejecutes Django ni
Angular directamente en el equipo anfitrión para el flujo normal de desarrollo:

| Servicio | URL o puerto local | Uso |
| --- | --- | --- |
| `frontend` | <http://localhost:4200> | Aplicación Angular; los cambios del código se recargan automáticamente. |
| `backend` | <http://localhost:8000/api/> | API Django; aplica migraciones al iniciar. |
| `keycloak` | <http://localhost:8080> | Inicio de sesión, realm `piedrazul` y usuarios. |
| `postgres` | `localhost:5432` | Base `piedra_db`; los datos se guardan en un volumen Docker. |

Las URLs son las predeterminadas. La aplicación que corre en el navegador usa
`localhost`; entre contenedores se usan los nombres DNS de Compose (`backend`,
`frontend`, `keycloak` y `postgres`).

### Preparar y levantar el proyecto

Desde la raíz del repositorio, crea `.env` a partir de la plantilla **solo si
aún no existe**. Luego construye e inicia los contenedores:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
docker compose config --quiet
docker compose up --build -d
docker compose ps
```

En macOS/Linux, crea `.env` con:

```sh
[ -f .env ] || cp .env.example .env
```

`docker compose config --quiet` valida la configuración sin imprimir sus
valores. Si informa variables sin definir, confirma que `.env` esté en la raíz
y tenga los valores de `.env.example`.

La primera ejecución puede tardar mientras Docker descarga imágenes y construye
las dependencias. Espera a que `backend` figure como `healthy` y `frontend`
esté iniciado en `docker compose ps`. Django aplica las migraciones
automáticamente al iniciar. Keycloak importa el realm, el cliente público y los
roles iniciales. Después, abre:

- Aplicación Angular: <http://localhost:4200>
- API Django: <http://localhost:8000/api/>
- Keycloak: <http://localhost:8080>
- PostgreSQL: `localhost:5432` (base de datos `piedra_db`)

La base PostgreSQL conserva los datos en el volumen `postgres_data` entre
reinicios. El script `docker/postgres/init-databases.sql` se ejecuta únicamente
cuando PostgreSQL inicializa un volumen vacío; cambiarlo no recrea bases de
datos en un volumen ya existente.

Para activar el tema Piedrazul en el realm y crear los usuarios de prueba,
ejecuta una vez cada script desde la raíz:

```powershell
.\scripts\configure-keycloak-theme.ps1
.\scripts\provision-demo-users.ps1
```

Las cuentas de prueba usan la contraseña `Piedrazul123!`. Son únicamente para
desarrollo local; no reutilices estas credenciales en otros entornos.

### Trabajo diario

- `docker compose up -d` inicia los servicios; `docker compose down` los detiene
  sin borrar los datos de PostgreSQL.
- Los cambios de código en `backend/` y `frontend-app/` se reflejan desde los
  volúmenes montados. Angular observa cambios también en Windows.
- Después de modificar `backend/requirements.txt`, reconstruye el backend con
  `docker compose up --build -d backend`.
- Después de modificar `frontend-app/package.json` o `package-lock.json`,
  actualiza las dependencias del volumen con
  `docker compose run --rm --no-deps frontend npm ci` y reinicia Angular con
  `docker compose restart frontend`.
- Django aplica las migraciones pendientes al iniciar. Para generarlas después
  de cambiar modelos, ejecuta
  `docker compose exec backend python manage.py makemigrations` y conserva los
  archivos de migración en el control de versiones.
- Para revisar registros: `docker compose logs -f backend frontend keycloak`.
- Para detener y **borrar permanentemente** los datos locales de la base de
  datos: `docker compose down -v`. No uses `-v` como parte del cierre habitual.

### Configuración y colaboración

- `.env` contiene configuración local y no se debe subir al repositorio.
  Comparte cambios de configuración actualizando `.env.example`, nunca
  publicando contraseñas o claves reales.
- Para iniciar Compose, la configuración que se lee es el `.env` de la raíz.
  Ahí se definen `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`,
  `KEYCLOAK_DB`, `KEYCLOAK_ADMIN`, `KEYCLOAK_ADMIN_PASSWORD`,
  `POSTGRES_PORT`, `KEYCLOAK_PORT`, `DJANGO_DEBUG`, `DJANGO_SECRET_KEY` y
  `DJANGO_ALLOWED_HOSTS`. La plantilla `.env.example` contiene valores locales
  de ejemplo; no son credenciales seguras ni deben usarse en producción.
- `POSTGRES_PORT` y `KEYCLOAK_PORT` cambian los puertos publicados en el equipo.
  Los puertos de Django (`8000`) y Angular (`4200`) están definidos en
  `docker-compose.yml`; si se cambian, también hay que revisar las URLs que usa
  el frontend y el cliente de Keycloak.
- Los valores predeterminados de `.env.example` son solo para desarrollo local.
  Cambia las credenciales de prueba si expones los servicios en una red; no uses
  esta configuración para producción.
- Los puertos predeterminados son `4200` (Angular), `8000` (Django), `8080`
  (Keycloak) y `5432` (PostgreSQL). Si uno está ocupado, detén el servicio local
  que lo usa antes de iniciar Compose. Cambiar los puertos publicados de Angular,
  Django o Keycloak también requiere actualizar las URLs de desarrollo del
  frontend y los orígenes permitidos del cliente/servidor.
- Los nombres de servicio de Compose (`backend`, `frontend`, `postgres` y
  `keycloak`) son los nombres DNS dentro de la red Docker. El navegador usa
  `localhost`; no cambies las URLs internas por `localhost` en la configuración
  del backend.
- Este Compose está preparado para desarrollo: usa `start-dev` de Keycloak,
  `runserver` de Django y el servidor de desarrollo de Angular. No lo expongas
  como configuración de producción.
- Si se agregan servicios, dependencias o variables necesarias para desarrollar,
  documenta y versiona sus archivos de configuración de ejemplo para que todos
  puedan reproducir el entorno.

## Usuarios demo

Con PostgreSQL y Keycloak levantados, ejecuta desde la raiz del proyecto:

```powershell
.\scripts\provision-demo-users.ps1
```

El script crea o actualiza las cuentas del realm `piedrazul`, asigna sus roles,
sincroniza sus IDs con PostgreSQL y deja la contrasena de prueba en
`Piedrazul123!`.

Las cuentas `medico.carlos` y `paciente.ana` quedan vinculadas a sus personas
correspondientes. La vinculacion tambien puede hacerse al crear o editar un
paciente o medico usando el campo opcional `usuario_id` en la API.

## Tema de inicio de sesion de Keycloak

Keycloak incluye el tema visual de Piedrazul desde `docker/keycloak/themes`.
El tema se monta al iniciar el contenedor. Actívalo en el realm `piedrazul`
(este script no modifica usuarios ni contraseñas):

```powershell
.\scripts\configure-keycloak-theme.ps1
```

El tema mantiene la autenticacion en Keycloak, aplica el logo y la paleta de
colores de la aplicacion y muestra sus textos en espanol. La pantalla de acceso
usa un diseno adaptable con el formulario y la marca Piedrazul en paneles
separados, controles mas claros y mensajes de seguridad.

## Registro de pacientes

La vista **Regístrate** está disponible desde la pantalla de inicio de sesión.
Al completar el formulario, el backend crea la cuenta en Keycloak con el rol
`PACIENTE` y registra en la base de datos su usuario, persona y perfil de
paciente. El usuario puede iniciar sesión inmediatamente con el nombre de
usuario y contraseña elegidos.

Las migraciones se aplican automáticamente cuando inicia el contenedor backend.
También se pueden aplicar manualmente desde la raíz del proyecto:

```powershell
docker compose exec backend python manage.py migrate
```

Para el entorno local, el backend usa `KEYCLOAK_ADMIN` y
`KEYCLOAK_ADMIN_PASSWORD` del `.env` para aprovisionar la cuenta mediante la
API administrativa de Keycloak; esas credenciales nunca se envían al navegador.
En una instalación de producción, configura credenciales de servicio con los
permisos mínimos necesarios para administrar usuarios y asignar el rol
`PACIENTE`.

Los usuarios con rol `MEDICO` pueden consultar **Mi agenda** desde la pestaña
Agenda. La vista muestra exclusivamente citas del perfil médico vinculado y
permite filtrarlas por fecha.

Los usuarios con rol `AGENDADOR` pueden consultar la agenda por profesional y
fecha. El calendario solo habilita fechas con citas para el profesional
seleccionado, o fechas con citas de cualquier profesional cuando no hay uno
seleccionado. Al elegir ambos filtros aparece **Exportar CSV**, que descarga el
listado de pacientes con fecha, hora, nombre, documento, teléfono y estado; las
citas canceladas se excluyen del calendario y del archivo.