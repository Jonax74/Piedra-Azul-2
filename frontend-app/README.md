# Frontend Angular

Este frontend forma parte del entorno local de Piedra Azul y se ejecuta en el
servicio `frontend` de Docker Compose. Para configurar y levantar el proyecto
completo, sigue el [README de la raíz](../README.MD). No necesitas instalar
Node.js ni Angular CLI en el equipo anfitrión.

Desde la raíz del repositorio:

```powershell
docker compose up --build -d
docker compose logs -f frontend
```

La aplicación queda disponible en <http://localhost:4200>. El contenedor monta
el código de `frontend-app/` para recargar los cambios durante el desarrollo.

## Comandos de Angular

Con los servicios iniciados, ejecuta las tareas dentro del contenedor:

```powershell
docker compose exec frontend npx ng generate component nombre-componente
docker compose exec frontend npm run build
docker compose exec frontend npm test -- --watch=false
```

Angular CLI no incluye un framework de pruebas end-to-end por defecto. Si se
incorpora uno, documenta su configuración y comando aquí.

Si cambias `package.json` o `package-lock.json`, actualiza las dependencias del
volumen de Node.js desde la raíz y reinicia Angular:

```powershell
docker compose run --rm --no-deps frontend npm ci
docker compose restart frontend
```

Para más información sobre Angular, consulta la
[documentación de Angular CLI](https://angular.dev/tools/cli).
