# Guía de trabajo: tercer corte de Piedrazul

**Propósito:** servir como guía ejecutable para completar los requisitos nuevos del tercer corte, preparar la evidencia y redactar el documento de arquitectura solicitado. No reemplaza el entregable académico ni afirma que funcionalidades aún pendientes ya estén implementadas.

**Fuentes revisadas**

- [Entregables del tercer corte](./EntregablesTercerCorte2026.md).
- [Entregables del segundo corte](./EntregablesSegundoCorte2026.2-IngSoft3.pdf).
- Implementación actual de citas: [modelo](./backend/appointments/models.py), [estados](./backend/appointments/states.py), [serializador](./backend/appointments/serializers.py), [vistas](./backend/appointments/views.py), [rutas](./backend/appointments/urls.py).
- Interfaz existente: [acciones de cita](./frontend-app/src/app/shared/components/appointment-actions/appointment-actions.ts), [pantalla Mis citas](./frontend-app/src/app/pages/mis-citas/mis-citas.ts) y [rutas del frontend](./frontend-app/src/app/app.routes.ts).
- Guía de pruebas existente: [guia_prueba_requisitos.md](./guia_prueba_requisitos.md).

## 1. Qué es nuevo en este corte

El Markdown del tercer corte explica que los requisitos nuevos se distinguían en azul, pero la conversión a `.md` no conserva ese color. La comparación con el PDF del segundo corte permite identificarlos:

| Tipo | Nuevos | Contexto |
|---|---|---|
| Funcional | **RF-6:** el paciente cancela una cita propia con al menos dos días de anticipación y registra el motivo. | RF-1 a RF-5 ya aparecen en el segundo corte. |
| Funcional | **RF-7:** médico/terapista consulta un gráfico de citas por mes y profesional para un año elegido. | No se encontró una vista, servicio o API de reporte gráfico en el código revisado. |
| No funcional | **RNF-3:** realizar pruebas de integración, sistema y/o aceptación para completar la pirámide. | El repositorio ya tiene pruebas backend y frontend, pero no se identificó un flujo E2E ni una configuración de CI. |
| No funcional | **RNF-4:** CI/CD con pruebas automáticas y despliegue continuo. | No se encontró directorio `.github` ni workflows de CI/CD. |
| No funcional | **RNF-5:** desplegar en la nube, procurando una tecnología gratuita y sin utilizar herramientas/servicios de pago. | Existe Compose de desarrollo; no equivale a una configuración de producción. |
| No funcional | **RNF-6:** monitoreo. | Es opcional según el enunciado; se propone como trabajo adicional si alcanza el tiempo. |

RNF-1 (Keycloak/JWT) y RNF-2 (ZAP) vienen del segundo corte. Se deben conservar y volver a validar, no contarlos como requisitos nuevos. Hay reportes locales en `zap-reports/`, pero el directorio está excluido por `.gitignore`; se deberá dejar evidencia breve, sanitizada y versionada para el entregable.

## 2. Estado inicial y brechas

| Requisito | Evidencia actual | Brecha que debe cerrarse |
|---|---|---|
| RF-6: cancelar cita | `Cita` ya contempla el estado `CANCELADA`; la máquina de estados permite transiciones a ese estado; el frontend muestra una acción de cancelar; pruebas existentes demuestran un PATCH de estado. | No se encontró una regla de servidor que impida cancelar fuera del plazo de dos días ni un campo específico obligatorio para la justificación. `observacion` existe, pero es una observación general y no prueba por sí sola el requisito. El frontend no pide el motivo. |
| RF-7: reporte gráfico | El modelo contiene fecha, médico y estado, datos suficientes para una agregación inicial. | No se encontró endpoint, política de acceso, filtro anual ni gráfica. |
| RNF-3: pirámide de pruebas | Hay pruebas Django y specs de frontend. | Definir y automatizar al menos un caso de integración y un recorrido de aceptación/sistema reproducible; verificar si el cliente espera las tres capas y no solo una. |
| RNF-4: CI/CD | Las dependencias y scripts de build/test existen. | Crear workflows; hacer que cada PR ejecute validaciones y que el despliegue dependa de ellas. |
| RNF-5: nube | `docker-compose.yml` permite levantar servicios locales. | Definir hosting elegible, runtime de producción, persistencia, URLs, secretos, TLS, migraciones, health checks y estrategia de rollback. Los comandos actuales (`ng serve`, Django `runserver`, Keycloak `start-dev`) son de desarrollo, no de producción. |
| RNF-6: monitoreo (opcional) | Compose ya tiene un healthcheck local para el backend. | Si se aborda, falta una comprobación de salud explícita para el servicio desplegado y un procedimiento de revisión de logs/alertas sin datos sensibles. |

