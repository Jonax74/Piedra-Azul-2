# Guía completa del frontend Angular

## Contexto

Esta guía corresponde al proyecto nuevo, cuya arquitectura será:

- Frontend: Angular SPA
- Estilos: Bootstrap y SCSS
- Backend: Django + Django REST Framework
- Base de datos: PostgreSQL
- Autenticación: Keycloak
- Arquitectura: monolito modular

La carpeta `Piedra-Azul-2 - copia` se utiliza únicamente como guía. La implementación real debe hacerse en:

```text
C:\Users\usuario\Desktop\Proyecto Piedra azul\Piedra-Azul-2
```

---

## 1. Verificar herramientas

Desde PowerShell:

```powershell
node --version
npm --version
ng version
```

Si Angular CLI no está instalado:

```powershell
npm install -g @angular/cli
```

Volver a verificar:

```powershell
ng version
```

---

## 2. Crear la aplicación Angular

Desde la raíz del proyecto nuevo:

```powershell
cd "C:\Users\usuario\Desktop\Proyecto Piedra azul\Piedra-Azul-2"
```

Si la carpeta `frontend` está vacía, se puede crear allí directamente. Una opción segura es:

```powershell
ng new frontend-app --routing --style=scss --skip-git
```

Configurar:

- Angular routing: `Yes`
- Stylesheet: `SCSS`

Debe existir una única aplicación frontend. Evitar mantener simultáneamente `frontend` y `frontend-app` para no crear confusión.

---

## 3. Instalar Bootstrap

```powershell
cd frontend-app
npm install bootstrap
```

En `angular.json`, dentro de `styles`, agregar:

```json
"node_modules/bootstrap/dist/css/bootstrap.min.css"
```

Conservar también el archivo de estilos de la aplicación:

```json
"styles": [
  "node_modules/bootstrap/dist/css/bootstrap.min.css",
  "src/styles.scss"
]
```

---

## 4. Verificar Angular

```powershell
ng serve
```

Abrir:

```text
http://localhost:4200
```

Detener con `Ctrl + C`.

---

## 5. Crear la estructura funcional

Desde la carpeta del frontend:

```powershell
ng generate component layout
ng generate component pages/login
ng generate component pages/dashboard
ng generate component pages/agenda
ng generate component pages/agendar-cita
ng generate component pages/mis-citas
ng generate component pages/medicos
ng generate component pages/disponibilidad
ng generate component pages/configuracion
ng generate service core/services/api
ng generate service core/services/auth
ng generate service core/services/citas
ng generate service core/services/personas
ng generate service core/services/medicos
ng generate service core/services/disponibilidad
ng generate service core/services/configuracion
ng generate guard core/guards/auth
ng generate guard core/guards/role
ng generate interceptor core/interceptors/auth
```

Crear también estas carpetas:

```text
src/app/
├── core/
│   ├── guards/
│   ├── interceptors/
│   └── services/
├── layout/
├── pages/
│   ├── login/
│   ├── dashboard/
│   ├── agenda/
│   ├── agendar-cita/
│   ├── mis-citas/
│   ├── medicos/
│   ├── disponibilidad/
│   └── configuracion/
├── shared/
│   ├── components/
│   └── models/
└── app.routes.ts
```

---

## 6. Configurar entornos

Crear o adaptar:

```text
src/environments/environment.ts
src/environments/environment.prod.ts
```

