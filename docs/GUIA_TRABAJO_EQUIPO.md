# Guía de trabajo en equipo: Git, GitHub y CI

**Proyecto:** Piedrazul (repositorio `Jonax74/Piedra-Azul-2`)
**Vigente desde:** 25 de noviembre de 2026
**Aplica a:** todos los integrantes del equipo

Esta guía reemplaza cualquier forma anterior de trabajar con el repositorio. Léela completa una vez. Después, usa la sección 3 como lista de pasos cada vez que hagas un cambio.

---

## 1. Qué cambió y por qué

Desde ahora el repositorio tiene **integración continua (CI)** con GitHub Actions y **reglas de protección** en las ramas `develop` y `main`.

| Antes | Ahora |
|---|---|
| Se podía subir código directo a `develop`. | **Está bloqueado.** Todo cambio entra por un Pull Request (PR). |
| Nadie verificaba el código automáticamente. | Cada PR ejecuta 3 verificaciones automáticas. |
| Un cambio podía fusionarse sin revisión. | Se necesita **1 aprobación de otra persona** del equipo. |

Qué verifica el CI en cada PR (los nombres exactos aparecen en GitHub):

| Check | Qué hace |
|---|---|
| `Backend (Django)` | Instala dependencias, ejecuta `manage.py check`, verifica que no falten migraciones y corre las pruebas unitarias. |
| `Frontend (Angular)` | Instala con `npm ci`, hace el build de producción y corre las pruebas unitarias. |
| `Docker (build)` | Valida `docker-compose.yml` y construye las imágenes. |

Un PR solo se puede fusionar cuando **los 3 checks están en verde** y **tiene 1 aprobación**.

---

## 2. Ramas

| Rama | Para qué sirve | ¿Se puede subir código directo? |
|---|---|---|
| `main` | Versión estable. Es la candidata a despliegue. | **No.** Solo por PR desde `develop`. |
| `develop` | Rama de integración. Aquí se juntan todos los cambios. | **No.** Solo por PR desde una rama de trabajo. |
| `tipo/descripcion-corta` | Tu rama de trabajo personal para un cambio concreto. | Sí, es tuya. |

### Nombres de ramas de trabajo

Formato: `tipo/descripcion-en-minusculas-con-guiones`

| Tipo | Cuándo usarlo | Ejemplo |
|---|---|---|
| `feat` | Funcionalidad nueva | `feat/reporte-citas-mensual` |
| `fix` | Corrección de un error | `fix/validacion-cancelacion-48h` |
| `test` | Agregar o corregir pruebas | `test/servicio-cancelacion` |
| `docs` | Documentación | `docs/diagrama-despliegue` |
| `ci` | Pipeline, Docker, configuración | `ci/dockerfile-produccion` |
| `refactor` | Reordenar código sin cambiar comportamiento | `refactor/politica-cancelacion` |

Reglas:
- Una rama por tarea. No mezcles dos funcionalidades en la misma rama.
- Ramas cortas: si llevas más de 2 o 3 días sin integrar, divide la tarea.
- Sin espacios, tildes ni mayúsculas.

---

## 3. Flujo completo de trabajo, paso a paso

Todos los comandos están en **PowerShell**, ejecutados desde la **raíz del repositorio**.

### Paso 0. Preparación (solo la primera vez)

```powershell
git clone https://github.com/Jonax74/Piedra-Azul-2.git
cd Piedra-Azul-2
Copy-Item .env.example .env
docker compose up --build -d
```

Configura tu identidad en Git (una sola vez por computador):

```powershell
git config --global user.name "Tu Nombre"
git config --global user.email "tu-correo@ejemplo.com"
```

> Usa el mismo correo que tienes registrado en GitHub. Así tus commits aparecen asociados a tu cuenta, y los docentes pueden ver que **todos** participaron.

Para escribir en el repositorio necesitas que el dueño te haya agregado como **Collaborator**. Si al hacer `git push` recibes un error de permisos, pídele a Jonax74 que te invite.

### Paso 1. Actualizar `develop` antes de empezar

```powershell
git checkout develop
git pull origin develop
```

Hazlo **siempre** antes de crear una rama nueva. Si no, partes de una versión vieja y después tendrás conflictos.