**Conclusión de alcance:** cerrar RF-6 y RF-7, y los RNF-3, RNF-4 y RNF-5. Añadir RNF-6 solo después de asegurar las pruebas, el despliegue y la documentación obligatoria.

## 3. Decisiones acordadas

Estas reglas se consideran cerradas para el diseño y la planificación. Si el cliente/docente solicita un cambio, actualizar primero esta guía y los criterios de aceptación relacionados.

| Tema | Decisión |
|---|---|
| Plazo RF-6 | Se permite cancelar cuando falten **48 horas o más** para la cita. Exactamente 48 horas es válido. Comparar instantes con zona horaria `America/Bogota`. |
| Permisos RF-6 | Puede cancelar el paciente propietario de la cita o un usuario con rol `AGENDADOR`. El backend verifica los permisos; ocultar el botón no es un control de seguridad. |
| Estados RF-6 | `ATENDIDA`, `NO_ASISTIDA` y `CANCELADA` son estados finales. Una cita cancelada no se confirma ni se reagenda después. Cancelar libera su franja. |
| Justificación | Obligatoria, no vacía tras quitar espacios y con máximo **500 caracteres**. Guardar el motivo asociado a la cancelación; no asumir que la observación general es su campo definitivo. No escribirlo en logs. |
| Conteo RF-7 | Incluir las citas de todos los estados: `PROGRAMADA`, `CONFIRMADA`, `REAGENDADA`, `ATENDIDA`, `NO_ASISTIDA` y `CANCELADA`. Contar cada cita una sola vez en el mes de su `fecha_hora` vigente y mostrar el desglose por estado. |
| Acceso RF-7 | Médico/terapista ve únicamente sus propias citas. `AGENDADOR` ve todos los profesionales y puede filtrar por uno. API y frontend respetan el mismo alcance. |
| Año/mes RF-7 | Permitir el año actual y anteriores; rechazar años futuros tanto en interfaz como en API. Devolver los 12 meses. Meses sin citas muestran cero; si todo el filtro está vacío, presentar “No hay citas registradas para este año/filtro”, no un error ni un gráfico ambiguo. |
| Nube/datos | Despliegue académico bien configurado, pero con datos sintéticos, nunca datos reales de pacientes. |
| Costo cloud | No usar herramientas, servicios ni planes de pago para desplegar. Preferir opciones sin tarjeta y confirmar que no haya cobros automáticos; “sin tarjeta” no significa que se exija un proveedor específico. |
| Proveedor | Aún no se ha seleccionado uno. La arquitectura y los diagramas deben ser independientes del proveedor hasta validar qué plataforma gratuita permite los servicios requeridos. |

El [instructivo para quien actualiza los diagramas](./Guia_Diagramas_Arquitectura_CI_CD_Despliegue.md) contiene el destino arquitectónico que debe reflejarse.

## 4. Historias nuevas y aceptación propuesta

Los criterios siguientes ya incorporan las decisiones acordadas.

### RF-6 — Cancelar una cita

1. Dado un paciente autenticado con una cita futura propia a 48 horas o más, al elegir **Cancelar**, la interfaz solicita y valida una justificación de hasta 500 caracteres antes de enviar el cambio.
2. Un usuario con rol `AGENDADOR` también puede cancelar dentro del plazo y registrar el motivo. El servidor valida rol, estado, fecha/hora y justificación; no confía en el rol enviado por el cliente.
3. La cita actualizada se muestra como cancelada y deja de ocupar la franja. Una persona puede consultar esa franja como disponible.
4. Si faltan menos de 48 horas, el backend rechaza la cancelación aunque se invoque la API directamente. Exactamente 48 horas se acepta.
5. Se rechazan motivo ausente, compuesto solo de espacios o mayor a 500 caracteres; paciente que intenta cancelar una cita ajena; estado final; cita inexistente; y token/rol no autorizado.
6. Después de cancelar, no se puede volver a confirmar ni reagendar esa cita. Atendida y no asistida tampoco pueden volver a estados activos.
7. Una respuesta fallida no cambia el estado ni libera el espacio. El listado y la agenda siguen representando correctamente las citas canceladas.

