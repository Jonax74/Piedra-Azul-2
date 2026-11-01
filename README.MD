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

## Verificación de seguridad web con OWASP ZAP

El 1 de noviembre de 2026 se realizó una verificación de seguridad en un entorno
local de pruebas, combinando pruebas manuales y un análisis pasivo con OWASP ZAP
2.17.0. El reporte ZAP se generó a las 00:59:48. Los resultados son evidencia
del corte y no constituyen una certificación de ausencia de vulnerabilidades.

### Alcance y método

- El escenario manual utilizó una base de datos de respaldo, un realm de
  Keycloak distinto al de desarrollo y usuarios y datos de prueba.
- Los servicios locales incluidos en el reporte fueron la interfaz
  (`localhost:4200`), la API (`localhost:8000`) y Keycloak
  (`localhost:8080`, realm de pruebas `piedrazul-zap`).
- Se documentaron pruebas manuales relacionadas con A01:2025 (Broken Access
  Control) y A02:2025 (Security Misconfiguration) del OWASP Top 10 2025.
- Las alertas del HTML corresponden al análisis pasivo de ZAP. Por su
  naturaleza, requieren revisión contextual y validación manual antes de
  concluir que existe una vulnerabilidad explotable.
- El reporte también enumera dominios externos utilizados por recursos o
  servicios de terceros. No deben atribuirse a la aplicación sin verificar
  propiedad y autorización; se recomienda limitar el alcance de futuros
  escaneos a los activos aprobados.

### Resultados del corte

El resumen de ZAP presenta 25 tipos o grupos de alerta: 1 de riesgo alto, 8
medios, 8 bajos y 8 informativos. Son categorías agrupadas y no equivalen a 25
ocurrencias individuales; algunas alertas aparecen en múltiples rutas o
respuestas.

**Prueba manual A01:2025 — control de acceso.** Se comprobó que paciente1 no
pudo acceder a la cita identificada como perteneciente a paciente2 al solicitar
su identificador. No se observó acceso horizontal no autorizado en ese caso
concreto. El resultado se limita al usuario, recurso y flujo probados; no
demuestra cobertura de todas las rutas, operaciones o roles.

**Hallazgos A02:2025 registrados como confirmados en la evidencia del corte:**

- **H1 — Política CSP ausente (riesgo medio):** observada en la pantalla de
  inicio de sesión de la interfaz. La evidencia base indica que el entorno
  local tenía `DEBUG=true`, por lo que debe repetirse la comprobación en una
  configuración representativa de producción.
- **H2 — Alerta de ausencia de token anti-CSRF (riesgo medio):** observada en
  el flujo de autenticación de Keycloak. Debe revisarse con respecto a los
  mecanismos de protección propios de Keycloak y validarse en el flujo
  completo; no basta con basarse únicamente en la presencia o ausencia de un
  campo visible del formulario.

**Alerta de riesgo alto pendiente de validación:** ZAP reportó una posible
redirección fuera del sitio (CWE-601), con confianza media, en el parámetro
`post_logout_redirect_uri` del flujo de cierre de sesión. El destino observado
en el reporte corresponde a una ruta local de inicio de sesión. Se debe probar
si es posible controlar el destino hacia un dominio externo no autorizado antes
de clasificarla como vulnerabilidad confirmada. Los URI de retorno deben
restringirse a destinos explícitamente permitidos para cada cliente y entorno.

Entre las alertas medias restantes se encuentran observaciones sobre políticas
CSP permisivas o incompletas, configuración cross-domain y protección contra
clickjacking. Las alertas bajas incluyen atributos de cookies (`HttpOnly` y
`SameSite`), cabeceras HTTP de seguridad y divulgación de información de
servidor. Las informativas incluyen observaciones sobre `localStorage`,
comentarios, información en URL y controles de caché o sesión. Su severidad y
relevancia deben determinarse según el componente, los datos implicados y la
configuración de despliegue.

### Seguimiento recomendado

1. Validar manualmente la redirección posterior al cierre de sesión con destinos
   permitidos y no permitidos, y corregir cualquier aceptación de destinos
   externos no registrados.
2. Revisar la protección CSRF de operaciones que modifican estado y del flujo
   de autenticación de Keycloak.
3. Definir una política CSP con directivas y orígenes mínimos necesarios;
   desplegarla gradualmente y reducir comodines y `unsafe-inline` cuando sea
   viable.
4. Verificar la política cross-domain y la protección anti-clickjacking,
   permitiendo únicamente los orígenes e integraciones requeridos.
5. Revisar cookies y cabeceras de seguridad en HTTPS, considerando las
   necesidades funcionales de cada cookie y los dominios realmente controlados.
6. Repetir las pruebas de autorización con más recursos, operaciones y roles,
   y volver a ejecutar ZAP con alcance autorizado y documentado.
7. Revisar los registros de ZAP antes de usar el resultado como evidencia de
   cobertura: el reporte señala 10 advertencias, 1 error y un uso de memoria
   del 89 % durante la ejecución.

### Evidencia y manejo de datos

El [informe técnico editable del corte](zap-reports/Informe_Tecnico_Seguridad_Web_OWASP_ZAP_2026-11-01.docx)
incluye el resumen, resultados y una captura recortada del árbol de alertas.
Los reportes originales y la captura completa se conservan como evidencia de
prueba; antes de compartirlos fuera del equipo, deben revisarse y redactarse
valores de autenticación, sesión, tokens y cualquier dato sensible. No se deben
copiar en documentación pública URLs completas de autenticación ni sus
parámetros.