### Paso 2. Crear tu rama de trabajo

```powershell
git checkout -b feat/nombre-de-tu-tarea
```

Verifica en qué rama estás:

```powershell
git branch --show-current
```

### Paso 3. Trabajar y probar en local

Levanta el entorno (si no está corriendo):

```powershell
docker compose up -d
docker compose ps
```

Mientras trabajas:
- Los cambios en `backend/` y `frontend-app/` se reflejan solos por los volúmenes montados.
- Para ver logs: `docker compose logs -f backend frontend keycloak`
- Si cambias **modelos de Django**, genera la migración (ver sección 4).
- Si agregas una **dependencia**, ver sección 5.
- **Escribe pruebas unitarias** para lo que agregues. Una funcionalidad nueva sin pruebas es un criterio de la rúbrica en riesgo.

### Paso 4. Ejecutar las mismas verificaciones que hará el CI, antes de subir

Si algo falla aquí, también fallará en GitHub. Corregirlo en local ahorra tiempo.

**Si tocaste el backend:**

```powershell
docker compose exec backend python manage.py check
docker compose exec backend python manage.py makemigrations --check --dry-run
docker compose exec backend python manage.py test
```

Resultado esperado: `System check identified no issues`, `No changes detected` y `OK` al final de las pruebas.

**Si tocaste el frontend:**

```powershell
docker compose exec frontend npm run build -- --configuration production
docker compose exec frontend npm test -- --watch=false
```

Resultado esperado: el build termina con `Application bundle generation complete` y las pruebas terminan con todos los archivos en `passed`. Las advertencias `WARNING` de tamaño de bundle **no son errores**. La prueba de `registro-paciente` es lenta (cerca de 45 segundos); es normal.

**Si tocaste Docker o `docker-compose.yml`:**

```powershell
docker compose config --quiet
docker compose build
```

### Paso 5. Revisar qué vas a subir

```powershell
git status
git diff
```

Comprueba que:
- Solo aparecen los archivos que tú modificaste.
- **No** aparece `.env`, contraseñas, llaves, tokens ni certificados. Si aparece alguno, **no lo agregues**.
- No hay archivos de dependencias (`node_modules`, `__pycache__`, `dist`). Ya están ignorados; si aparecen, avisa al equipo.

### Paso 6. Hacer commit

Agrega solo lo que corresponde. Evita `git add .` si no revisaste antes con `git status`.

```powershell
git add ruta/del/archivo1 ruta/del/archivo2
git commit -m "feat: agrega reporte mensual de citas por medico"
```

**Formato del mensaje:** `tipo: descripción corta en minúsculas y en presente`

| Tipo | Uso |
|---|---|
| `feat` | Funcionalidad nueva |
| `fix` | Corrección de un error |
| `test` | Pruebas |
| `docs` | Documentación |
| `ci` | Pipeline, Docker, configuración |
| `refactor` | Reorganización sin cambiar comportamiento |

Buenos ejemplos:
- `feat: valida cancelacion con 48 horas de anticipacion`
- `fix: corrige filtro de fecha en la agenda`
- `test: agrega pruebas de la politica de cancelacion`

Malos ejemplos: `cambios`, `arreglos varios`, `asdf`, `update`.

Haz commits **pequeños y frecuentes** (uno por idea). Esto facilita las revisiones y deja evidencia clara de lo que hizo cada integrante.

### Paso 7. Subir tu rama a GitHub

La primera vez que subes la rama:

```powershell
git push -u origin feat/nombre-de-tu-tarea
```

Las siguientes veces basta con:

```powershell
git push
```

> **Nunca** hagas `git push origin develop` ni `git push origin main`. Estarán bloqueados, y no se debe intentar saltarse la regla.
> **Nunca** uses `git push --force` en `develop` ni `main`.

### Paso 8. Abrir el Pull Request

1. Entra al repositorio en GitHub. Verás un botón amarillo **"Compare & pull request"**. Si no aparece, ve a la pestaña **Pull requests** → **New pull request**.
2. Configura:
   - **base:** `develop`
   - **compare:** tu rama