**API a diseñar:** preservar el patrón de actualización de `PATCH /api/citas/{id}/` si el contrato actual lo permite. Definir explícitamente el campo de justificación; no asumir que `observacion` sirve sin aprobar su semántica. Respuestas esperadas: éxito con recurso actualizado; `400` por plazo/datos inválidos; `403` por falta de permisos; `404` para un recurso fuera del alcance, según la política de privacidad que se apruebe.

### RF-7 — Reportar citas por año y profesional

1. El usuario autorizado abre la pantalla de reporte y elige el año actual o uno anterior. El año futuro se deshabilita en la interfaz y también se rechaza en el backend.
2. El gráfico resume todas las citas por mes y profesional. El desglose identifica los seis estados persistidos: programada, confirmada, reagendada, atendida, no asistida y cancelada.
3. El médico/terapista ve solo sus citas. El agendador puede ver todos y elegir **Todos** o filtrar por un médico/terapista.
4. Los meses sin citas se representan con cero; si el año/filtro completo no tiene registros, se muestra “No hay citas registradas para este año/filtro”, no un error ni un gráfico vacío sin explicación.
5. El servidor calcula las cantidades agregadas en la base de datos y no manda todas las citas/personas al navegador.
6. Un usuario sin rol autorizado recibe una respuesta no autorizada; el backend impone el alcance de profesionales aprobado aunque el frontend manipule parámetros.
7. El gráfico tiene título, leyenda, etiquetas accesibles y una tabla textual con el total/desglose por estado.
8. El cambio de año/profesional actualiza los datos, carga y errores; un año futuro/inválido no ejecuta una consulta sin límites.

**Contrato de referencia:** endpoint de lectura `GET /api/reportes/citas/por-medico/?anio=2026`, con datos agregados por `anio`, `mes`, `medico_id`, `medico`, `estado` y `cantidad`. Para `AGENDADOR`, un parámetro opcional de médico filtra la respuesta; el backend deriva el médico permitido para el rol `MEDICO`, no del parámetro del cliente.

## 5. Plan de ejecución por fases

Crear en Jira/Trello una tarea por cada elemento y asignar una persona/agente responsable. Las tareas de frontend no deben inventar contratos mientras el backend siga sin definirlos.

### Fase 0 — Aclaración y preparación

- **T00 — Contratos RF-6/RF-7:** registrar las decisiones acordadas, requests/responses y códigos de error.
- **T01 — Contratos:** definir request/response, códigos de error, permisos, estados contabilizados, zona horaria y paginación si aplica.
- **T02 — Plan de pruebas y datos:** preparar usuarios por rol, citas sintéticas antes/en/después del límite, estados terminales, meses con/sin datos y más de un profesional.
- **Salida:** historias aprobadas, contratos escritos y tareas en el tablero del sprint.

### Fase 1 — RF-6, política de cancelación

- **T10 — Dominio/API:** implementar el límite inclusivo de 48 horas, cancelación por propietario o agendador, terminalidad del estado y motivo de hasta 500 caracteres. Si se agrega un campo al modelo, crear migración de Django.
- **T11 — Pruebas API:** probar límite exacto, un segundo antes, permisos paciente/agendador, cita ajena, motivo ausente/blanco/largo, estado final e inexistente, no reactivación y liberación de franja.
- **T12 — Interfaz:** agregar campo de motivo, confirmación, errores de plazo y accesibilidad; actualizar la cita solo con la respuesta exitosa del servidor.
- **T13 — Regresión:** verificar agenda, exportación, disponibilidad, listado de citas y que las citas canceladas no se presenten como ocupadas.
- **Salida:** criterios RF-6 satisfechos desde UI y API, suite verde y evidencia del antes/después.

### Fase 2 — RF-7, reporte

