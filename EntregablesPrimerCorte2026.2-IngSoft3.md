Universidad del Cauca Facultad de Ingeniería Electrónica y Telecomunicaciones Programa de ingeniería de sistemas Ingeniería de Software II Grupos A y B Laboratorio de Ingeniería de Software II Grupos A, B y C Francisco Obando, Daniel Paz, Juan Carlos Narvaez, . W. Libardo Pantoja Y 2026.2 

# **Ingeniería de Software III** 

# **Teoría y Laboratorio** 

# **Entregables Proyecto de Curso - Primer Corte 2026.1** 

_Fecha creación: 23 Feb 2026 Última actualización: 8 Agosto 2026 4:22 PM_ 

## **Entregables** 

En grupos de 3 a 5 estudiantes, hacer una implementación  del proyecto de curso de Reserva de Citas médicas. La aplicación **frontend** debe ser una **SPA** y se deben utilizar principios de diseño, patrones de diseño y de arquitectura de manera adecuada. Se puden utilizar tecnologías front modernas como Angular, React, Vue, etc. Las tecnologías son decisiones de diseño de cada grupo. Para el **backend** se hará una refactorización del proyecto 2026.1 de tal forma que se diseñará un **monolito-modular** . Se pueden utilizar tecnologías modernas como Spring Boot, NestJS, etc. Para la persistencia de datos se pueden utilizar bases de datos SQL y/o NoSQL como MongoDB. 

A continuación, los requisitos funcionales que se deben entregar para el primer corte. 

## **Requisitos funcionales** 

Se deben implementar los siguientes requisitos funcionales de alto valor para el cliente: 

1. **Yo como** agendador de citas **necesito** listar las citas médicas de un determinado médico/terapista en una fecha determinada **para** visualizar el listado y la cantidad de citas. _Contexto_ : Se sugiere diseñar un sistema de búsqueda con filtros y resultados en una tabla. 

2. **Yo como** paciente **necesito** agendar una cita mediante la web **para** tener una cita de manera sencilla y rápida sin tener que usar WhatsApp. _Contexto_ : El paciente debe tener un registro de usuario para poder agendar una cita. El sistema debe brindar un mecanismo para hacer la cita de manera segura, usable y eficiente, mostrando las franjas disponibles para cada médico. 

3. **Yo como** administrador **necesito** configurar los parámetros del sistema **para** que el agendamiento de citas autónomo funcione acorde a la disponibilidad de los médicos y terapistas de Piedrazul. _Contexto_ : Se debe configurar la ventana de tiempo que se habilitarán las citas (en semanas), los días de la semana que cada médico/terapista atiende, la franja horaria de cada médico/terapista, el intervalo de tiempo (minutos) que cada médico/terapista tiene entre cita y cita. 

## **Rúbrica de evaluación** 

|**Criterio**|**Excelente (5 pts)**|**Bueno (4 pts)**|**Aceptable (3**<br>**pts)**|**Insuficiente (0,**<br>**1 pts)**|<br>**Peso**<br>**(%)**|
|---|---|---|---|---|---|
|Cumplimiento de<br>los requisitos<br>funcionales|Implementa<br>completamente<br>los requisitos<br>funcionales del<br>primer corte|Los requisitos se<br>implementan<br>con pequeñas<br>limitaciones o<br>errores.|Los requisitos<br>se<br>implementan<br>parcialmente o<br>con errores<br>funcionales<br>importantes.|Los requisitos<br>no se<br>implementan<br>o no cumplen<br>las<br>necesidades<br>del cliente.|30%|
|Cumplimiento de<br>los requisitos NO<br>funcionales de<br>modificabilidad|Se aplican<br>correctamente los<br>principios de<br>diseño SOLID,<br>patrones de diseño<br>y de arquitectura<br>de software.|Se aplican los<br>principios<br>SOLID y<br>patrones de<br>diseño con<br>pequeñas<br>limitaciones o<br>errores|Se aplican los<br>principios<br>SOLID y<br>patrones de<br>diseño con<br>limitaciones o<br>errores<br>importantes|No se aplican<br>los principios<br>SOLID y<br>patrones de<br>diseño.|30%|
|Documentación<br>de arquitectura|Documento claro,<br>estructurado, con<br>diagramas y<br>detalles técnicos<br>relevantes.<br>Repositorio Git<br>bien organizado y<br>documentado|Documento y<br>repositorio<br>adecuados, pero<br>con  pequeños<br>aspectos<br>mejorables.|Documento y<br>repositorio,<br>con  aspectos<br>importantes a<br>mejorar.|No hay<br>documento de<br>la arquitectura<br>y repositorio<br>Git.|20%|
|Pruebas unitarias<br>automatizadas|Hay pruebas<br>unitarias  a todas<br>las clases del dominio (entidades y servicios) correctamente implementadas|Hay pruebas<br>unitarias con<br>pequeñas limitaciones o errores |Hay pruebas<br>unitarias con<br>limitaciones o errores importantes|No hay<br>pruebas<br>unitarias|20%|


### **PROTOCOLO DE SUSTENTACIÓN** 

Para la sustentación del primer entregable del proyecto de curso, cada equipo debe elaborar un video en **Youtube** . Importante que **todos los integrantes** del equipo participen en la sustentación. A continuación, el protocolo de sustentación: 

1. Mostrar las historias de usuario con sus criterios de aceptación, prototipos de la interfaz gráfica de usuario y test de usabilidad: **2 minutos** 

2. Mostrar los atributos de calidad relevantes para la iteración: **1 minuto** 3. Mostrar la arquitectura y diseño de software (usar el modelo C4 y 4+1 vistas - UML): **2 minutos** 

4. Mostrar las pruebas unitarias automatizadas: **1 minuto** 

5. Mostrar el repositorio git con los commits de todos los integrantes del equipo y el tablero de tareas del primer sprint Scrum: **1 minuto** 

6. Mostrar el software funcional y algunos aspectos claves de la codificación que evidencien la implementación de la arquitectura: **5 minutos** 

**NOTA:** Los grupos que se pasen de estos tiempos (o aceleren el video), serán penalizados con -1 punto. 

### **DOCUMENTO DE ARQUITECTURA** 

El documento de arquitectura debe tener estas partes: 

- Portada 

- Introducción breve al documento 

- Historias de usuario (con criterios de aceptación) implementadas en la primera iteración 

- Prototipos de la interfaz de usuario y un test de evaluación de usabilidad 

- Pantallazo de planificación de tareas del Sprint 1: Jira, Trello. 

- Escenarios de calidad de los dos atributos de calidad prioritarios (usabilidad y seguridad): contexto, estímulo, respuesta, medición de calidad, resultado esperado. 

- Arquitectura y diseño de software usando el modelo C4. 

- Listado de patrones de diseño implementados ya sea en el frontend o backend y una breve descripción de cómo y en qué contexto del problema se aplicaron. 

- URL del video de YouTube 

- URL del repositorio GIT 
