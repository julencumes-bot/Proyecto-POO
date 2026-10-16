# Tercera entrega: alcance y correspondencia

## Documentos utilizados y prioridad

1. **Guía de la Tercera Entrega del proyecto 2026.pdf**: determina el mínimo de implementación y los elementos de entrega.
2. Instrucciones del estudiante: restringen la implementación a los conceptos del curso.
3. **Segunda entrega del proyecto POO (Tutorias).pdf**, páginas 4–27: requisitos funcionales, clases, responsabilidades y UML.
4. **investigacion_tecnologias_datos_asesorias.pdf**: selección de PostgreSQL como propuesta de persistencia futura.

La guía exige al menos un requisito funcional completo por integrante. En el documento anterior figuran Alan, Julen, Oscar y Caleb: el mínimo es cuatro. La guía solicita investigar la persistencia; los pasos de conexión y GUI aparecen como ejemplo de descomposición de tareas de un login, no como requisitos funcionales obligatorios de este sistema.

## Requisitos priorizados y código

Se conservan las prioridades 1 y 2 de la segunda entrega. Cada función de esta tabla tiene acceso desde los menús; los nombres de métodos se refieren a la implementación actual.

| Perfil | Requisito | Prioridad | Implementación |
| --- | --- | --- | --- |
| Estudiante | Consultar cursos con tutorías | 1 | `EstudianteController.consultarCursos` |
| Estudiante | Consultar tutores por curso | 1 | `EstudianteController.consultarTutores` |
| Estudiante | Consultar horarios disponibles | 1 | `EstudianteController.consultarHorarios` |
| Estudiante | Solicitar tutoría | 1 | `EstudianteController.solicitarTutoria` |
| Estudiante | Consultar solicitudes y estados | 1 | `EstudianteController.consultarSolicitudes` |
| Tutor | Consultar cursos asignados | 1 | `TutorController.consultarCursosAsignados` |
| Tutor | Consultar solicitudes, horarios solicitados y tema | 1 | `TutorController.consultarSolicitudes`; información en `SolicitudTutoria` |
| Tutor | Aceptar tutoría | 1 | `TutorController.aceptarTutoria` |
| Coordinador | Consultar tutores y su información | 1 | `CoordinadorController.consultarTutores`; cursos, horarios e historial en `CoordinadorView` |
| Coordinador | Consultar estudiantes que reciben tutorías | 1 | `CoordinadorController.consultarEstudiantesConTutorias` |
| Coordinador | Consultar progreso | 1 | `CoordinadorController.consultarProgreso` |
| Coordinador | Aprobar tutor para un curso | 1 | `CoordinadorController.aprobarTutor` y `ProfesorCoordinador.aprobarTutor` |
| Estudiante | Consultar precio | 2 | `EstudianteController.consultarPrecio`, antes de confirmar la solicitud |
| Estudiante | Consultar historial | 2 | `EstudianteController.consultarHistorial` |
| Estudiante | Consultar temas vistos y notas | 2 | `EstudianteView.mostrarHistorial` y datos de `SesionTutoria` |
| Estudiante | Realizar preguntas | 2 | `EstudianteController.registrarPregunta` y `consultarPreguntas` |
| Estudiante | Consultar tema de próxima tutoría | 2 | `SesionTutoria.getTemaProximaTutoria`, visible en historial |
| Tutor | Registrar temas vistos y notas | 2 | `TutorController.finalizarSesion` y `SesionTutoria.finalizar` |
| Tutor | Consultar historial | 2 | `TutorController.consultarHistorial` |
| Tutor | Consultar remuneración y pago | 2 | `TutorController.consultarRemuneracion` y `consultarPago` |
| Tutor | Consultar horas beca | 2 | `TutorController.consultarHorasBeca` y `RegistroHorasBeca` |
| Coordinador | Consultar solicitudes y demanda por curso/tema | 2 | `CoordinadorController.consultarSolicitudes` y `consultarDemanda` |
| Coordinador | Consultar historial general | 2 | `CoordinadorController.consultarHistorial` |
| Profesor | Impartir tutorías, registrar temas y notas | 2 | Perfil `Tutor` asociado al profesor; reutiliza `TutorView` y `TutorController` |
| Estudiante de último año | Buscar apoyo de pares o profesor | Sin número en el documento | `CoordinadorController.buscarTutorAlternativo`, accesible desde `EstudianteView` |