Desarrollo:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://127.0.0.1:8000/api',
  keycloakUrl: 'http://localhost:8080',
  keycloakRealm: 'piedrazul',
  keycloakClientId: 'piedrazul-frontend',
};
```

Producción debe utilizar las URLs reales del entorno desplegado. No incluir secretos en estos archivos.

---

## 7. Configurar HttpClient

En `src/app/app.config.ts`:

```typescript
import { provideHttpClient } from '@angular/common/http';
```

Agregar `provideHttpClient()` dentro de `providers`.

Verificar que Django esté funcionando:

```powershell
cd "C:\Users\usuario\Desktop\Proyecto Piedra azul\Piedra-Azul-2\backend"
.\venv\Scripts\Activate.ps1
python manage.py runserver
```

---

## 8. Crear el servicio API base

En `src/app/core/services/api.service.ts`, centralizar inicialmente:

- `/api/health/`
- `/api/citas/disponibles/`
- `/api/citas/agenda/`
- `/api/citas/`
- `/api/configuracion/`

Ejemplo:

```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  getHealth() {
    return this.http.get(`${this.apiUrl}/health/`);
  }

  getAvailableSlots(medicoId: number, fecha: string) {
    return this.http.get(`${this.apiUrl}/citas/disponibles/`, {
      params: { medico: medicoId, fecha },
    });
  }

  getAppointments(medicoId: number, fecha: string) {
    return this.http.get(`${this.apiUrl}/citas/agenda/`, {
      params: { medico: medicoId, fecha },
    });
  }
}
```

Más adelante, separar las llamadas en servicios por dominio.

---

## 9. Probar conexión Angular-Django

Crear una pantalla temporal de dashboard que consulte:

```text
GET http://127.0.0.1:8000/api/health/
```

Respuesta esperada:

```json
{
  "status": "ok",
  "service": "piedra-azul-backend"
}
```

Ejecutar Angular:

```powershell
cd frontend-app
ng serve
```

Abrir:

```text
http://localhost:4200/dashboard
```

No continuar hasta comprobar esta comunicación.

---

## 10. Integrar Keycloak

Instalar el cliente:

```powershell
npm install keycloak-js
```

Configurar:

- URL: `http://localhost:8080`
- realm: `piedrazul`
- client ID: `piedrazul-frontend`
- cliente público
- flujo Authorization Code + PKCE

Crear `AuthService` con métodos para:

- iniciar sesión;
- cerrar sesión;
- obtener token;
- renovar token;
- consultar usuario;
- consultar roles;
- saber si hay sesión activa.

No guardar contraseñas en Angular.

No usar `client_secret` en el frontend.

---

## 11. Crear el interceptor JWT

El interceptor debe:

1. obtener el token actual de Keycloak;
2. clonar la petición HTTP;
3. agregar:

```http
Authorization: Bearer TOKEN
```

4. enviar la petición al backend;
5. renovar el token si está próximo a expirar;
6. cerrar sesión o redirigir al login si el token ya no es válido.

No adjuntar token innecesariamente a recursos públicos.

---

## 12. Crear guards

### AuthGuard

Protege rutas que requieren sesión:

- dashboard;
- citas;
- agenda;
- configuración;
- médicos;
- disponibilidades.

### RoleGuard

Protege por roles:

- `ADMIN`: configuración, médicos y disponibilidades;
- `AGENDADOR`: agenda de citas;
- `PACIENTE`: agendamiento y mis citas;
- `MEDICO`: consulta de sus citas.

Si el usuario no tiene permisos, redirigir a dashboard o una página de acceso denegado.

---

## 13. Crear rutas de la SPA

Rutas sugeridas:

```text
/login
/dashboard
/agenda
/agendar-cita
/mis-citas
/medicos
/disponibilidad
/configuracion
```

Configurar redirección inicial:

```text
/ -> /dashboard
```

Las rutas deben usar `AuthGuard` y `RoleGuard` cuando corresponda.

---

## 14. Crear layout principal

El layout debe contener:

- encabezado;
- menú lateral o barra de navegación;
- nombre del usuario;
- rol del usuario;
- botón de cerrar sesión;
- área para contenido de rutas;
- mensajes globales.

El menú debe ocultar opciones que no correspondan al rol.

---

## 15. Crear la pantalla de login

La pantalla debe:

- mostrar la identidad visual de Piedrazul;
- ofrecer el botón de inicio de sesión;
- redirigir a Keycloak;
- mostrar errores de autenticación;
- regresar al dashboard después del login.

El formulario no debe pedir ni almacenar la contraseña localmente.

---

## 16. Crear dashboard por rol

### Paciente

- próximas citas;
- botón para agendar;
- acceso a mis citas.

### Agendador

- acceso a agenda;
- búsqueda rápida por médico y fecha;
- cantidad de citas del día.

### Médico

- citas asignadas;
- disponibilidad propia.

### Administrador

- configuración;
- médicos;
- disponibilidades;
- resumen del sistema.

