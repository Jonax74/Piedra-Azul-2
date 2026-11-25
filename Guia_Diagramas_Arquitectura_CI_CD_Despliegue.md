# Instructivo para actualizar los diagramas de arquitectura de Piedrazul

**Dirigido a:** integrante responsable de diagramas y documentación arquitectónica.  
**Objetivo:** actualizar los diagramas de los cortes anteriores para que representen el estado final previsto del tercer corte: RF-6, RF-7, pruebas, CI/CD y despliegue académico en la nube.  
**Alcance:** producir diagramas y sus leyendas; no implementar funcionalidades ni inventar decisiones distintas a las acordadas aquí.

## 1. Fuente de verdad y convenciones

Lee primero el [entregable del tercer corte](./EntregablesTercerCorte2026.md), la [guía del entregable y sus decisiones](./Guia_Entregable_Tercer_Corte_2026.md) y los diagramas C4/4+1 ya elaborados para el segundo corte. **Actualiza esos diagramas; no crees una arquitectura alternativa que contradiga los anteriores.**

Usa el código actual como evidencia de lo existente:

- Backend modular Django: `appointments`, `persons`, `users`, `notifications`, `common` y `config`. Ver [rutas de API](./backend/config/urls.py).
- Frontend Angular con páginas, servicios, autenticación Keycloak y guards: [rutas](./frontend-app/src/app/app.routes.ts).
- Dominio actual: [citas y estados](./backend/appointments/models.py), [transiciones](./backend/appointments/states.py), [disponibilidad y profesionales](./backend/persons/models.py).
- Servicios locales en [Compose](./docker-compose.yml). Es **solo desarrollo**: usa Angular `ng serve`, Django `runserver`, Keycloak `start-dev` y PostgreSQL local.
- Las URLs del navegador actuales están en [environment.ts](./frontend-app/src/environments/environment.ts). En cloud deberán ser HTTPS y configurarse para el dominio final.

### Etiquetado visual

Cada artefacto debe decir si representa **estado actual**, **arquitectura objetivo del tercer corte** o ambos. El documento solicitado necesita el estado final; prepara principalmente diagramas objetivo y distingue claramente las adiciones:

- Marcar con color/borde/etiqueta `Nuevo — tercer corte`: cancelación con política, reporte mensual, CI/CD y servicios cloud.
- Marcar servicios y responsabilidades ya existentes como `Existente`.
- No representar como implementado lo que siga pendiente. Si los diagramas se entregan antes del código, titularlos `Arquitectura objetivo` y conservar la leyenda.
- Mantener nombres y roles consistentes con el modelo y Keycloak: `PACIENTE`, `AGENDADOR`, `MEDICO` y tipo de profesional `MEDICO`/`TERAPISTA`.
- Usar una leyenda para actores, contenedores, bases de datos, servicios externos, flujos HTTPS/OIDC/API, red privada y límites de confianza.

## 2. Decisiones funcionales que deben verse en los diagramas

Estas reglas ya están cerradas por el equipo:

### RF-6 — Cancelación

- Solo el paciente propietario o un `AGENDADOR` puede cancelar; la autorización se valida en Django, no solo en el botón Angular.
- Se cancela con **48 horas o más** de anticipación. Exactamente 48 horas se acepta. El cálculo usa `America/Bogota`.
- La justificación es obligatoria, no puede quedar en blanco y permite hasta **500 caracteres**.
- Al cancelar, el horario se libera y queda disponible.
- `CANCELADA`, `ATENDIDA` y `NO_ASISTIDA` son estados finales. Una cancelada no se reactiva ni se reagenda.
- Mostrar la justificación como dato protegido de la cita, no como dato de autenticación ni contenido de logs. El diseño recomienda un campo explícito de cancelación de hasta 500 caracteres; no asumir que la observación genérica existente representa el motivo.

### RF-7 — Reporte

