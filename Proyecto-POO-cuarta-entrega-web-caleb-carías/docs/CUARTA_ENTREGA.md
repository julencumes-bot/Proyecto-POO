# Cuarta entrega del sistema web

Fecha de verificación: 12 de octubre de 2026. Entrega: viernes 16 de octubre.

33 de 33 requisitos de la matriz ampliada tienen recorrido técnico implementado y comprobado (100 %). La línea base tabular original contiene 32; RF33 recoge el caso narrativo de último año. Los tests automatizados no sustituyen las evidencias humanas.

| ID | Requisito | Estado | Recorrido verificado |
| --- | --- | --- | --- |
| RF01 | Consultar cursos | Pasa | Buscar tutoría y filtrar Programación. |
| RF02 | Consultar tutores | Pasa | Elegir María entre tutores del curso. |
| RF03 | Consultar horarios | Pasa | Elegir bloque; retirar horario ocupado tras conflicto. |
| RF04 | Consultar precio | Pasa | Resumen Q0 y Q60 antes de confirmar. |
| RF05 | Solicitar tutoría | Pasa | Confirmar y ver estado pendiente. |
| RF06 | Consultar solicitudes | Pasa | Ver cambios pendiente, confirmada, finalizada y cancelada. |
| RF07 | Consultar historial | Pasa | Consultar sesión finalizada desde Ana. |
| RF08 | Consultar temas vistos | Pasa | Historial muestra Herencia y constructores. |
| RF09 | Consultar notas | Pasa | Historial muestra observaciones de autonomía. |
| RF10 | Realizar preguntas | Pasa | Guardar pregunta y consultarla como tutor. |
| RF11 | Consultar próximo tema | Pasa | Historial muestra Polimorfismo. |
| RF12 | Consultar cursos asignados | Pasa | Disponibilidad muestra cursos del tutor. |
| RF13 | Consultar solicitudes | Pasa | María consulta solicitudes dirigidas a su cuenta. |
| RF14 | Consultar horarios solicitados | Pasa | Tarjeta muestra fecha, inicio y fin reservados. |
| RF15 | Aceptar tutoría | Pasa | Aceptar y crear sesión; rechazar segunda aceptación. |
| RF16 | Consultar tema | Pasa | Tarjeta y demanda muestran el tema solicitado. |
| RF17 | Registrar temas vistos | Pasa | Finalizar con temas; recuperar desde historial. |
| RF18 | Registrar notas | Pasa | Finalizar con notas; rechazar notas vacías. |
| RF19 | Consultar historial | Pasa | Historial del tutor después de finalizar. |
| RF20 | Consultar remuneración | Pasa | Diego acumula Q60 al finalizar. |
| RF21 | Consultar horas beca | Pasa | María acumula una hora beca sin duplicación. |
| RF22 | Consultar pago | Pasa | Compensación y detalle muestran el monto correspondiente. |
| RF23 | Consultar tutores | Pasa | Coordinación lista tutores registrados. |
| RF24 | Consultar información del tutor | Pasa | Disponibilidad, cursos e historial en ficha de tutor. |
| RF25 | Consultar estudiantes | Pasa | Filtro Con tutorías muestra estudiantes atendidos. |
| RF26 | Consultar progreso | Pasa | Progreso de Ana muestra notas y próximo tema. |
| RF27 | Consultar solicitudes y demanda | Pasa | Demanda agrupa el tema solicitado. |
| RF28 | Consultar historial | Pasa | Historial general incluye sesión del profesor. |
| RF29 | Impartir tutorías | Pasa | Profesor acepta y finaliza desde coordinación. |
| RF30 | Registrar temas vistos | Pasa | Sesión del profesor conserva Grafos dirigidos. |
| RF31 | Registrar notas | Pasa | Sesión del profesor conserva sus observaciones. |
| RF32 | Aprobar tutor | Pasa | Aprobar María para Diseño web y ver autorización. |
| RF33 | Buscar apoyo alternativo | Pasa | Ayuda para encontrar tutor; casos de fallback en modelo. |

## Arquitectura y límites

Java conserva el dominio y controladores. WebMain sirve HTML/CSS/JS y API; ServicioWeb autoriza acciones; RepositorioTutorias y BaseDatos guardan SQL; Seguridad gestiona cuentas. Ver UML_WEB.md. H2 persistente y acceso por contraseña están implementados. PostgreSQL es una opción no probada. El servidor es local y los pagos son cálculos.

## Responsabilidades previstas

Alan: oferta, solicitudes y alternativa. Julen: servidor, tutor y compensación. Joaquin: coordinación y revisión de persistencia. Caleb: interfaz, preguntas e integración. Cada integrante debe aportar contribuciones significativas propias y explicar el código; este reparto no acredita autoría.

## Cierre

Ver PRUEBAS_WEB.md para las 183 comprobaciones y ENTREGA_EQUIPO.md para video, formularios, certificados y GitHub pendientes.