Funciones de apoyo necesarias para utilizar el sistema: registrar estudiantes, tutores y cursos; agregar disponibilidad; cancelar solicitudes; finalizar sesiones. Las cancelaciones permiten alcanzar el estado cancelado previsto en los requisitos; las finalizaciones alimentan el historial y la compensación.

## Ajustes respecto al diseño anterior

Los cambios se reflejan en `UML.md`. Los PDF originales permanecen intactos.

| Problema del diseño anterior | Solución implementada y justificación |
| --- | --- |
| Se propone PostgreSQL con JDBC/ORM, fuera de los temas autorizados | Se utiliza `ArrayList` en memoria. La investigación se conserva como propuesta futura, sin afirmar que existe persistencia. |
| El prototipo dibujado tiene interfaz gráfica, pero no se autorizan APIs gráficas | Las tres vistas son menús de consola con `Scanner`. |
| El profesor puede impartir, pero la solicitud únicamente referencia un `Tutor` | `ProfesorCoordinador` conserva su herencia de `Usuario` y tiene un `perfilTutor`. Ese objeto participa en las mismas solicitudes y sesiones. Su historial se consulta mediante el perfil. |
| No se especifica cómo calcular precio ni distinguir horas beca de pago | `Tutor` agrega `tarifaHora` y `recibeHorasBeca`. Se fija precio al crear la solicitud. |
| La búsqueda por año no dispone del año del tutor | `Tutor` agrega `anioCarrera`. El perfil del profesor se distingue por su referencia y no por ese año. |
| Los estados son textos modificables arbitrariamente | Se usa el enum permitido `EstadoSolicitud` y métodos `confirmar`, `cancelar` y `finalizar`. |
| Las listas generales y el arranque no tienen un responsable | `SistemaTutorias` reúne los registros compartidos y `Main` crea la demostración y selecciona perfiles. |
| No se definen las dependencias de vistas y controladores | Se reciben referencias mediante constructores, igual que cualquier otro objeto. |
| El precio se consulta sin indicar duración | `consultarPrecio` recibe también el horario, para devolver el precio total. |
| Algunos métodos modifican sesiones sin indicar quién opera | Aceptar, cancelar y finalizar reciben al tutor; se comprueba que le pertenezcan los registros. |
| El historial del profesor y de su perfil podría duplicarse | `getTutoriasImpartidas` devuelve el historial del perfil, sin mantener una segunda lista. |
| Agregar horas o remuneración libremente permitiría duplicar créditos | Los créditos se obtienen de sesiones finalizadas; no hay un método para sumar montos arbitrarios. |
| Setters de IDs y horarios permitirían alterar reservas existentes | Se mantienen estables los identificadores y los datos del bloque. Los cambios de estado tienen métodos específicos. |
| La tabla de `Pregunta` la vincula con una solicitud, pero el dibujo incluye una asociación a sesión | Se sigue la tabla de atributos: una pregunta pertenece a una solicitud y puede registrarse antes de su aceptación. |

Se agregan únicamente tres apoyos de aplicación (`SistemaTutorias`, `Entrada`, `Validacion`), el enum y el punto de entrada `Main`. `Entrada` reúne la lectura y recuperación de errores; `Validacion` evita repetir la validación de texto, números, fechas y horas. Se conservan las diez clases de modelo, tres controladores y tres vistas del documento.

## Reglas y supuestos del prototipo

