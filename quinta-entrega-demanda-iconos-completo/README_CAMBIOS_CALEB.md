# Gestión del coordinador — Caleb

Cambios realizados sobre la aplicación web de la rama
`quinta-entrega-demanda-iconos-completo` revisada en el ZIP.
La aplicación está en `quinta-entrega-demanda-iconos-completo/` dentro del repositorio.
La copia local de trabajo es `C:\Users\Owner\Proyecto-POO-Caleb-Correcto`,
en la rama `quinta-entrega-coordinador-Caleb`.

## Uso

- **Cursos → Eliminar curso:** pide confirmación y retira el curso de la oferta.
- **Tutores → Editar datos y pago:** permite cambiar nombre, correo, año,
  horas beca y tarifa. El carné permanece fijo porque identifica la cuenta y
  sus relaciones. Marcar horas beca coloca la tarifa en cero; desmarcar permite
  indicar la tarifa por hora. Se conserva el pago gratuito con tarifa cero
  admitido por la versión anterior.
- **Tutores → Quitar tutor/profesor:** pide confirmación y retira su perfil de la
  oferta. Un tutor retirado no puede seguir usando su acceso como tutor.
- **Personal retirado / Cursos retirados:** permiten consultar los registros
  que dejaron de ofrecerse y conservar sus referencias históricas.

La eliminación es lógica: se conserva el registro para no destruir sesiones,
solicitudes, demanda ni compensación. Su identificador no se reutiliza.
Si hay solicitudes pendientes o confirmadas, se debe cancelarlas o finalizarlas
mediante sus flujos existentes antes de retirar el curso o el personal.

En este proyecto hay un solo `ProfesorCoordinador` con un `perfilTutor`.
Editar ese perfil sincroniza su nombre y correo con coordinación. Retirarlo
deshabilita su actividad docente y mantiene su acceso administrativo. No se
agregó un nuevo rol ni un registro de profesores independientes.

## Compensación e historial

Cada solicitud conserva el precio y la modalidad de compensación pactados al
reservar. Cambiar el perfil afecta las solicitudes nuevas. Una reserva anterior
puede finalizar después de cambiar el perfil sin convertirse de pago a beca ni
de beca a pago. Ambos acumulados se reconstruyen correctamente al reiniciar.

`sql/schema.sql` agrega columnas mediante migraciones que conservan las tablas y
los datos. Las solicitudes anteriores obtienen su modalidad del perfil que
existía antes de esta actualización, que todavía no permitía editarla.
El paquete no incluye bases de datos ni modifica las imágenes, los estilos o
los documentos y evidencias de la entrega original.

## Verificación

Verificado con Java 17 y H2 incluido en la entrega:

- Compilación de todos los archivos Java con `--release 17 -Xlint:all`.
- Pruebas originales: 91 del modelo, 115 de HTTP y persistencia, 39 de demanda,
  iconos y migración.
- 64 comprobaciones nuevas de gestión del coordinador: permisos, edición
  atómica, cambio de compensación en ambos sentidos, reservas abiertas,
  historia, retiro, sesiones anteriores, profesor y migración de bases previas.
- 13 pruebas del HTML generado (6 anteriores y 7 nuevas).

Ejecutar en PowerShell desde esta carpeta:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/probar.ps1
```

Con Node.js, las pruebas del HTML no requieren instalar paquetes:

```powershell
npm run test:ui
```

No se ejecutó una revisión visual con navegador automatizado porque este
entorno no dispone del ejecutable de Chromium. Tampoco se verificó contra un
servidor PostgreSQL. La revisión visual final se hace en la computadora de Caleb.

## Demostración sugerida

1. Iniciar con `Iniciar web.cmd`, entrar como coordinador y crear un curso de prueba.
2. Eliminar ese curso: confirmar que desaparece de la oferta y aparece en retirados.
3. Editar María de horas beca a pago, guardar, recargar y comprobar la tarifa.
4. Cambiarla nuevamente a horas beca y comprobar tarifa cero.
5. Quitar un tutor sin solicitudes abiertas y comprobar que deja de ofrecerse.
6. Detener y reiniciar el servidor: comprobar que los cambios se mantienen.

El repositorio contiene archivos compilados y una base de datos que pueden
cambiar al ejecutar la aplicación. Al preparar el commit, seleccionar los
archivos fuente modificados; no agregar nuevas bases de datos ni resultados
temporales de pruebas.