---

## 17. Implementar requisito 1: agenda

Historia: el agendador lista citas de un médico o terapista en una fecha determinada.

Crear `pages/agenda` con:

- selector de médico;
- selector de fecha;
- botón buscar;
- tabla de resultados;
- contador de citas;
- estado vacío;
- mensaje de error.

Consumir:

```text
GET /api/citas/agenda/?medico={id}&fecha={yyyy-mm-dd}
```

Mostrar:

- paciente;
- médico;
- fecha;
- hora;
- estado;
- observación.

Restringir la vista a `ADMIN` y `AGENDADOR`.

---

## 18. Implementar requisito 2: franjas y agendamiento

Crear `pages/agendar-cita`.

Flujo:

1. seleccionar especialidad;
2. seleccionar médico;
3. seleccionar fecha;
4. consultar franjas disponibles;
5. mostrar horarios como botones o tarjetas seleccionables;
6. seleccionar una franja;
7. confirmar datos;
8. enviar `POST /api/citas/`;
9. mostrar confirmación;
10. actualizar la disponibilidad.

Consumir:

```text
GET /api/citas/disponibles/?medico={id}&fecha={yyyy-mm-dd}
POST /api/citas/
```

Manejar errores de:

- horario ocupado;
- médico inactivo;
- fecha festiva;
- fecha fuera de ventana;
- falta de disponibilidad;
- token vencido.

Restringir la vista a `PACIENTE` y, si el alcance lo permite, a `AGENDADOR`.

---

## 19. Implementar mis citas

Crear `pages/mis-citas`.

Debe permitir:

- listar citas del paciente autenticado;
- ver fecha y hora;
- ver médico;
- ver estado;
- ver observación;
- cancelar o reagendar cuando el backend lo permita.

La API debe filtrar los datos por el usuario autenticado.

---

## 20. Implementar administración de médicos

Crear `pages/medicos`.

Funcionalidades:

- listar médicos;
- crear médico;
- editar médico;
- activar o desactivar médico;
- asignar especialidades;
- consultar datos profesionales.

Restringir a `ADMIN`.

---

## 21. Implementar disponibilidades

Crear `pages/disponibilidad`.

Funcionalidades:

- seleccionar médico;
- seleccionar día;
- definir hora inicial;
- definir hora final;
- definir intervalo;
- crear disponibilidad;
- asignar disponibilidad al médico;
- eliminar disponibilidad.

Restringir a `ADMIN` y, según las historias de usuario, a `MEDICO` para su propia disponibilidad.

---

## 22. Implementar configuración del sistema

Crear `pages/configuracion`.

Consumir:

```text
GET /api/configuracion/
PUT /api/configuracion/
```

Permitir al administrador:

- consultar semanas habilitadas;
- modificar ventana de agendamiento;
- activar o desactivar configuración;
- registrar festivos cuando exista el endpoint;
- ver confirmación de cambios;
- ver errores de validación.

Restringir exclusivamente a `ADMIN`.

---

## 23. Crear servicios por dominio

Separar las llamadas HTTP:

```text
src/app/core/services/
├── api.service.ts
├── auth.service.ts
├── citas.service.ts
├── personas.service.ts
├── medicos.service.ts
├── disponibilidad.service.ts
└── configuracion.service.ts
```

Cada servicio debe tener métodos con tipos explícitos.

---

## 24. Crear modelos TypeScript

Crear interfaces en `shared/models`:

```text
usuario.model.ts
paciente.model.ts
medico.model.ts
especialidad.model.ts
disponibilidad.model.ts
cita.model.ts
configuracion.model.ts
```

Definir interfaces para:

- respuesta de agenda;
- respuesta de franjas;
- cita;
- usuario autenticado;
- configuración.

Evitar `any` salvo casos excepcionales.

---

## 25. Crear formularios reactivos

Usar `ReactiveFormsModule` para:

- registro o edición de pacientes;
- médicos;
- citas;
- disponibilidades;
- configuración.

Validar:

- campos obligatorios;
- fechas válidas;
- fechas no pasadas;
- horas válidas;
- intervalo mayor que cero;
- correo;
- DNI;
- observaciones.