- El reporte trabaja solo con el año actual o anteriores; año futuro es inválido.
- Se contabilizan todas las citas, agrupadas por mes, profesional y estado: `PROGRAMADA`, `CONFIRMADA`, `REAGENDADA`, `ATENDIDA`, `NO_ASISTIDA`, `CANCELADA`.
- Cada cita se cuenta una vez según su `fecha_hora` vigente. Una reagendada usa su fecha nueva; una cancelada conserva su fecha programada.
- Médico/terapista ve sus propias citas. `AGENDADOR` puede ver todos y elegir **Todos** o filtrar por médico/terapista.
- Se incluyen los doce meses. Meses sin registros muestran cero; si todo el filtro está vacío, el frontend presenta un mensaje entendible de que no hay datos para ese año/filtro.
- La API devuelve agregados; nunca necesita entregar la información personal de cada paciente para calcular o graficar.

## 3. Arquitectura objetivo que se debe plasmar

El proveedor cloud todavía no se ha seleccionado. Los diagramas deben ser **independientes de marca**, pero representar esta topología concreta; cambiar el nombre del proveedor al aprobar una opción gratuita.

| Contenedor/servicio | Responsabilidad | Ejecución objetivo | Acceso |
|---|---|---|---|
| Navegador | UI del paciente, médico/terapista, agendador y administrador. | Cliente web por HTTPS. | Internet. |
| Frontend | Aplicación Angular; autenticación en navegador, agenda, cancelación y reporte. | Imagen de producción Node que compila Angular y sirve el SSR existente; puerto interno configurable (`PORT`, actualmente predeterminado 4000). | HTTPS público detrás del ingress del proveedor. |
| API | API REST Django, reglas del dominio, JWT/RBAC, disponibilidad y agregaciones. | Imagen de producción Python con servidor WSGI (no `runserver`); puerto interno 8000. | HTTPS público para navegador; conexiones internas a Keycloak/PostgreSQL. |
| Keycloak | OIDC, login, tokens JWT, realm, roles y usuarios demo. | Contenedor en modo de producción (no `start-dev`), con hostname HTTPS público y base de datos persistente. | HTTPS público para el navegador; administración restringida. |
| PostgreSQL | Datos del producto y estado durable de identidad. | PostgreSQL persistente/administrado; bases lógicas `piedra_db` y `keycloak_db`, con credenciales distintas por aplicación cuando el proveedor lo permita. | Solo red privada; no publicar el puerto a Internet. |
| CI/CD | Pruebas, construcción de imágenes y despliegue. | GitHub Actions; registry incluido por la plataforma o GHCR gratuito si sus cuotas aplican. | Se comunica con Git y el proveedor mediante secretos protegidos. |
| Ingress/hosting | TLS, dominios y enrutamiento. | Servicio cloud gratuito aprobado; sin planes, add-ons ni herramientas de pago. | Expone solo frontend, API y login. |

### Flujos y límites de red

1. Los usuarios abren el frontend por `https://<dominio-app>`.
2. El navegador redirige al login de Keycloak por `https://<dominio-auth>` usando OIDC Authorization Code + PKCE. Keycloak devuelve el JWT al cliente público de Angular.
3. El navegador llama a `https://<dominio-api>/api/...` con `Authorization: Bearer <JWT>`.
4. Django valida firma, issuer y roles del token; consulta datos en `piedra_db`.
5. Django consulta claves JWKS y, para registro/administración que lo requiera, llama a Keycloak por la red interna. El `issuer` que valida debe seguir siendo la URL pública con HTTPS; la URL de acceso interno no la sustituye.
6. Keycloak guarda realm/usuarios/configuración en `keycloak_db`; Django no comparte sus credenciales.
7. El tráfico entre servicios y PostgreSQL no sale a Internet. TLS termina en el ingress o se usa en cada conexión según el proveedor.
8. No incluir secretos en el navegador, diagramas, imágenes, argumentos de build ni código versionado. En diagramas escribir solo nombres de variables secretas, nunca sus valores.

Los nombres reales de dominio, proveedor y registry son campos por completar después de validar la cuota gratuita. No dibujar `localhost`, puertos publicados de Compose ni el volumen local como topología cloud.

## 4. Instrucciones para actualizar C4

Usa la misma notación, orientación y nivel de detalle de los diagramas existentes. C4 y 4+1 son vistas complementarias; no los fusiones en una sola lámina ilegible.

