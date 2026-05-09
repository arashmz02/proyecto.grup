2.1 Convenciones de Git
2.1.1 Estructura de ramas
1.	main: contiene únicamente la versión estable y entregable del proyecto. No se hace push directo a main bajo ninguna circunstancia.
2.	develop: rama de integración. Todo código del Sprint 2 debe estar consolidado aquí antes del cierre del sprint.
3.	feature/issue-N-descripcion: cada integrante crea ramas individuales por Issue, partiendo siempre de develop. Ejemplo: feature/issue-9-asambleistas.

2.1.2 Notación obligatoria de commits
Cada commit debe seguir la estructura Conventional Commits. Los prefijos permitidos son los siguientes:
•	db(modulo): descripción — para cambios en scripts SQL o estructura de base de datos.
•	feat(modulo): descripción — para nuevas funcionalidades o componentes.
•	fix(modulo): descripción — para correcciones de errores en código existente.
•	docs(modulo): descripción — para cambios en documentación, README o diccionarios.
Ejemplos correctos
db(normativa): crear tabla recursiva elemento_normativo con fechas de vigencia
feat(asambleistas): implementar formulario de registro con validacion de cedula
fix(folios): corregir bloqueo de fila en obtenerSiguienteFolio
Ejemplos incorrectos (serán penalizados)
cambios
subiendo archivo
.

2.1.3 Pull Requests obligatorios
4.	Antes de fusionar trabajo a develop, el integrante abre un Pull Request describiendo qué incluye su rama.
5.	El PR debe incluir en su descripción la línea Closes #N donde N es el número del Issue resuelto. Esto cierra automáticamente el Issue al fusionar.
6.	Otro compañero del equipo (asignado como revisor) debe aprobar el PR antes del merge. Nadie aprueba sus propios PRs.
7.	Una vez aprobado, el autor del PR realiza el merge a develop usando la opción Squash and merge para mantener limpio el historial.

2.1.4 Pareo de revisores
•	Los Pull Requests de Frank los revisa Josué.
•	Los Pull Requests de Josué los revisa Arash.
•	Los Pull Requests de Arash los revisa Frank.
Esta rotación garantiza que ningún integrante se aísle en su propio dominio y que todos comprendan el trabajo del equipo.
