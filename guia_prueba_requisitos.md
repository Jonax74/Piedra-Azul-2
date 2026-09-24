# Guía de prueba de requisitos funcionales

## Preparar el entorno

1. Levantar PostgreSQL y Keycloak:

```powershell
docker compose up -d
```

2. Cargar o actualizar los datos demo:

```powershell
Get-Content -Raw docker\postgres\seed-demo.sql |
docker exec -i piedra-azul-postgres psql `
  -v ON_ERROR_STOP=1 `
  -U piedra_admin `
  -d piedra_db
```

3. Levantar Django:

```powershell
Set-Location backend
.\venv\Scripts\python.exe manage.py runserver 127.0.0.1:8000
```

4. En otra terminal, levantar Angular:

```powershell
Set-Location frontend-app
cmd /C "npm run start"
```

## Requisito 1: agenda de un médico por fecha

- Rol: `AGENDADOR`.
- Ruta: `/agenda`.
- Médico demo: médico con ID `2`, Valentina Pabón.
- Fecha con citas: `2026-09-28`.
- Resultado esperado: aparecen las citas del día y el contador muestra `7 citas`.

Selecciona el médico, escribe `2026-09-28` y pulsa **Buscar agenda**. El sistema muestra un mensaje indicando cuántas citas encontró.

## Requisito 2: agendar una cita

- Rol: `PACIENTE`.
- Ruta: `/agendar-cita`.
- Paciente demo: Laura Gómez.
- Médico: Valentina Pabón, ID `2`.
- Fecha con disponibilidad: `2026-09-28`.
- Horario: lunes de `08:00` a `17:00`, intervalo de `30` minutos.

Selecciona paciente, profesional y fecha. Pulsa **Consultar disponibilidad**, elige una franja y pulsa **Confirmar cita**. El mensaje esperado es similar a: `Cita agendada correctamente para el 2026-09-28 a las ...`.

Para probar el terapista:

- Terapista demo: Carlos Mora, ID `8`.
- Fecha: `2026-09-29`.
- Horario: martes de `09:00` a `16:00`, intervalo de `45` minutos.

## Requisito 3: configuración del sistema

- Rol: `ADMIN`.
- Rutas: `/configuracion` y `/disponibilidad`.
- Ventana configurada: `4 semanas`.
- Horarios configurados:
  - Médico `2`: lunes `08:00-17:00`, cada `30` minutos.
  - Médico `2`: miércoles `08:00-12:00`, cada `30` minutos.
  - Terapista `8`: martes `09:00-16:00`, cada `45` minutos.

En **Configuración**, cambia las semanas y pulsa **Guardar cambios**. En **Disponibilidad**, crea un horario seleccionando profesional, día, horas e intervalo. El sistema confirma si el horario se creó y se asoció correctamente.

## Mensajes esperados

- Carga de reserva: `Cargando pacientes, profesionales y especialidades...`.
- Franjas encontradas: `Hay N horarios disponibles para el ...`.
- Sin franjas: `No hay horarios disponibles para el ...`.
- Reserva exitosa: `Cita agendada correctamente para el ...`.
- Agenda: `Se encontraron N citas para el ...`.
- Configuración: `Configuración guardada correctamente.`.