### C1 — Contexto del sistema

Mostrar el sistema Piedrazul como una unidad y los actores:

- `PACIENTE`: registra/consulta citas y cancela una cita propia dentro del plazo, con motivo.
- `MEDICO`/`TERAPISTA`: consulta su reporte anual y solo sus datos.
- `AGENDADOR`: consulta agenda, cancela en nombre de la operación y ve reporte de todos los profesionales o filtra uno.
- `ADMIN`: conserva las funciones administrativas existentes.
- `Keycloak`: servicio OIDC externo al sistema funcional, aunque se despliegue junto a él.
- `GitHub Actions` / proveedor cloud: actores de operación y despliegue, no usuarios del sistema.

Conectar las acciones con etiquetas de protocolo (HTTPS/OIDC, API REST/JWT). No mostrar datos clínicos en el reporte; este solo devuelve conteos.

### C2 — Contenedores

Dibujar navegador, Angular SSR, API Django REST, Keycloak y PostgreSQL, con dirección y protocolo de cada conexión según la sección 3. Añadir a PostgreSQL sus dos bases lógicas y el límite de red privada. Mostrar que Angular habla con API/Keycloak desde el navegador y que API valida token y consulta DB. No dibujar Angular conectándose directamente a PostgreSQL.

Añadir una nota: Angular frontend, API y Keycloak se exponen mediante HTTPS; PostgreSQL permanece privado. Los secretos se inyectan desde configuración segura del proveedor.

### C3 — Componentes (enfocar en los requisitos nuevos)

No es necesario repetir todas las clases. Preparar dos ampliaciones del contenedor API y sus consumidores del frontend:

**Flujo RF-6**

- Frontend: `Mis citas` + componente de acciones; nuevo campo de justificación y validación de 500 caracteres.
- Backend: autenticación/permiso por rol y propiedad; política de cancelación de 48 horas; validación de estado; persistencia del motivo; actualización de estado/transacción.
- Persistencia: `Cita` con fecha, estado, paciente, profesional y justificación; índice/constraint existente para no ocupar una franja cuando está cancelada.
- Disponibilidad: consulta de franjas excluye estados no ocupados; tras cancelar, la franja vuelve a ofrecerse.

**Flujo RF-7**

- Frontend: página de reportes, selector de año, filtro de profesional visible solo para `AGENDADOR`, gráfica mensual y tabla de resumen por estado.
- API Django: permiso de reporte, filtro de alcance por rol y año, consulta agregada por `mes + medico + estado`; no serializar pacientes.
- Persistencia: `Cita.fecha_hora`, `Cita.estado` y relación con `Medico`; devolver conteos sintéticos/agrupados.
- Respuesta vacía: doce meses con cantidades cero; mensaje claro si no hay ninguna cita para el año/filtro.

Los componentes `CancellationPolicy`, `AppointmentReportQuery` o equivalentes pueden dibujarse como responsabilidades lógicas futuras; etiquetarlos como propuestos, no como clases que ya existen.

### C4 — Código (solo si aporta valor)

El nivel de código es opcional; reutilizar el diagrama de clases existente si ya explica el dominio. Si se amplía:

- Modelos: `Cita`, `Paciente`, `Medico`, `Usuario`.
- Estados: `PROGRAMADA`, `CONFIRMADA`, `REAGENDADA` como activos; `ATENDIDA`, `NO_ASISTIDA`, `CANCELADA` como terminales.
- Indicar que RF-6 agrega motivo y regla temporal a la transición de cancelación, y RF-7 consulta/agrega por mes, médico y estado.
- No crear dependencias circulares entre vistas, modelos y frontend. No convertir la respuesta agregada en una dependencia del módulo de interfaz con modelos Django.

## 5. Diagramas 4+1 que se deben actualizar

El entregable académico pide modelo 4+1. Incluir título, propósito, leyenda y trazabilidad a escenarios. Mantener las vistas compactas y referenciar C4 donde ya se describan contenedores.

