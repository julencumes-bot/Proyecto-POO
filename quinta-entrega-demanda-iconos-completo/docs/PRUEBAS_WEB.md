# Verificación de demanda e iconos — 31 de octubre de 2026

Se compiló la aplicación web con Java 17 (`--release 17`, `-Xlint:all`), sin advertencias. Las bases de prueba H2 son temporales e independientes de los datos del usuario.

| Suite | Resultado de esta actualización |
| --- | --- |
| PruebasSistema | 91 comprobaciones correctas |
| PruebasWeb | 115 comprobaciones correctas |
| PruebasCoordinacion | 39 comprobaciones correctas |
| npm run test:ui | 6 pruebas del HTML generado correctas |
| node --check | Sintaxis correcta para app.js y navegador.cjs |
| Playwright / Chromium | Pendiente de ejecutar: descarga de Chromium bloqueada en el entorno |

Se verificaron conteos por curso y tema, exclusión de canceladas, cursos sin solicitudes, temas que solo difieren en mayúsculas, permisos por rol, selección y validación de iconos, persistencia después de reiniciar y migración repetible del esquema anterior conservando registros.

El recorrido `tests/web/navegador.cjs` está actualizado para revisar selección visual, tabla, recarga, vista del estudiante y adaptación a 390 px. No se afirman resultados visuales de esta actualización. PostgreSQL no se probó. Las capturas y documentos de `entrega/` se conservan como antecedentes.

Repetir Java: `powershell -ExecutionPolicy Bypass -File scripts/probar.ps1`. Repetir HTML: `npm run test:ui`. Repetir navegador, después de compilar: `npm install`, `npx playwright install chromium`, `npm run test:web`.

---

## Antecedente conservado de la entrega original

# Verificación de la cuarta entrega

Fecha de ejecución: 12 de octubre de 2026, Guatemala. Entorno: Windows, JDK 25.0.3; código requiere JDK 17 o superior. Base H2 2.5.252 en archivos temporales. Navegador Microsoft Edge mediante Playwright.

| Suite | Resultado observado | Alcance |
| --- | --- | --- |
| PruebasSistema | 91 comprobaciones superadas | Modelo, controladores y consola |
| PruebasWeb | 65 comprobaciones superadas | HTTP, permisos, CSRF, SQL, reinicio y concurrencia |
| tests/web/navegador.cjs | 27 comprobaciones; 0 errores JavaScript | Flujos de los tres perfiles, celular y recuperación de conflicto |

Total: **183 comprobaciones satisfactorias**. No equivale a 183 requisitos distintos ni sustituye la validación con personas.

## Reproducción

- `powershell -ExecutionPolicy Bypass -File scripts/probar.ps1`
- Con Node y Playwright instalados: `node tests/web/navegador.cjs`.
- La prueba de navegador crea su propio servidor en un puerto libre, una base temporal y contextos separados para cada persona. No usa la base de la demostración habitual.

## Evidencias

`entrega/evidencias/resultado.json` contiene el resultado del navegador. Las capturas numeradas muestran acceso, estudiante, reserva, compensación, coordinación y vistas móviles. En la reserva aparece intencionalmente texto con una etiqueta HTML para comprobar que se presenta como texto y no ejecuta código.

## Casos comprobados

Oferta y precio; solicitud pendiente; privacidad entre estudiantes y tutores; aceptación única; registro de preguntas; rechazo de notas vacías sin guardar parcialmente; finalización única; historial y próximo tema; Q60 por 90 minutos; horas beca; cancelar y volver a reservar; conflicto de dos solicitudes simultáneas; altas y aprobaciones; fechas inválidas y horarios superpuestos; profesor como tutor; rechazo de una revisión SQL obsoleta; cambio de contraseña; logout; recuperación de datos y contraseña después de reiniciar.

En navegador también se comprobó filtro de cursos, demanda, estudiantes atendidos, aprobación, ayuda para último año, confirmación de cancelación, etiquetas de formularios y ausencia de desbordamiento a 390 px. Cuando un horario es reservado desde otro navegador, el formulario conserva el tema y retira la opción ocupada.

## Correcciones realizadas durante la revisión

Se eliminó un doble render al guardar y navegar que podía cerrar un formulario recién abierto. Se actualiza la oferta tras un conflicto sin borrar el tema. Al expirar la sesión se descarta la vista autenticada anterior. La prueba de demanda se corrigió para comparar sin distinguir mayúsculas, acorde a la agrupación del modelo.

## Límites de la evidencia

No se probaron PostgreSQL, despliegue público ni carga masiva. La aplicación escucha en localhost. El requisito de interacción con más de cinco usuarios sigue pendiente de grabación real; las cuentas y pruebas automatizadas no cuentan como personas entrevistadas.
