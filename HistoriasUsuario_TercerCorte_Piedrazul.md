# Servicios Médicos de Piedrazul

## Historias de Usuario y Criterios de Aceptación

**Sistema:** Reserva de citas médicas — Piedrazul  
**Corte:** Tercer Corte (2026.2)  
**Épicas:** 5  
**Historias de Usuario:** 19

## Convención de estado

- **Implementada:** existe flujo funcional en frontend y/o backend.
- **Implementada parcialmente:** existe la funcionalidad principal, pero faltan criterios completos.
- **Pendiente:** no existe una implementación funcional verificable.

---

# HE-01: Agendamiento autónomo de citas

## HU-1.1 — Agendar una cita

**Rol:** PACIENTE  
**Necesidad:** Agendar una cita para recibir atención médica sin utilizar WhatsApp.

**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Con una sesión válida, el paciente puede seleccionar profesional, fecha y franja disponible y crea la cita. | Implementado |
| 2 | El sistema permite seleccionar la primera franja disponible dentro del calendario. | Implementado parcialmente |
| 3 | El sistema impide agendar dos citas al mismo horario con el mismo médico. | Implementado |
| 4 | El sistema impide agendar con médicos inactivos o horarios sin disponibilidad. | Implementado |
| 5 | El sistema rechaza fechas posteriores a la ventana configurada por el administrador. | Implementado |
| 6 | El sistema muestra únicamente franjas libres dentro de la disponibilidad configurada. | Implementado |
| 7 | Si dos usuarios intentan simultáneamente la misma franja, la restricción única impide una cita doble. | Implementado parcialmente |
| 8 | Una solicitud sin autenticación o con token vencido no crea citas y requiere iniciar sesión. | Implementado |
| 9 | Para un paciente, la cita se registra para su propia identidad y no para la cédula enviada. | Implementado |

**Nota:** actualmente no se verifica en backend que el mismo paciente no tenga otra cita a la misma hora con un médico distinto.

---

## HU-1.2 — Cancelar una cita médica

**Rol:** PACIENTE  
**Necesidad:** Cancelar una cita ya agendada para liberar su espacio.

**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Una cita puede cancelarse con al menos 48 horas de anticipación y registra el estado CANCELADA. | Implementado |
| 2 | Si faltan menos de 48 horas, el sistema impide la cancelación. | Implementado |
| 3 | El paciente debe ingresar un motivo al cancelar. | Implementado parcialmente |
| 4 | Una cita cancelada o vencida no puede cancelarse nuevamente. | Implementado |
| 5 | Al cancelar una cita, su franja vuelve a estar disponible para agendar. | Implementado |
| 6 | Un paciente no puede modificar una cita perteneciente a otro paciente. | Implementado |
| 7 | Una cancelación sin token válido requiere autenticación. | Implementado |
| 8 | Ante un error del servidor, la interfaz debe informar el problema sin dejar cambios parciales. | Implementado parcialmente |

**Cambio de criterio:** el motivo de cancelación debe contener entre 10 y 250 caracteres. El sistema actual permite 500 caracteres y debe ajustarse antes de considerar este criterio totalmente implementado.

---

# HE-02: Agendamiento manual de citas

## HU-2.6 — Exportar citas como médico o terapista

**Rol:** MEDICO/TERAPISTA  
**Necesidad:** Exportar las citas de una fecha para organizar la jornada.

**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Exporta las citas propias de la fecha seleccionada en formato CSV. | Implementado |
| 2 | El sistema informa si no existen citas para la fecha. | Implementado parcialmente |
| 3 | Se requiere una fecha válida. | Implementado |
| 4 | Ante un error del servidor, la interfaz muestra una advertencia. | Implementado parcialmente |
| 5 | La exportación solo permite consultar las citas propias del médico autenticado. | Implementado |
| 6 | Los valores iniciados con `=`, `+`, `-` o `@` se prefijan con apóstrofo. | Implementado |

**Estructura real del CSV:** UTF-8 con BOM, separado por punto y coma (`;`), incluye una fila de título y las columnas:

`Fecha`, `Hora`, `Paciente`, `Documento`, `Teléfono`, `Estado`