3. **Título:** igual que un mensaje de commit (`feat: agrega reporte mensual de citas`).
4. **Descripción:** escribe de forma breve:
   - Qué cambia y por qué.
   - Qué historia de usuario o requisito cubre (por ejemplo RF-7).
   - Cómo probarlo.
5. En el panel derecho, en **Reviewers**, elige a un compañero.
6. Pulsa **Create pull request**.

### Paso 9. Esperar el CI

Al final del PR aparece la sección de checks.

| Símbolo | Significado |
|---|---|
| Círculo amarillo | Está corriendo (tarda de 1 a 3 minutos) |
| Check verde | Pasó |
| X roja | Falló |

**Si algo falla:**
1. Haz clic en **Details** junto al check rojo.
2. Abre el paso marcado con la X roja.
3. Lee las últimas líneas del log: ahí está el motivo.
4. Corrige en local, repite el Paso 4, haz commit y `git push`. El PR se actualiza solo y vuelve a ejecutar el CI.

No necesitas crear otro PR para corregir.

### Paso 10. Revisión y aprobación

Otro integrante revisa tu PR (ver sección 7). Si pide cambios, los haces en tu rama, haces commit y push, y le avisas.

No puedes aprobar tu propio PR. El botón **Merge pull request** se activa cuando hay **1 aprobación y 3 checks en verde**.

### Paso 11. Fusionar

Quien tenga el PR aprobado pulsa **Merge pull request**. Si el equipo no decide otra cosa, usa **Squash and merge** si aparece disponible, para que `develop` quede con un commit limpio por cambio.

Después, pulsa **Delete branch** en GitHub.

### Paso 12. Limpiar y sincronizar en local

```powershell
git checkout develop
git pull origin develop
git branch -d feat/nombre-de-tu-tarea
```

Si Git dice que la rama "no está completamente fusionada" después de un squash, es normal. Si ya confirmaste que el PR quedó fusionado, usa `git branch -D feat/nombre-de-tu-tarea`.

---

## 4. Cambios en la base de datos (migraciones Django)

Si modificas `models.py`:

```powershell
docker compose exec backend python manage.py makemigrations
docker compose exec backend python manage.py migrate
docker compose exec backend python manage.py test
```

- **Incluye los archivos de `migrations/` en tu commit.** Si no lo haces, el CI falla en el check `makemigrations --check`.
- Nunca edites ni borres una migración que ya está en `develop`. Crea una nueva.
- Si dos personas crean migraciones del mismo módulo a la vez, habrá conflicto de numeración. Avisa en el grupo antes de resolverlo.

---

## 5. Dependencias

### Backend
1. Agrega la librería a `backend/requirements.txt`.
2. Reconstruye: `docker compose up --build -d backend`
3. Ejecuta las pruebas y haz commit del archivo.

### Frontend
1. Instala con `docker compose run --rm --no-deps frontend npm install nombre-paquete`
2. Haz commit de **`package.json` y `package-lock.json` juntos**.
3. Los demás actualizan con `docker compose run --rm --no-deps frontend npm ci` y `docker compose restart frontend`.

El CI usa `npm ci`: si `package.json` y `package-lock.json` no coinciden, falla.

---

## 6. Si `develop` avanzó mientras trabajabas (conflictos)

Antes de pedir revisión, o si GitHub muestra "This branch has conflicts", trae los cambios nuevos a tu rama:

```powershell
git checkout develop
git pull origin develop
git checkout feat/nombre-de-tu-tarea
git merge develop
```

Si hay conflictos:
1. Git lista los archivos en conflicto. Ábrelos en VS Code.
2. VS Code muestra las opciones **Accept Current Change**, **Accept Incoming Change** y **Accept Both Changes**. Elige según corresponda, o edita a mano.
3. Elimina las marcas `<<<<<<<`, `=======` y `>>>>>>>`.
4. Ejecuta las pruebas (Paso 4).
5. Finaliza:

```powershell
git add .
git commit -m "fix: resuelve conflictos con develop"
git push
```

Si no entiendes un conflicto, **no adivines**: pregúntale a quien escribió ese código.

---

## 7. Cómo revisar el PR de un compañero