Las validaciones del frontend mejoran UX, pero las definitivas permanecen en Django.

---

## 26. Crear componentes reutilizables

Crear componentes compartidos para:

- tablas;
- selector de médico;
- selector de fecha;
- selección de franjas;
- alertas;
- mensajes de error;
- indicadores de carga;
- modales de confirmación;
- estados vacíos.

---

## 27. Aplicar Bootstrap y diseño responsive

Configurar estilos para:

- barra de navegación;
- formularios;
- tablas;
- botones;
- tarjetas;
- alertas;
- modales;
- estados de carga;
- mensajes de error.

Verificar en:

- escritorio;
- tablet;
- móvil.

---

## 28. Manejar estados de interfaz

Cada pantalla debe tener estados para:

- carga;
- datos cargados;
- lista vacía;
- error del servidor;
- error de autorización;
- éxito;
- formulario inválido.

No dejar pantallas en blanco durante llamadas HTTP.

---

## 29. Probar integración con backend

Probar como mínimo:

```text
GET  /api/health/
GET  /api/citas/disponibles/
GET  /api/citas/agenda/
POST /api/citas/
GET  /api/configuracion/
PUT  /api/configuracion/
```

En las herramientas del navegador revisar **Network** y confirmar que las peticiones protegidas llevan `Authorization: Bearer ...`.

---

## 30. Pruebas unitarias Angular

Crear pruebas para:

- `AuthService`;
- interceptor;
- guards;
- servicios HTTP;
- formularios;
- componentes principales;
- validaciones de roles;
- manejo de errores.

Ejecutar:

```powershell
ng test
```

---

## 31. Pruebas de aceptación por rol

### Paciente

- iniciar sesión;
- consultar franjas;
- seleccionar horario;
- agendar cita;
- consultar sus citas;
- recibir errores claros si el horario no está disponible.

### Agendador

- iniciar sesión;
- seleccionar médico;
- seleccionar fecha;
- consultar agenda;
- ver cantidad de citas.

### Administrador

- iniciar sesión;
- modificar configuración;
- administrar médicos;
- administrar disponibilidades;
- acceder a funciones administrativas.

### Seguridad

- usuario sin sesión no accede;
- paciente no ve configuración;
- agendador no modifica configuración;
- token vencido provoca reautenticación;
- endpoints protegidos rechazan solicitudes sin token.

---

## 32. Construcción de producción

Ejecutar:

```powershell
ng build
```

Corregir errores de:

- TypeScript;
- rutas;
- imports;
- estilos;
- variables de entorno.

No almacenar secretos en Angular.

---

## 33. Documentación del frontend

Actualizar `README.md` con:

- requisitos de Node y Angular;
- instalación;
- ejecución local;
- variables de entorno;
- URL de Angular;
- URL de Django;
- URL de Keycloak;
- usuarios de prueba;
- roles;
- comandos de pruebas;
- comando de producción.

---

## 34. Orden final de implementación

1. verificar Node y Angular;
2. crear SPA;
3. instalar Bootstrap;
4. probar Angular vacío;
5. configurar entornos;
6. activar `HttpClient`;
7. probar conexión con Django;
8. integrar Keycloak;
9. crear interceptor;
10. crear guards;
11. crear layout;
12. crear login;
13. crear dashboard;
14. implementar agenda;
15. implementar franjas y agendamiento;
16. implementar mis citas;
17. implementar médicos;
18. implementar disponibilidades;
19. implementar configuración;
20. agregar pruebas;
21. ejecutar `ng build`;
22. documentar.

---

## 35. Criterio de finalización

El frontend estará listo para el primer corte cuando:

- Angular se ejecute como SPA;
- el usuario pueda autenticarse con Keycloak;
- el token se envíe a Django;
- el requisito 1 funcione desde la pantalla de agenda;
- el requisito 2 funcione desde la pantalla de agendamiento;
- el requisito 3 funcione desde configuración;
- los permisos por rol funcionen;
- existan pruebas unitarias;
- `ng build` termine sin errores;
- la documentación del frontend esté actualizada.

---

# Paso 7: Estado real del proyecto, lo hecho y lo que falta

## 1. Resumen general