El archivo actual se nombra `citas-{slug_del_médico}-{fecha}.csv` y está ordenado por hora.

---

## HU-2.7 — Exportar citas como agendador

**Rol:** AGENDADOR  
**Necesidad:** Exportar las citas de un médico o terapista en una fecha determinada.

**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Exporta las citas de un profesional y fecha seleccionados. | Implementado |
| 2 | No presenta citas si no existen registros para los filtros. | Implementado parcialmente |
| 3 | Requiere seleccionar profesional y fecha válida. | Implementado |
| 4 | Ante un error del servidor, muestra una advertencia. | Implementado parcialmente |
| 5 | Solo AGENDADOR y ADMIN pueden invocar la exportación. | Implementado |
| 6 | Los nombres y teléfonos iniciados con caracteres de fórmula se neutralizan. | Implementado |

**Estructura real del CSV:** mismo formato, columnas y nombre que la exportación del médico.

---

## HU-2.8 — Listar citas por médico y fecha

**Rol:** AGENDADOR  
**Necesidad:** Visualizar y contar las citas de un profesional en una fecha determinada.

**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Lista las citas filtradas por profesional y fecha. | Implementado |
| 2 | Muestra correctamente el estado vacío y una cantidad cero. | Implementado |
| 3 | Rechaza fechas posteriores al rango configurado cuando corresponda. | Implementado parcialmente |
| 4 | Marca los filtros obligatorios cuando faltan profesional o fecha. | Implementado parcialmente |
| 5 | Un PACIENTE no puede consultar la agenda general ni el listado restringido. | Implementado |
| 6 | Ante una falla del servidor, la interfaz informa que no fue posible cargar la agenda. | Implementado parcialmente |

---

## HU-2.9 — Consultar historial por cédula como médico

**Rol:** MEDICO/TERAPISTA  
**Necesidad:** Conocer el historial de citas de un paciente mediante su cédula.

**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Busca por cédula y muestra las citas ordenadas de la más reciente a la más antigua. | Implementado |
| 2 | Sugiere pacientes mientras se escribe una cédula de al menos dos dígitos. | Implementado parcialmente |
| 3 | Informa cuando un paciente registrado no tiene citas. | Implementado parcialmente |
| 4 | Informa cuando no existe un paciente para la cédula. | Implementado |
| 5 | Rechaza documentos vacíos, con letras o con caracteres especiales. | Implementado |
| 6 | Un PACIENTE no puede consultar el historial de otra persona. | Implementado |
| 7 | Ante una falla del servidor, informa el error o el tiempo de espera agotado. | Implementado parcialmente |

---

## HU-2.10 — Consultar historial por cédula como agendador

**Rol:** AGENDADOR  
**Necesidad:** Conocer el historial de citas de un paciente mediante su cédula.

**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Busca por cédula y muestra las citas ordenadas de la más reciente a la más antigua. | Implementado |
| 2 | Sugiere pacientes mientras se escribe la cédula. | Implementado parcialmente |
| 3 | Informa cuando el paciente no tiene citas. | Implementado parcialmente |
| 4 | Informa cuando no existe un paciente con esa cédula. | Implementado |
| 5 | Rechaza documentos vacíos, con letras o con caracteres especiales. | Implementado |
| 6 | Un PACIENTE no puede consultar el historial de otro paciente. | Implementado |
| 7 | Ante una falla del servidor, informa el error o el tiempo de espera agotado. | Implementado parcialmente |

---

## HU-2.12 — Generar reporte gráfico anual de citas

**Rol:** MEDICO/TERAPISTA  
**Necesidad:** Analizar las citas atendidas por mes y profesional durante un año.

**Estado:** Pendiente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Genera un gráfico de barras agrupadas por meses y profesionales. | Pendiente |
| 2 | Informa cuando el año no tiene citas. | Pendiente |
| 3 | Valida que se haya seleccionado un año válido de cuatro dígitos. | Pendiente |
| 4 | Representa con cero los meses sin citas. | Pendiente |
| 5 | Solo MEDICO/TERAPISTA puede generar el reporte. | Pendiente |
| 6 | Sin autenticación no entrega el reporte. | Pendiente |
| 7 | Ante un error del servidor no muestra un gráfico incompleto. | Pendiente |