| Vista | Qué debe mostrar para el tercer corte |
|---|---|
| Lógica | Entidades y relaciones existentes; estados de `Cita`; motivo de cancelación; restricción por rol/propietario; consulta de reporte agrupada por fecha/profesional/estado. |
| Proceso | Secuencias RF-6 y RF-7: validación 48h y rol, actualización atómica/liberación de franja; emisión de JWT, solicitud del reporte, autorización y agregación. Incluir un escenario denegado. |
| Desarrollo | Módulos del repositorio: Angular (`pages/mis-citas`, nueva página de reportes, servicios/guards); Django (`appointments`, permisos/autenticación de `config`, nuevas operaciones/reportes); Keycloak/infra. Dibujar dependencias por módulo, no todos los archivos. |
| Física | Nodos cloud, ingress HTTPS, contenedores Angular/API/Keycloak, PostgreSQL privado persistente, automatización/registry y líneas de red. Debe coincidir con el diagrama de despliegue. |
| Escenarios (+1) | Casos que justifican las decisiones: paciente cancela a 48h; cancela antes del límite y es rechazado; agendador cancela; estado terminal no se reactiva; médico ve solo sus series; agendador ve todos/filtra; año futuro se rechaza; filtro sin datos presenta mensaje claro. |

## 6. Diagrama de despliegue cloud

Este diagrama debe ser independiente del C2 de contenedores: C2 dice qué unidades existen; despliegue dice en qué nodos viven, cómo se exponen y cómo se conectan.

Incluir:

- Borde de Internet, DNS/URLs públicas de app, API y Keycloak, certificado TLS/ingress.
- Red/proyecto cloud gratuito aprobado y límites de confianza.
- Contenedores y puertos internos: Angular SSR `PORT` (4000 actual), Django WSGI `8000`, Keycloak `8080`, PostgreSQL `5432` solo interno.
- PostgreSQL persistente con `piedra_db` y `keycloak_db`; respaldo/restore como flujo operativo.
- Variables secretas (solo nombres), configuración pública del cliente y origen CORS.
- Volumen/datos persistentes donde correspondan. No mostrar el volumen Docker local `postgres_data` como volumen cloud.
- CI/CD como plano de gestión: build/registry/deploy con tag de commit; no mezclar tráfico de runtime con tráfico de despliegue.
- Declarar que el entorno es académico, usa información sintética y no ofrece SLA de producción clínica.

**Comprobación de coherencia:** si una flecha atraviesa el límite público, etiquetarla HTTPS/OIDC/API. Ninguna flecha de navegador debe terminar en PostgreSQL. Administración de Keycloak debe quedar restringida, no abierta al público sin protección.

## 7. Diagrama CI/CD

Usar GitHub Actions como flujo de referencia y no incluir servicios pagos. Representar un ambiente cloud académico único para evitar mantener staging/prod duplicados en el plan gratuito; las pruebas de integración pueden usar servicios efímeros aislados en CI.

### Flujo obligatorio

1. **Pull request:** checkout; dependencias lockeadas; pruebas backend; pruebas frontend; chequeo de migraciones; validación Compose; build de Dockerfiles. En el futuro, sumar pruebas de integración/E2E reproducibles con datos sintéticos.
2. **Gate:** no permitir merge/release si falla cualquiera de esos jobs.
3. **Merge a `main`:** crear tag con commit SHA y construir artefactos/imágenes reproducibles. Publicar en registry gratuito/incluido o pedir al proveedor que construya desde el commit.
4. **Deploy al ambiente académico:** cargar secrets desde configuración protegida; preparar DB; ejecutar `manage.py migrate --noinput` una vez por release como job de despliegue, no desde cada réplica del backend.
5. **Verificación:** health check de API/servicios y smoke test de login, cancelación y reporte con usuarios/datos ficticios.
6. **Promoción:** el despliegue exitoso identifica commit e imagen. Si el health check o smoke test falla, restaurar la imagen anterior y seguir el procedimiento de migraciones compatibles.
7. **ZAP:** correr un baseline pasivo/seguro sobre el dominio de demo después de desplegar; guardar resumen sin tokens, cookies ni información personal. Las pruebas activas requieren autorización y alcance explícitos.

### Secretos y costos que deben aparecer en la vista