Este proyecto se está desarrollando con una arquitectura de monolito modular, pero con una separación funcional clara por dominios. La intención es mantener una base sólida y un flujo de negocio bien definido, siguiendo lo que pide el entregable del primer corte.

La estructura actual contempla:

- Backend en Django + DRF
- Frontend en Angular
- Base de datos PostgreSQL
- Autenticación con Keycloak
- Diseño modular por dominios y servicios
- Reglas de negocio centradas en el backend

La idea principal es que el backend resuelva la lógica real del negocio y el frontend solo consuma esa lógica con una UI clara y validaciones de UX.

---

## 2. Lo que ya está hecho

### 2.1. Infraestructura base

- Se creó la estructura de contenedores para PostgreSQL y Keycloak.
- Se levantó la infraestructura con Docker Compose.
- Se validó que PostgreSQL se conecta correctamente.
- Se validó que Keycloak queda levantado y operativo.
- Se configuró el realm, usuarios y clientes necesarios para la autenticación.

### 2.2. Backend Django

- Se creó el proyecto Django con estructura modular.
- Se configuró la conexión a PostgreSQL.
- Se ejecutaron migraciones y se validó el proyecto.
- Se configuró el entorno de CORS y REST Framework.
- Se conectó Django con Keycloak mediante JWT.

### 2.3. Autenticación con Keycloak

- Se implementó validación de tokens JWT.
- Se configuró el issuer y la verificación de firma.
- Se validó que el usuario autenticado llega correctamente al backend.
- Se sincronizaron los usuarios con el sistema y se trajeron roles desde Keycloak.
- Se comprobó que `/api/me/profile/` entrega la información del usuario autenticado con roles.

### 2.4. Módulo de usuarios

- Se definieron modelos y servicios relacionados con usuarios.
- Se integró la relación entre usuario y su identidad en Keycloak.
- Se validó el flujo de autenticación y roles por usuario.

### 2.5. Módulo de personas

- Se implementaron modelos para personas, pacientes y médicos.
- Se definieron especialidades y entidades del negocio médico.
- Se validó que el modelo encaja con el flujo del proyecto.

### 2.6. Módulo de citas

- Se implementó la lógica de agenda.
- Se implementaron las franjas disponibles.
- Se validó el cálculo de disponibilidad por médico y fecha.
- Se contemplaron reglas como días festivos, ventanas de agendamiento y validación de horarios.
- Se implementó la creación de citas y consulta de disponibilidad.
- Se validó la API de citas y la lógica centralizada en servicios.

### 2.7. Endpoints funcionales del backend

Se validó que existen y responden correctamente los siguientes puntos clave:

- `/api/me/profile/`
- `/api/citas/disponibles/`
- `/api/citas/agenda/`
- `/api/configuracion/`
- `/api/citas/`

Estos endpoints son la base sobre la que el frontend deberá trabajar.

### 2.8. Reglas de negocio ya consolidadas

Se dejó resuelto el enfoque correcto del dominio:

- la disponibilidad depende de la configuración del sistema;
- la agenda se consulta por médico y fecha;
- la creación de citas valida condiciones de negocio;
- la lógica no debe duplicarse en frontend ni en vistas aisladas;
- la validación real debe quedar en el backend.

---

## 3. Lo que falta por hacer

### 3.1. Frontend Angular

Esto es lo que aún falta de forma prioritaria:

- crear la aplicación Angular desde cero;
- configurar el proyecto base con routing y SCSS;
- instalar Bootstrap y preparar la estructura visual;
- crear le layout general de la aplicación;
- definir la estructura por módulos y páginas;
- integrar Keycloak con el frontend;
- crear guards y interceptores para autenticación y roles;
- crear servicios HTTP por dominio;
- construir las pantallas y flujos funcionales;
- validar la UI con datos reales del backend;
- preparar pruebas unitarias y build final.

### 3.2. Pantallas faltantes

Las pantallas que deben implementarse siguen siendo estas:

- login
- dashboard
- agenda
- agendar cita
- mis citas
- médicos
- disponibilidad
- configuración

### 3.3. Roles y permisos

Hay que completar el flujo real de permisos por rol:

- Admin: configuración, médicos, disponibilidades y gestión general
- Médico: visualización y gestión de su agenda
- Paciente: agendamiento, disponibilidad y consultas personales
- Agendador: consulta y gestión de agenda

La parte de autorización debe quedar bien resuelta en rutas y UI para evitar que un usuario vea opciones que no le corresponden.

### 3.4. Validación del frontend con backend

Se debe validar que cada pantalla haga peticiones correctas al backend y que reciba correctamente el token JWT.

También es necesario validar:

- errores 401 y 403;
- errores de validación 400;
- llamadas con Authorization Bearer;
- comportamiento con fechas y franjas no disponibles;
- actualizaciones de estado en la UI tras una creación o cancelación.

### 3.5. Pruebas y cierre final

Falta:

- pruebas unitarias y component tests;
- pruebas de integración con backend;
- validación del flujo completo desde login hasta agendamiento;
- ejecución de `ng build` sin errores;
- actualización de documentación del frontend.

---

## 4. Qué ya está listo del negocio

El backend ya tiene lo necesario para sostener el primer corte funcional.

Esto significa que la parte más importante del proyecto ya no es inventar lógica, sino convertirla en una experiencia usable en Angular.

En otras palabras:

- la lógica del negocio está hecha,
- la API está operando,
- la base para el frontend ya existe,
- ahora el trabajo pendiente es la capa de presentación y flujo de usuario.

---

## 5. Qué falta exactamente para el frontend

### Prioridad 1: base del proyecto Angular

- crear la app Angular;
- instalar Bootstrap;
- configurar `HttpClient`;
- crear `environment.ts`;
- verificar que Angular conversa con Django.

### Prioridad 2: autenticación

- integrar Keycloak;
- crear AuthService;
- crear interceptor JWT;
- crear guards por sesión y por rol;
- proteger rutas.

### Prioridad 3: pantallas principales

- login;
- dashboard;
- agenda;
- agendar cita;
- mis citas;
- configuración;
- médicos y disponibilidades.

### Prioridad 4: pruebas

- validación de flujos reales;
- pruebas unitarias;
- pruebas por rol;
- verificación de `ng build`.

---

## 6. Orden recomendado de ejecución

Para no perder tiempo, el orden ideal es este:

1. crear Angular base;
2. montar Bootstrap y estructura visual;
3. conectar Angular con Django;
4. integrar Keycloak;
5. crear guards y interceptor;
6. construir layout y login;
7. crear dashboard por rol;
8. implementar agenda;
9. implementar agendamiento y franjas;
10. implementar mis citas;
11. implementar médicos y disponibilidad;
12. implementar configuración;
13. probar por roles;
14. ejecutar build final;
15. documentar.

---

## 7. Criterio de cierre del primer corte

El proyecto se puede considerar en un estado de primer corte cuando se cumpla lo siguiente:

- Angular está funcionando correctamente como SPA.
- El usuario puede autenticarse con Keycloak.
- El frontend envía el token al backend.
- La agenda se consulta desde UI.
- La disponibilidad y agendamiento funcionan claramente desde la vista del paciente.
- La configuración la maneja el admin.
- Los roles restringen acceso correctamente.
- El backend y el frontend trabajan juntos sin errores funcionales.
- El sistema está documentado y puede ejecutarse localmente.

---

## 8. Estado final real

### Hecho

- infraestructura levantada;
- PostgreSQL funcionando;
- Keycloak funcionando;
- backend Django activo;
- autenticación JWT validada;
- usuarios y roles funcionando;
- personas y citas del negocio implementadas;
- endpoints funcionales del negocio validados.

### Falta

- frontend Angular completamente implementado;
- login con UI y roles en Angular;
- pantallas principales del negocio;
- pruebas y validación de producción.

---

## 9. Conclusión

La parte más compleja del negocio ya quedó resuelta en backend. El proyecto ya tiene la base técnica y funcional necesaria para que el frontend sea la última pieza grande por cerrar.

La idea es simple: no se trata de volver a construir la lógica del negocio desde cero, sino de convertir lo que ya existe en una experiencia real de usuario con Angular, Keycloak y una navegación clara por roles.

Este documento sirve como referencia del estado real del proyecto y como guía para cerrar el primer corte.