1. Abre el PR → pestaña **Files changed**.
2. Revisa:
   - ¿Hace lo que dice la descripción?
   - ¿Incluye pruebas?
   - ¿Hay contraseñas, llaves o datos reales? (Si las hay, **no apruebes** y avisa de inmediato.)
   - ¿Respeta la arquitectura? (lógica de negocio en el backend, no solo en el botón de Angular.)
   - ¿Nombres claros y sin código comentado sobrante?
3. Para comentar una línea, pasa el cursor sobre ella y pulsa el signo `+` azul.
4. Pulsa **Review changes** y elige:
   - **Approve**: todo bien.
   - **Request changes**: falta algo; explica qué.
   - **Comment**: solo comentarios.
5. Pulsa **Submit review**.

Intenta revisar los PRs pendientes en menos de un día. Un PR parado frena a todos.

---

## 8. Reglas de seguridad (obligatorias)

- **Nunca** subas `.env`, contraseñas, tokens, llaves privadas ni certificados. Están en `.gitignore`; no intentes saltártelo.
- Si compartes una configuración nueva, actualiza `.env.example` con **valores de ejemplo**, nunca reales.
- Las cuentas demo (`Piedrazul123!`) son **solo para local**.
- Si subiste un secreto por error, **avisa de inmediato al equipo**. Borrarlo en un commit nuevo no lo elimina del historial; hay que cambiar esa credencial.
- No pegues tokens ni contraseñas en el chat del grupo, en issues ni en descripciones de PR.

---

## 9. Cuidado con Docker y los datos locales

- `docker compose down` detiene los servicios **sin borrar** datos.
- `docker compose down -v` **borra permanentemente** la base de datos local. No lo uses como cierre habitual.
- Si un puerto está ocupado (`4200`, `8000`, `8080`, `5432`), detén el servicio local que lo usa antes de levantar Compose.

---

## 10. Problemas frecuentes

| Problema | Causa probable | Qué hacer |
|---|---|---|
| `git push` dice `permission denied` o `403` | No eres Collaborator del repositorio | Pide a Jonax74 que te invite y acepta la invitación por correo |
| `git push` a `develop` es rechazado | La rama está protegida | Es lo esperado: crea una rama y abre un PR |
| El PR dice **"Review required"** | Falta la aprobación de otra persona | Pide a un compañero que lo revise |
| El botón Merge está gris y hay un check rojo | El CI falló | Abre el check, lee el log, corrige y haz push |
| Falla `Backend (Django)` en `makemigrations --check` | Cambiaste modelos sin generar o subir migraciones | Ejecuta `makemigrations` y haz commit de los archivos |
| Falla `Frontend (Angular)` en `npm ci` | `package.json` y `package-lock.json` desincronizados | Vuelve a instalar y sube ambos archivos |
| Falla en CI pero pasa en tu computador | Diferencia de entorno o rama desactualizada | Haz `git merge develop`, vuelve a probar y revisa el log completo |
| El CI no aparece en tu PR | Tu rama no incluye `.github/workflows/ci.yml` | Fusiona `develop` en tu rama |
| `git pull` dice que hay cambios locales sin guardar | Tienes cambios sin commit | `git stash`, luego `git pull`, luego `git stash pop` |
| Hiciste commit en `develop` por error | Olvidaste crear tu rama | Pide ayuda antes de hacer cualquier otro comando |

---

## 11. Lista rápida (para imprimir)

```text
1. git checkout develop; git pull origin develop
2. git checkout -b tipo/descripcion
3. Trabajar + escribir pruebas
4. Correr check, makemigrations --check, test (backend) / build + test (frontend)
5. git status; git diff   (sin secretos)
6. git add <archivos>; git commit -m "tipo: descripcion"
7. git push -u origin tipo/descripcion
8. Abrir PR hacia develop, con descripcion y revisor
9. CI en verde (si falla: leer log, corregir, push)
10. Otra persona aprueba
11. Merge + Delete branch
12. git checkout develop; git pull origin develop
```

---

## 12. Lo que viene

Esta guía cubre la **integración continua**. Más adelante se sumarán:
- Fusiones de `develop` a `main` por PR, que dispararán la construcción de imágenes.
- Despliegue en la nube y verificaciones posteriores.

Cuando eso ocurra, esta guía se actualizará. Cualquier cambio a este documento se hace también por PR.