- Dibujar secretos como almacén del proveedor/GitHub Actions; flecha hacia el deploy job y servicios, nunca al repositorio público ni al navegador.
- Mostrar `DJANGO_SECRET_KEY`, credenciales DB/Keycloak y token de deploy solo como nombres/agrupaciones, sin valores.
- Indicar que se usan cuotas gratuitas y que no se activan add-ons/planes de pago. Mostrar como pendiente el nombre del proveedor hasta validar sus límites oficiales.
- Si GitHub Actions o el registry exceden la cuota gratuita, el job debe fallar/notificar; no hacer upgrade automático.

## 8. Secuencias de comportamiento necesarias

Crear diagramas de secuencia separados y legibles; numerar mensajes y enlazarlos a la historia/prueba.

### RF-6 — Cancelación válida e inválida

**Válida:** paciente/AGENDADOR → Angular → API (JWT + estado + motivo) → política (48h) → DB (transacción, estado CANCELADA) → API → Angular; la franja queda libre. Mostrar que paciente solo opera su propia cita y agendador puede operar cualquiera según rol.

**Inválida:** mismo flujo con <48h, motivo vacío/largo, paciente de otra cita o estado terminal; API devuelve error controlado, la DB no cambia y la franja continúa según la cita original.

### RF-7 — Consulta del reporte

**Médico:** login Keycloak → JWT → filtro de año → API deriva el médico desde el usuario autenticado → DB agrega estados/meses de ese médico → respuesta solo con conteos → gráfica/tabla.

**Agendador:** login → elige año y Todos/profesional → API valida el rol y filtro opcional → DB agrega todos/seleccionado → respuesta agregada → UI actualiza resumen y gráfica.

**Sin datos/año futuro:** filtro válido sin resultados devuelve estructura vacía/ceros y mensaje funcional; año futuro se rechaza sin consulta abierta ni error 500.

## 9. Paquete de archivos y revisión

Guardar fuentes editables de los diagramas y sus exportaciones en una carpeta versionada; por ejemplo:

```text
docs/architecture/third-cut/
  c4-context.(puml|mmd)
  c4-containers.(puml|mmd)
  c4-components-appointments.(puml|mmd)
  deployment-cloud.(puml|mmd)
  4plus1-logical.(puml|mmd)
  4plus1-process-cancellation.(puml|mmd)
  4plus1-process-report.(puml|mmd)
  4plus1-development.(puml|mmd)
  4plus1-physical.(puml|mmd)
  4plus1-scenarios.(puml|mmd)
  cicd.(puml|mmd)
  exports/
```

La extensión depende de la herramienta acordada por el equipo; no se deben mantener diagramas y exportaciones que diverjan. Incluir exportaciones SVG/PNG legibles en el documento académico y enlazar el fuente editable. Usar placeholders hasta que el proveedor cloud y sus URLs sean reales; reemplazarlos antes de entregar.

Antes de cerrar, pedir a un integrante backend, uno frontend y uno de operaciones que confirmen respectivamente reglas/contratos, pantallas y flujos, y hosting/pipeline. Revisar esta lista:

- [ ] Se conservaron y actualizaron los diagramas de los dos cortes anteriores.
- [ ] RF-6 muestra 48h inclusivas, paciente propietario/AGENDADOR, motivo máximo 500, estados terminales y liberación de franja.
- [ ] RF-7 muestra los seis estados, año actual/anterior, restricción médico/agendador, filtro y estado vacío.
- [ ] C4, 4+1, despliegue y CI/CD coinciden en nombres, puertos, datos y conexiones.
- [ ] PostgreSQL solo tiene conexiones privadas y Keycloak conserva persistencia.
- [ ] Se distinguen sistema actual y arquitectura objetivo, sin presentar trabajo pendiente como terminado.
- [ ] El proveedor cloud está etiquetado como aprobado solo después de comprobar que se usan únicamente servicios/planes gratuitos.
- [ ] No se muestran secretos, tokens ni datos reales; la nube académica usa información sintética.
- [ ] Las imágenes exportadas se leen al tamaño del documento y tienen leyendas/fuentes.