**Nota:** no existe endpoint ni componente gráfico implementado para esta historia.

---

# HE-03: Registro de Usuarios

## HU-3.3 — Registrarme en el sistema

**Rol:** PACIENTE  
**Necesidad:** Crear una cuenta para agendar citas de forma autónoma.

**Estado:** Implementada.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Desde el inicio de sesión se puede abrir el registro y crear el usuario con rol PACIENTE. | Implementado |
| 2 | Valida que la cédula sea numérica y que la contraseña cumpla la política configurada. | Implementado |
| 3 | Marca los campos obligatorios vacíos. | Implementado |
| 4 | Impide registrar dos veces la misma cédula. | Implementado |
| 5 | Impide registrar un nombre de usuario duplicado. | Implementado |
| 6 | La solicitud pública ignora cualquier rol enviado y asigna únicamente PACIENTE. | Implementado |

---

# HE-04: Autenticación de Usuarios

## HU-4.1 — Inicio de sesión como administrador

**Rol:** ADMINISTRADOR  
**Estado:** Implementada.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Las credenciales válidas crean una sesión con rol ADMIN y muestran el panel administrador. | Implementado |
| 2 | Las credenciales incorrectas muestran un mensaje genérico. | Implementado |
| 3 | Los campos vacíos muestran validación. | Implementado |
| 4 | Keycloak impide iniciar sesión con una cuenta deshabilitada. | Implementado |
| 5 | Keycloak aplica la protección contra intentos fallidos configurada en el realm. | Implementado parcialmente |

---

## HU-4.2 — Inicio de sesión como médico o terapista

**Rol:** MEDICO/TERAPISTA  
**Estado:** Implementada.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Las credenciales válidas muestran el panel médico o terapista. | Implementado |
| 2 | Las credenciales incorrectas muestran un mensaje genérico. | Implementado |
| 3 | Los campos vacíos muestran validación. | Implementado |
| 4 | Una cuenta deshabilitada no inicia sesión. | Implementado |
| 5 | Keycloak controla el bloqueo temporal por intentos fallidos. | Implementado parcialmente |

---

## HU-4.3 — Inicio de sesión como agendador

**Rol:** AGENDADOR  
**Estado:** Implementada.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Las credenciales válidas muestran el panel de agendador. | Implementado |
| 2 | Las credenciales incorrectas muestran un mensaje genérico. | Implementado |
| 3 | Los campos vacíos muestran validación. | Implementado |
| 4 | Una cuenta deshabilitada no inicia sesión. | Implementado |
| 5 | Keycloak controla el bloqueo temporal por intentos fallidos. | Implementado parcialmente |

---

## HU-4.4 — Inicio de sesión como paciente

**Rol:** PACIENTE  
**Estado:** Implementada.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Las credenciales válidas muestran el panel de paciente. | Implementado |
| 2 | Las credenciales incorrectas muestran un mensaje genérico. | Implementado |
| 3 | Los campos vacíos muestran validación. | Implementado |
| 4 | Una cuenta deshabilitada no inicia sesión. | Implementado |
| 5 | Keycloak controla el bloqueo temporal por intentos fallidos. | Implementado parcialmente |

---

## HU-4.5 — Cerrar la sesión

**Rol:** USUARIO  
**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Al cerrar sesión se ejecuta el cierre de Keycloak y se elimina la sesión local. | Implementado |
| 2 | El token se renueva automáticamente mientras el refresh token sea válido. | Implementado |
| 3 | La interfaz gestiona el vencimiento y redirige al inicio de sesión. | Implementado parcialmente |
| 4 | Al cerrar la pestaña no se restaura automáticamente la sesión local. | Implementado |
| 5 | La API rechaza un token vencido o inválido. | Implementado |

---

## HU-4.6 — Limitar funciones según el rol

**Rol:** USUARIO AUTENTICADO  
**Estado:** Implementada.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Las operaciones permitidas se procesan según el rol del usuario. | Implementado |
| 2 | Las operaciones restringidas devuelven 403. | Implementado |
| 3 | Un token con firma o emisor inválido devuelve 401. | Implementado |
| 4 | Un token vencido devuelve 401 y la interfaz redirige al login. | Implementado |
| 5 | Una operación protegida sin token devuelve 401. | Implementado |
| 6 | Un PACIENTE no puede consultar ni modificar citas de otra persona. | Implementado |

