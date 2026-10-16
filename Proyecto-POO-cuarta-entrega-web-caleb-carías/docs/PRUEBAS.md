# Verificación del prototipo

## Comprobaciones automáticas

Se compilaron todas las clases con `javac -encoding UTF-8 -Xlint:all`, usando el JDK 25.0.3 disponible. La compilación terminó sin errores ni advertencias. El código no usa características modernas específicas de esa versión.

`PruebasSistema` contiene **91 comprobaciones**, ejecutadas sin JUnit ni otras dependencias. Si alguna falla, se lanza una excepción con el motivo; el resultado satisfactorio termina con:

```text
CORRECTO: 91 comprobaciones superadas.
```

| Grupo | Comportamientos comprobados |
| --- | --- |
| Flujo de beca | Oferta, precio cero, solicitud, reserva, pregunta, aceptación, historial, progreso y acreditación |
| Estados | Aceptación y finalización única, cancelación válida, rechazo al reabrir o cancelar una sesión terminada |
| Seguimiento | Temas, notas y próximo tema; un dato inválido no deja la sesión parcialmente finalizada |
| Remuneración | Q60 por 1.5 horas a Q40/h; sin pago anticipado ni doble pago; sin horas beca para tutor remunerado |
| Cancelaciones | Liberar bloque, volver a reservar, no reutilizar IDs, no finalizar una sesión cancelada |
| Cruces | Impedir dos reservas del tutor, cruces del estudiante entre tutores y cruces entre dos perfiles de una misma persona |
| Pertenencia | Otro tutor no acepta ni finaliza; otro estudiante no cancela ni pregunta sobre la solicitud |
| Aprobación | El curso debe estar autorizado; no se admiten horarios ajenos al tutor; autorización repetida no duplica relaciones |
| Profesor | Preferir tutor par, recurrir al profesor sin bloques de pares, registrar tutoría del profesor y agotar candidatos |
| Registros | Rechazar duplicados de curso y carné dentro del mismo perfil |
| Fechas y horas | Años bisiestos, días inexistentes, formato, intervalos invertidos, duración cero, bloques contiguos y superpuestos |
| Entrada | Recuperarse de texto en campos numéricos, valores negativos, `NaN`, respuestas inválidas y texto vacío |
| Encapsulamiento | Vaciar la copia de una lista no elimina los registros internos |

## Recorridos completos ejecutados por consola

Además de las 91 comprobaciones, se ejecutaron dos secuencias de entradas sobre `Main` y se verificó su salida. Ambas llegaron a `Programa finalizado.` sin errores de operación ni agotamiento inesperado de la entrada:

1. **Tres perfiles:** Ana solicita POO y registra una pregunta; María acepta, consulta la pregunta y finaliza; se comprueba una hora beca; Ana revisa el historial y el coordinador consulta progreso, demanda e historial general.
2. **Registros nuevos:** el coordinador registra un curso, un tutor y una estudiante, y aprueba al tutor; el tutor agrega un horario; la estudiante encuentra la oferta y crea una solicitud con esos registros nuevos.

## Pruebas manuales recomendadas para la presentación

1. Siga la demostración de `README.md` y compruebe que los tres perfiles ven el mismo estado.
2. Solicite una tutoría y, desde otro estudiante, compruebe que ese horario ya no aparece disponible. Cancele la primera y compruebe que vuelve a aparecer.
3. Registre un tutor nuevo desde coordinación. Confirme que no aparece en la oferta hasta aprobarlo para un curso y agregarle un horario desde su perfil.
4. Pruebe fecha `2026-02-29`, hora `25:00`, horario final anterior al inicial y entrada `hola` en un menú. Debe mostrarse un mensaje y permitir continuar.
5. Finalice una sesión remunerada de 90 minutos. Compruebe el monto y que no pueda volver a finalizarla.
6. Cierre y vuelva a abrir. Deben reaparecer solo los datos iniciales, conforme al alcance en memoria.

Estas pruebas locales no equivalen a una validación con usuarios finales. Los comentarios de usuarios para esta tercera entrega deben recogerse y registrarse realmente.