- **T20 — Agregación/API:** consulta de año actual o anterior; agrupar por mes/profesional/estado, incluir ceros y aplicar alcance por rol en el servidor.
- **T21 — Pruebas de consulta:** validar conteos de los seis estados, enero/diciembre, año futuro, año/filtro sin datos, médico propio, agendador con todos/filtro y acceso no autorizado.
- **T22 — Interfaz:** selector de año, selector de médico para agendador, gráfico mensual y desglose de estados en gráfico/tabla, con estados de carga/vacío/error. No transferir datos personales para dibujarlo.
- **T23 — Usabilidad:** probar si un usuario puede elegir año, identificar meses/picos y comparar profesionales; registrar observaciones y cambios.
- **Salida:** el gráfico coincide con datos de control y los criterios RF-7 son demostrables.

### Fase 3 — RNF-3, completar la pirámide

- **Unidad:** reglas de plazo/estado y validadores; agregación mensual y autorización.
- **Integración:** endpoint real con base temporal PostgreSQL para persistencia, respuesta y roles. Aislar Keycloak o usar tokens de prueba controlados, sin depender de un realm personal.
- **Sistema/aceptación:** recorrido de navegador con datos sintéticos. Propuesta: Playwright, después de confirmar el estado del proyecto y agregar su dependencia/configuración en una tarea dedicada.
- **Regresión:** correr pruebas backend, frontend, build y migraciones; volver a probar RF-1 a RF-5.
- **Salida:** comandos reproducibles, resultados guardados en CI y matriz de requisito → prueba → resultado.

### Fase 4 — RNF-4, automatización CI/CD

- **T40 — CI en pull request:** configurar un workflow en `.github/workflows/` para validar Compose, instalar dependencias reproducibles, correr pruebas Django y frontend, comprobar migraciones y construir las imágenes.
- **T41 — Integración/E2E en CI:** levantar dependencias desechables o un entorno de test aislado; nunca usar el volumen local ni las credenciales de despliegue.
- **T42 — Despliegue condicionado:** después de validaciones verdes, desplegar automáticamente al único ambiente cloud académico. Guardar credenciales en secretos del repositorio/entorno, no en YAML ni imágenes.
- **Salida:** un PR con fallo no puede desplegar; el ambiente demo identifica la versión/commit y conserva logs y resultado de smoke test. No crear un staging permanente si el plan gratuito no lo permite.

### Fase 5 — RNF-5, preparación y despliegue cloud

- **T50 — Selección del proveedor:** aplicar la lista de verificación de la sección 6 y registrar proveedor, fecha, límites, región y costos potenciales. Usar solo planes/servicios gratuitos, sin activar herramientas de pago ni cobros.
- **T51 — Producción:** crear configuración/imagenes de producción separadas de Compose de desarrollo. Reemplazar `runserver` por un servidor WSGI de producción (por ejemplo, Gunicorn), `ng serve` por el build/servidor SSR de Angular o hosting estático validado, y `start-dev` de Keycloak por su modo de producción.
- **T52 — Servicios persistentes:** configurar PostgreSQL administrado/persistente, respaldo, usuario/credenciales de mínimo privilegio y base separada para Keycloak.
- **T53 — URLs y secretos:** establecer HTTPS, dominios, callback/redirect URIs de Keycloak, CORS exacto, `ALLOWED_HOSTS`, emisor JWT, JWKS y secretos en el gestor del proveedor.
- **T54 — Publicar y comprobar:** aplicar migraciones como paso de release; desplegar API, Keycloak y frontend; correr smoke/aceptación y registrar URL y commit.
- **T55 — Operación/rollback:** documentar rollback de versión, respaldo/restauración y responsable para suspender el servicio si una prueba o escaneo revela exposición.
- **Salida:** ambiente HTTPS alcanzable, login y recorridos completos, persistencia validada y procedimiento de recuperación documentado.

### Fase 6 — RNF-6 opcional y cierre

- Añadir endpoint de salud sin datos sensibles y checks de disponibilidad.
- Centralizar logs de aplicación/proveedor y definir retención; excluir contraseñas, JWT, justificaciones clínicas y datos personales.
- Añadir alerta básica de indisponibilidad si el nivel gratuito lo permite.
- Completar documento, matriz de evidencia, demostración y retroalimentación.

## 6. Guía de despliegue en la nube

### 6.1 Arquitectura objetivo

