## ADR: Decisiones de Despliegue

### 1. Ambiente único académico
Se utilizará un solo ambiente académico, sin entorno de staging. Esta decisión responde a las limitaciones de la capa gratuita y es la opción más sensata para el contexto del proyecto.

### 2. Estrategia de ramas y CI/CD
- Todo Pull Request hacia la rama `develop` ejecuta el pipeline completo de CI.
- El merge a la rama `main` dispara la construcción de imágenes y el despliegue automático.

### 3. Registry de imágenes
Se utilizará GitHub Container Registry (GHCR), que es gratuito con GitHub, salvo que el proveedor cloud seleccionado incluya un registry propio.

### 4. Proveedor cloud
El proveedor cloud queda como *placeholder* (“Proveedor por confirmar”) hasta verificar las cuotas y condiciones de cada opción disponible.

### 5. Topología y criterio para Keycloak
Keycloak es el servicio que más recursos consume (usualmente requiere entre 512 MB y 1 GB de RAM). Este requerimiento será el principal criterio para la selección del proveedor cloud, y debe quedar explícito en la evaluación de alternativas.