---

## HU-4.7 — Crear usuarios y asignar roles mediante Keycloak

**Rol:** ADMINISTRADOR  
**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Un administrador puede crear usuarios MEDICO o AGENDADOR desde Keycloak/Postman. | Implementado |
| 2 | Keycloak rechaza nombres de usuario o correos duplicados. | Implementado |
| 3 | Keycloak rechaza solicitudes con datos obligatorios incompletos. | Implementado |
| 4 | Sin privilegios de administración, Keycloak rechaza la operación. | Implementado |
| 5 | Un administrador puede deshabilitar un usuario. | Implementado |

**Nota:** no existe una interfaz propia para administración de usuarios; el procedimiento se realiza desde Keycloak o la API administrativa.

---

# HE-05: Gestión de Horarios

## HU-5.1 — Definir ventana de agendamiento

**Rol:** ADMINISTRADOR  
**Estado:** Implementada.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Permite guardar una cantidad entera de semanas. | Implementado |
| 2 | Rechaza letras, caracteres especiales, decimales y valores vacíos. | Implementado |
| 3 | Solo permite valores entre 1 y 52 semanas. | Implementado |
| 4 | Los nuevos agendamientos respetan la ventana configurada. | Implementado |
| 5 | Al reducir la ventana se conservan las citas ya agendadas. | Implementado |
| 6 | Solo ADMIN puede modificar la configuración. | Implementado |

---

## HU-5.2 — Configurar días y franjas de atención

**Rol:** ADMINISTRADOR  
**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Permite asociar un profesional a uno o varios días y una franja válida. | Implementado |
| 2 | Requiere seleccionar al menos un día de atención. | Implementado parcialmente |
| 3 | Rechaza que la hora final sea menor o igual a la inicial. | Implementado |
| 4 | Impide guardar franjas solapadas para el mismo profesional y día. | Implementado parcialmente |
| 5 | Marca profesional y horas obligatorios. | Implementado |
| 6 | Conserva las citas existentes al modificar horarios. | Implementado |
| 7 | Solo ADMIN puede modificar horarios. | Implementado |

**Cambio de alcance:** el modelo actual ofrece únicamente lunes a viernes. Sábado y domingo quedan fuera hasta ampliar los días disponibles. La validación final de solapamientos debe aplicarse también en backend.

---

## HU-5.3 — Configurar intervalo entre citas

**Rol:** ADMINISTRADOR  
**Estado:** Implementada parcialmente.

| # | Criterio de aceptación | Estado |
|---|---|---|
| 1 | Permite establecer el intervalo del profesional. | Implementado |
| 2 | El intervalo permitido debe estar entre 5 y 120 minutos. | Implementado parcialmente |
| 3 | Rechaza letras, caracteres especiales, decimales y valores vacíos. | Implementado parcialmente |
| 4 | Rechaza intervalos mayores que la duración de la franja. | Implementado parcialmente |
| 5 | Las franjas disponibles se generan usando el intervalo configurado. | Implementado |
| 6 | Solo ADMIN puede modificar el intervalo. | Implementado |

**Nota:** aunque el campo admite actualmente valores menores a 5 y mayores a 120, el criterio oficial debe limitarlo a `5–120` minutos y validar el intervalo contra la duración de la franja en backend.

---

## Resumen de adaptación al estado actual

| Estado | Cantidad aproximada |
|---|---:|
| Implementadas | 7 |
| Implementadas parcialmente | 11 |
| Pendientes | 1 |

**Pendiente principal:** reporte gráfico por mes y médico/terapista (HU-2.12).

**Cambios funcionales recomendados antes de la sustentación:**

1. Ajustar el motivo de cancelación a un rango de 10 a 250 caracteres.
2. Formalizar el CSV según el formato realmente generado por el programa.
3. Impedir que un paciente tenga citas simultáneas con médicos distintos.
4. Aplicar el intervalo válido de 5 a 120 minutos en frontend y backend.
5. Validar solapamiento de franjas en backend.