No usar en producción los procesos de desarrollo actuales. La arquitectura lógica mínima conserva cuatro responsabilidades:

1. **Frontend Angular:** sirve la aplicación compilada; si conserva SSR, requiere un servicio Node persistente/compatible. Si se elige hosting estático, comprobar que todas las rutas Angular funcionan al recargar la página.
2. **API Django:** imagen propia, servidor WSGI de producción, HTTPS, migraciones como paso controlado y variables de configuración externas.
3. **Keycloak:** servicio independiente, HTTPS, modo de producción, realm/client configurados con orígenes permitidos explícitos y almacenamiento persistente.
4. **PostgreSQL:** persistente/administrado; una base o credenciales separadas para Django y Keycloak, respaldo y prueba de restauración.

El Docker Compose actual es de desarrollo: publica puertos locales, monta el código, ejecuta `ng serve`, Django `runserver` y Keycloak `start-dev`. **No desplegarlo tal cual** ni utilizar el volumen PostgreSQL local como almacenamiento cloud.

### 6.2 Compuerta de selección sin herramientas de pago

No se selecciona proveedor en el enunciado. La condición acordada es no utilizar herramientas/servicios de pago ni generar costos para el despliegue; no se exige una marca de nube. Antes de crear recursos:

- Verificar en documentación oficial que el plan seleccionado sea gratuito, sus cuotas y qué acciones podrían generar cobros.
- Confirmar soporte de contenedores (o alternativa compatible), persistencia PostgreSQL, HTTPS, variables secretas, logs y URL pública.
- Revisar límites de CPU/memoria; Keycloak y Django requieren recursos para iniciar y sostener la demo.
- Confirmar si los servicios se suspenden por inactividad, si la base expira, límites de almacenamiento, cuota mensual, regiones y condiciones de eliminación.
- Registrar fecha, URLs oficiales, cuota gratuita y responsables. Guardar capturas de la configuración de costo/plan sin exponer información de cuenta.
- No habilitar servicios, cuotas, extensiones ni upgrades pagos. Si la única ruta disponible implica pago, detener el despliegue y acordar otra opción académica.

| Servicio | Capacidad que se debe confirmar | Prueba de aceptación de proveedor |
|---|---|---|
| Frontend | HTTPS, rutas SPA/SSR y dominio estable. | Login redirige y las rutas internas sobreviven a recarga. |
| API Django | Contenedor Python, health check, configuración privada y logs. | API responde por HTTPS; secreto no aparece en logs/config pública. |
| Keycloak | Contenedor Java con memoria suficiente, hostname HTTPS y persistencia. | Realm y login sobreviven a reinicio; token tiene issuer esperado. |
| PostgreSQL | Persistencia real, límites claros, backups y conexiones seguras. | Datos sobreviven a reinicio y una copia se restaura en entorno de prueba. |

### 6.3 Secuencia de despliegue

1. Crear cuentas únicamente después de aprobar la compuerta anterior. Seleccionar entorno de prueba con datos sintéticos.
2. Crear PostgreSQL y credenciales distintas por servicio; restringir acceso por red/SSL cuando el proveedor lo permita. Activar backup y registrar retención.
3. Crear Keycloak con almacenamiento persistente. Cambiar contraseñas por defecto; no utilizar `admin/admin123` ni las credenciales demo fuera del equipo local.
4. Configurar realm `piedrazul`, cliente de Angular y roles. Actualizar URL del issuer, callbacks y web origins a los dominios HTTPS finales; limitar redirecciones a los dominios del proyecto.
5. Crear los servicios de backend, frontend y Keycloak desde Dockerfiles/imágenes de producción. Guardar imágenes y asociarlas con commit/tag inmutable.
6. Definir secretos/variables en el proveedor: `DJANGO_SECRET_KEY`, `DJANGO_DEBUG=False`, `DJANGO_ALLOWED_HOSTS`, conexión de DB, URLs de Keycloak, issuer/JWKS, origen CORS y credenciales administrativas de servicio con mínimo privilegio.
7. Comprobar la configuración antes de publicar: no hay secretos en `.env` versionado, Docker build args, logs o bundle del frontend. Solo configuración pública (URLs/realm/client ID) puede estar en el cliente.
8. Desplegar primero API y Keycloak. Ejecutar migraciones una vez por release, verificar health check, esquema, issuer JWKS y login desde una URL HTTPS válida.
9. Desplegar el frontend con URL de API y Keycloak públicas; comprobar CORS y flujos de autenticación en navegador real, no solo desde dentro de Docker.
10. Ejecutar smoke test: inicio de sesión por rol, agenda, crear cita sintética, cancelar con motivo, verificar liberación de horario, consultar reporte anual y denegación de acceso indebido.
11. Ejecutar ZAP contra el ambiente académico con datos sintéticos, revisar hallazgos manualmente y corregir o documentar riesgos antes de la sustentación.
12. Registrar URL, commit, resultado de pruebas, configuración de backups, límites del plan y procedimiento de rollback en el documento de arquitectura.

