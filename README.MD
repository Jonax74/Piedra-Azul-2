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

## Pruebas automatizadas

El proyecto tiene pruebas unitarias para backend y frontend. En la ultima
verificacion se ejecutaron correctamente 25 pruebas Django y 41 pruebas
Angular distribuidas en 22 archivos.

### Que cubren

- **Citas y agenda (backend):** alcance de citas por paciente, permisos de
	configuracion, generacion de franjas, dias festivos, horarios ocupados,
	intervalos medicos y limites de la ventana de agendamiento. Las pruebas
	estan en `backend/appointments/tests.py`.
- **Personas y usuarios (backend):** vinculacion de usuarios con pacientes o
	medicos, validacion de roles y sincronizacion de roles recibidos desde
	Keycloak. Las pruebas estan en `backend/persons/tests.py` y
	`backend/users/tests.py`.
- **Autenticacion (backend):** manejo del encabezado Bearer, configuracion del
	emisor, validacion del JWT y adaptacion de sus claims al usuario de DRF. Las
	pruebas estan en `backend/config/security/tests.py`.
- **Servicios frontend:** rutas, metodos HTTP, parametros, payloads y cache de
	las peticiones para citas, personas, medicos, configuracion y disponibilidad.
- **Acceso frontend:** inicializacion de autenticacion, permisos por rol y
	envio del token Bearer en solicitudes API.
- **Flujos de interfaz:** calendario y reserva de citas, configuracion de
	semanas, validacion de cruces de disponibilidad, filtros y paginacion de
	agenda, lista personal de citas, dashboard y busqueda de medicos.

Las pruebas frontend reemplazan las dependencias HTTP y de autenticacion con
dobles de prueba; no necesitan levantar Django ni Keycloak. Las pruebas JWT del
backend simulan el cliente JWKS y la decodificacion del token, por lo que
tampoco requieren conectarse a Keycloak. Django utiliza una base temporal para
sus pruebas y necesita poder conectarse al PostgreSQL configurado en
`backend/.env`.

### Como ejecutarlas

Desde la raiz del repositorio, en PowerShell:

```powershell
Push-Location backend
.\venv\Scripts\python.exe manage.py test
Pop-Location
```

Para ejecutar las pruebas del frontend:

```powershell
Push-Location frontend-app
npm install
npm test -- --watch=false
Pop-Location
```

El script de frontend ejecuta Angular/Vitest con `--isolate=false`, configurado
para evitar cierres inesperados de workers en Windows. Si solo se quieren
ejecutar pruebas especificas, Angular acepta `--include`; por ejemplo:

```powershell
npx ng test --include=src/app/pages/agendar-cita/agendar-cita.spec.ts --watch=false
```

`common`, `notifications` y la pagina `Citas` aun no tienen logica propia
significativa; por eso no se les atribuye cobertura de comportamiento adicional.

## Tema de inicio de sesion de Keycloak

Keycloak incluye el tema visual de Piedrazul desde `docker/keycloak/themes`.
Despues de iniciar los servicios, recrea el contenedor para montar el tema y
desactiva su cache de desarrollo:

```powershell
docker compose up -d --force-recreate keycloak
```

Activa el tema en el realm existente `piedrazul` (este script no modifica
usuarios ni contrasenas):

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

Después de actualizar el proyecto, aplica las migraciones desde `backend`:

```powershell
cd backend
python manage.py migrate
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
