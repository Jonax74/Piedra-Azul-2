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