**Protección de datos:** usar solo personas y citas ficticias en hosting gratuito o de aula. No cargar nombres, documentos, teléfonos, tokens o historias clínicas reales. Mantener acceso a base de datos restringido y eliminar datos demo al terminar la evaluación si el proveedor no garantiza su retención/aislamiento.

## 7. CI/CD y estrategia de pruebas

### Flujo sugerido

```text
Rama de trabajo
    -> Pull request
       -> pruebas backend + frontend
       -> verificación de migraciones
       -> build de imágenes
       -> integración/E2E
    -> merge aprobado
       -> build/tag de imagen
       -> migración controlada
       -> despliegue al ambiente cloud académico
       -> health check + smoke test + ZAP
       -> rollback si falla
```

### Matriz mínima de pruebas

| Capa | Casos mínimos | Evidencia |
|---|---|---|
| Unidad | Límite temporal, transiciones de estado, motivo, validación de año y agregación. | Test name, salida de runner y commit. |
| Integración | PATCH de cancelación real con DB; permiso/propietario/agendador; liberar franja. API de reporte con conteos y roles. | Runner contra PostgreSQL aislado. |
| Sistema/aceptación | Navegador inicia sesión como paciente/agendador y cancela; médico consulta su reporte; agendador filtra; rol no permitido es rechazado. | Playwright trace/captura y resultado CI, sin PII. |
| Regresión | RF-1 a RF-5, autenticación JWT y exportación/agendas no se degradan. | Suite existente y pruebas de regresión seleccionadas. |
| Seguridad | ZAP en el ambiente cloud académico y revisión de autorización, CORS, secretos y datos sensibles. | Informe breve y sanitizado, con severidad/impacto/mitigación. |

Para OWASP ZAP, los informes existentes en `zap-reports/` deben revisarse por fecha y ambiente; `.gitignore` los excluye. Versionar un resumen seguro (o reporte sanitizado) y capturas necesarias en el entregable. No entregar tokens, credenciales, cookies ni información de pacientes.

## 8. Cómo completar el documento de arquitectura

El entregable del curso pide portada, introducción, historias de tres iteraciones, prototipos y prueba de usabilidad, pantallazos de planeación de tres sprints, arquitectura final C4 y 4+1, patrones, CI/CD, despliegue cloud y monitoreo opcional. Usar esta estructura como índice del documento académico:

