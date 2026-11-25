Universidad del Cauca

Facultad de Ingeniería Electrónica y Telecomunicaciones

Programa de ingeniería de sistemas Ingeniería de Software II Grupos A y B

Laboratorio de Ingeniería de Software II Grupos A, B y C

Francisco Obando, Daniel Paz, Juan Carlos Narvaez,. W. Libardo Pantoja Y

2026.2

## Ingeniería de Software III

## Teoría y Laboratorio

## Entregables Proyecto de Curso - Tercer Corte 2026.2

Fecha creación: 12 Mayo 2026

Última actualización: 8 Ago 2026 4:23 PM

## Entregables

Para el tercer corte, se trabajarán requisitos funcionales y no funcionales del Sistema de Reserva de Citas de Piedrazul. Los requisitos de los cortes anteriores se pueden mejorar. Los nuevos requisitos aparecen en color azul.

## Requisitos funcionales

Se deben implementar los siguientes requisitos funcionales de alto valor para el cliente:

- 1. Yo como agendador de citas necesito listar las citas médicas de un determinado médico/terapista en una fecha determinada para visualizar el listado y la cantidad de citas. Contexto: Se sugiere diseñar un sistema de búsqueda con filtros y resultados en una tabla.

- 2. Yo como paciente necesito agendar una cita mediante la web para tener una cita de manera sencilla y rápida sin tener que usar WhatsApp. Contexto: El paciente debe tener un registro de usuario para poder agendar una cita. El sistema debe brindar un mecanismo para hacer la cita de manera segura, usable y eficiente, mostrando las franjas disponibles para cada médico.

- 3. Yo como administrador necesito configurar los parámetros del sistema para que el agendamiento de citas autónomo funcione acorde a la disponibilidad de los médicos y terapistas de Piedrazul. Contexto: Se debe configurar la ventana de tiempo que se habilitarán las citas (en semanas), los días de la semana que cada médico/terapista atiende, la franja horaria de cada médico/terapista, el intervalo de tiempo (minutos) que cada médico/terapista tiene entre cita y cita.

- 4. Yo como médico/terapista/agendador necesito exportar las citas correspondientes a una fecha específica y a un médico o terapista determinado, en un formato de texto compatible con hojas de cálculo (por ejemplo, CSV) para que durante la jornada de atención haya un listado queorganizar los pacientes y su ingreso a la consulta médica.

- 5. Yo como médico/terapista/agendador necesito consultar las citas que de un paciente a partir de su cédula para conocer el historial de citas que ha tenido.

- 6. Yo como paciente necesito cancelar una cita médica que agendé con anterioridad para liberar ese espacio a otro paciente. Nota: Se puede cancelar una cita mínimo con dos días de anticipación y el paciente debe dejar la justificación de la cancelación.

- 7. Yo como médico/terapista necesito generar un reporte gráfico de cantidad de citas por mes y médico para analizar la cantidad de citas (ver Figura 6 del documento del proyecto de curso – Classroom Contextualización del proyecto final). Nota: El sistema debe permitir elegir un año específico para procesar el reporte.

## Requisitos no funcionales y restricciones

- 1. Implementar un sistema de autenticación y autorización basado en tokens JWT. Usar un sistema externo como Keycloak. La gestión de usuarios y roles se puede hacer por Postman, no se requiere aún una interfaz gráfica en el frontend. [URL 🔗](https://www.keycloak.org/)

- 2. Testear con OWASP ZAP al menos dos vulnerabilidades de la aplicación web acorde a la especificación OWASP top 10. Generar un informe breve de las vulnerabilidades reportadas.

- 3. Hacer al menos una prueba de integración, sistema, aceptación, completando la pirámide de pruebas de software.

- 4. Implementar un esquema de CI/CD: integración continua, pruebas automáticas y despliegue continuo.

- 5. Desplegar el sistema en la nube, se puede usar o no contenedores docker, procurar usar una tecnología de no pago y que no pida tarjeta de crédito.

- 6. Opcionalmente, pueden aplicar algunos de los temas vistos en clase sobre monitoreo: estado salud del backend, login centralizado, monitoreo de logs.

## Rúbrica de evaluación

| Criterio | Excelente (5 pts) Bueno (4 pts) |   | Aceptable (3 pts) | Insuficiente (0, Peso 1 pts) | (%) |
| --- | --- | --- | --- | --- | --- |
| Cumplimiento de los requisitos funcionales | Implementa completamente los requisitos funcionales del primer corte | Los requisitos se implementan con pequeñas limitaciones o errores. | Los requisitos se implementan parcialmente o con errores funcionales importantes. | Los requisitos no se implementan o no cumplen las necesidades del cliente. | 30% |
| Cumplimiento de los requisitos NO funcionales de modificabilidad, seguridad y usabilidad | Se aplican correctamente los principios de diseño SOLID, patrones de diseño y de arquitectura de software. | Se aplican los principios SOLID y patrones de diseño con pequeñas limitaciones o errores | Se aplican los principios SOLID y patrones de diseño con limitaciones o errores importantes | No se aplican los principios SOLID y patrones de diseño. | 30% |
| Documentación de arquitectura | Documento claro, estructurado, con los puntos solicitados en la sección Documento De Arquitectura | Documento claro y estructurado, pero con pequeños aspectos mejorables. | Documento claro y estructurado, con aspectos importantes a mejorar. | No hay documento de la arquitectura y repositorio Git. | 20% |
| Sustentación | Las respuestas a las preguntas de los clientes, evidencian dominio técnico y funcional del sistema | Las respuestas tienen pequeñas limitaciones o errores | Las respuestas tienen limitaciones o errores importantes | Las respuestas fueron deficientes | 20% |

## PROTOCOLO DE SUSTENTACIÓN

Para la sustentación del tercer entregable del proyecto de curso, se hará de manera presencial. Importante que todos los integrantes del equipo participen en la sustentación. Además, se debe asumir el rol de empresarios tratando de vender el producto a los clientes (los docentes). A continuación, el protocolo de sustentación:


- 1. Mostrar el software funcional y convencer a los clientes que cumple con los requisitos de negocio de Piedrazul. Mencionar también cómo la aplicación contempla requisitos de seguridad, usabilidad y modificabilidad: 10 minutos.

- 2. Resolver dos o tres preguntas de los clientes. Los docentes eligen quienes del grupo responden cada pregunta (pueden ser preguntas técnicas). 5 minutos.

## DOCUMENTO DE ARQUITECTURA

El documento de arquitectura debe tener estas partes:

- Portada

- Introducción breve al documento

- Historias de usuario (con criterios de aceptación) implementadas en las tres iteraciones

- Prototipos de la interfaz de usuario y un test de evaluación de usabilidad

- Pantallazo de planificación de tareas de los tres sprints: Jira, Trello.

- Arquitectura y diseño de software final usando el modelo C4 y 4+1 vistas.

- Listado de patrones de diseño implementados ya sea en el frontend o backend y una breve descripción de cómo y en qué contexto del problema se aplicaron.

- Diseño del esquema CI/CD.

- Diseño de despliegue en la nube

- Esquemas de monitoreo implementados (opcional)

- URL del repositorio GIT