- **Reserva:** la solicitud pendiente ocupa el bloque completo. Se decidió así para evitar solicitudes simultáneas del mismo horario. Cancelar una solicitud pendiente o confirmada libera el bloque.
- **Estados:** `PENDIENTE → CONFIRMADA → FINALIZADA`; se permite cancelar desde pendiente o confirmada. Una solicitud cancelada o finalizada no cambia otra vez de estado.
- **Sesiones:** aceptar crea una sesión. Finalizar guarda temas, notas y próximo tema opcional, actualiza ambos historiales y acredita la compensación una sola vez.
- **Duración:** se usa la duración completa del bloque publicado. No se registran sesiones parciales ni cruces de medianoche.
- **Precio:** tarifa por hora multiplicada por duración del bloque; se muestra antes de confirmar. Los tutores de horas beca tienen tarifa cero. Los tutores sin beca pueden ser gratuitos. Los montos son una simulación con `double` acorde al curso.
- **Compensación:** se consulta el total generado por sesiones finalizadas. No se registra que el dinero ya se pagó ni se conectan bancos. El profesor de demostración tiene tarifa cero y no acumula beca. Estas reglas son supuestos del prototipo, no una declaración de políticas oficiales de la universidad.
- **Aprobación:** el coordinador autoriza a un tutor para cada curso. El sistema registra esa decisión; no verifica expedientes o notas universitarias.
- **Cruces:** se comparan fecha, inicio y fin. Dos bloques contiguos son válidos. Se verifica tanto el carné del estudiante como el del tutor, incluso cuando la misma persona tiene ambos perfiles.
- **Doble perfil:** un estudiante puede tener un registro de tutor con el mismo carné. No puede solicitarse tutoría a sí mismo. El carné no se puede modificar.
- **Fechas:** se valida el calendario, incluyendo años bisiestos, con operaciones básicas. No se compara la fecha contra el reloj del equipo; esto permite demostrar sesiones con fechas de ejemplo.
- **Búsqueda alternativa:** recomienda un tutor aprobado con algún bloque compatible. Prioriza año superior, mismo año, otro tutor autorizado y finalmente al profesor. La aprobación del curso es necesaria en todos los casos; el año no sustituye esa aprobación.
- **Historial:** solo contiene tutorías finalizadas. Una sesión confirmada se consulta entre las sesiones por realizar; si se cancela conserva su registro pero no aparece como pendiente ni genera créditos.
- **Preguntas:** se pueden agregar en solicitudes pendientes o confirmadas. Se consultan por el estudiante que las creó y el tutor correspondiente. No se añadió mensajería o respuestas, pues no aparecen en el requisito.
- **Tema próximo:** es una recomendación visible en el historial. No crea una nueva reserva automáticamente.
- **Disponibilidad por curso:** un tutor comparte los mismos bloques entre todos sus cursos; una reserva bloquea ese horario para cualquiera de ellos.
- **Privacidad:** los controladores de modificación comprueban a quién pertenece el registro; el selector de perfiles es una demostración local sin autenticación.

## Pendientes de entrega según la guía

| Elemento | Estado real |
| --- | --- |
| Prototipo funcional | Implementado por consola y probado localmente |
| Requisitos funcionales priorizados | Relacionados con el código en este documento |
| UML consistente con el código | Incluido en `UML.md` |
| Investigación de persistencia | Documento original disponible; implementación de PostgreSQL pendiente de que el curso autorice los conceptos necesarios |
| Planificación detallada y calendario | El equipo debe confirmar responsables, estimaciones y fechas reales; propuesta de trabajo posterior abajo |
| Formularios individuales de tiempo | Deben completarlos los integrantes con sus tiempos reales |
| Muestra a usuarios y comentarios | Hay antecedentes de la segunda entrega; falta mostrar esta implementación y registrar observaciones reales |
| Repositorio GitHub | Pendiente: no se proporcionó una dirección de repositorio ni se publicó código |
| Más de tres contribuciones semanales por integrante | Pendiente del trabajo real de cada integrante; no se fabrican commits o autorías |
| Word con control de cambios o Google Docs | Pendiente de integrar los resultados y evidencias del equipo al documento de entrega |

### Propuesta para distribuir la revisión y demostración

Esta tabla es una **propuesta**, no evidencia de trabajo hecho ni una asignación confirmada. Las horas son estimaciones para revisar, explicar, probar y ajustar el código que ya está disponible. No representan cuánto tardó cada persona en desarrollarlo.

| Tarea | Descripción | Responsable propuesto | Horas estimadas | Fecha probable |
| --- | --- | --- | --- | --- |
| Revisar solicitudes | Explicar y probar consulta de oferta, creación y cancelación; ajustar lo encontrado | Alan | 2 | Por acordar |
| Revisar modelo | Verificar constructores, relaciones, estados, UML y coherencia de los cambios | Julen | 2 | Por acordar |
| Revisar sesiones y compensación | Probar aceptación, seguimiento, horas beca y remuneración; ajustar lo encontrado | Oscar | 2 | Por acordar |
| Revisar vistas | Probar los menús y recuperación de errores; ajustar mensajes a partir de uso real | Caleb | 2 | Por acordar |
| Mostrar a usuarios | Cada integrante muestra su flujo, recoge observaciones y registra las mejoras | Todos | 1 por integrante | Por acordar |
| Integrar entrega | Revisar requisitos, investigación, evidencias, calendario y contribuciones | Todos | 1 por integrante | Por acordar |

El calendario debe completarse con la fecha límite del curso y la disponibilidad del equipo. Cada integrante debe comprender, revisar y contribuir al código; esta propuesta no reemplaza la obligación de programar indicada por la guía.