1. **Portada:** curso, institución, nombre del sistema, integrantes, docente, grupo y fecha. No inventar autores ni URLs.
2. **Introducción:** propósito, alcance del corte, usuarios y límites del prototipo/despliegue.
3. **Historias y aceptación:** tablas por iteración/sprint. Incluir RF-6/RF-7 con aceptación acordada, rol, prioridad, tarea y prueba asociada. Identificar claramente qué fue entregado y qué quedó limitado.
4. **Prototipos y usabilidad:** imagen/enlace del prototipo vigente, tareas pedidas a participantes, perfil y número de participantes, consentimiento, métricas (éxito, tiempo, errores, comprensión) y cambios derivados. No afirmar que hubo prueba si no se ejecutó.
5. **Planificación:** una captura legible por cada uno de los tres sprints desde Jira/Trello, con fechas, responsables/estados y vínculo a historias. La captura debe reflejar el tablero real.
6. **Arquitectura C4:** contexto (paciente, médico/terapista, agendador/admin, servicios externos), contenedores (Angular, Django, Keycloak, PostgreSQL y hosting), componentes relevantes y despliegue. Añadir leyenda, tecnologías, protocolos, límites de confianza y dirección de datos.
7. **Modelo 4+1:** vista lógica (dominio/capas), proceso (login, reservar/cancelar, consultar reporte), desarrollo (módulos), física (nodos/servicios cloud) y escenarios (+1) que validan las vistas. Explicar cómo los diagramas C4 complementan estas vistas sin duplicarlas.
8. **Patrones:** describir solo patrones comprobables en el código final. Hoy es verificable **State** en `backend/appointments/states.py`. Para cada patrón: problema, clases/archivos, decisión y beneficio. No llamar “patrón” a una dependencia/librería por sí sola.
9. **CI/CD:** diagrama del pipeline, eventos, gates, pruebas, registro de imágenes, despliegue por ambiente, manejo de secretos, aprobación/rollback y enlace al workflow real.
10. **Despliegue cloud:** diagrama físico y tabla de servicio/proveedor/rol/URL/plan gratuito/límite/secretos/persistencia/backup. Incluir fecha de verificación de costo cero, límites del proveedor y confirmación de que no se activaron servicios de pago.
11. **Monitoreo (opcional):** health checks, logs, retención, alertas, responsable y datos que nunca se registran.
12. **Cierre y anexos:** repositorio Git, URL cloud, informes sanitizados, comandos de prueba, enlaces de CI y créditos/roles de integrantes.

### Mapeo de rúbrica a evidencia

| Criterio del curso | Evidencia concreta que debe quedar en el documento/repositorio |
|---|---|
| Funcionales (30 %) | RF-6 y RF-7 demostrados; criterios de aceptación, pruebas y capturas. Regresión de RF-1 a RF-5. |
| No funcionales/modificabilidad/seguridad/usabilidad (30 %) | Estructura modular, patrones verificables, controles de autorización, ZAP, prueba de usabilidad, secretos fuera del código. |
| Arquitectura (20 %) | C4, 4+1, diagrama CI/CD y despliegue, descripción clara de decisiones y límites. |
| Sustentación (20 %) | Demo funcional, argumentos de negocio, URLs/evidencia accesibles, reparto de participación y respuestas técnicas preparadas. |

## 9. Preparación de la sustentación

El protocolo del documento reserva 10 minutos para mostrar el producto y 5 minutos para preguntas. Preparar una demo con datos sintéticos y una ruta de respaldo si el hosting gratuito está dormido o sin conexión:

- 0–2 min: problema, usuarios y arquitectura en una lámina.
- 2–5 min: paciente inicia sesión, ve su cita, cancela en plazo y presenta motivo; mostrar el rechazo fuera de plazo sin datos reales.
- 5–7 min: médico/terapista selecciona año y explica cómo leer el reporte.
- 7–8 min: mostrar pruebas/CI y resultado del escaneo ZAP sanitizado.
- 8–10 min: despliegue cloud, límites/costos del plan y controles de seguridad.
- 5 min de preguntas: designar responsable por arquitectura, datos/pruebas, seguridad y despliegue; todos los integrantes deben participar según el protocolo académico.

## 10. Criterio de cierre

No dar por terminado el corte hasta que:

- [ ] RF-6 respete las 48 horas inclusivas, los permisos de paciente/agendador, el motivo de 500 caracteres y la terminalidad de cancelación.
- [ ] RF-7 muestre todos los estados, el alcance por rol, el año actual/anterior y los estados vacíos acordados.
- [ ] Pruebas backend/frontend e integración/aceptación se ejecuten automáticamente y estén verdes.
- [ ] CI/CD despliegue un commit identificable y el smoke test cloud pase.
- [ ] Se haya desplegado usando exclusivamente planes/servicios gratuitos; límites, costos, proveedor y URL estén documentados.
- [ ] El diagrama de arquitectura, CI/CD y despliegue coincida con la [guía de diagramas](./Guia_Diagramas_Arquitectura_CI_CD_Despliegue.md).
- [ ] ZAP y los flujos JWT/RBAC estén revisados en el ambiente cloud académico; evidencia publicada sin secretos ni datos reales.
- [ ] El documento de arquitectura incluya todos los puntos solicitados y enlaces/capturas verificables.
- [ ] El equipo haya ensayado la demo de 10 minutos y las preguntas de 5 minutos.

