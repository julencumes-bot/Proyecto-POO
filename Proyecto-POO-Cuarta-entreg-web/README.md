# Tutorías académicas — cuarta entrega

Aplicación web de POO con **Java 17 o superior**, HTML, CSS y JavaScript. El navegador se conecta a los controladores Java; las operaciones se guardan en una base SQL local H2. Incluye perfiles de estudiante, tutor y profesor/coordinador, acceso con contraseña y una interfaz adaptable a celular.

## Iniciar en Windows

1. Instala un **JDK 17 o superior** y comprueba `java -version` y `javac -version`.
2. Extrae el ZIP completo en una carpeta con permisos de escritura.
3. Abre **Iniciar web.cmd**. Mantén abierta la ventana del servidor.
4. Visita **http://127.0.0.1:8080**. No abras `web/index.html` directamente.
5. Para detenerlo, presiona Ctrl+C en la ventana del servidor.

Alternativa PowerShell: `powershell -ExecutionPolicy Bypass -File scripts/iniciar.ps1`.
Si 8080 está ocupado: `powershell -ExecutionPolicy Bypass -File scripts/iniciar.ps1 -Puerto 8081` y abre http://127.0.0.1:8081.
En Linux/macOS: `sh iniciar.sh`, con un JDK y los JAR de `lib/` disponibles.

El paquete de entrega incluye los controladores JDBC. Si se clona solo el código, `scripts/dependencias.ps1` descarga las versiones indicadas; esa primera preparación necesita internet. La aplicación no requiere internet para funcionar con H2.

## Cuentas de demostración

Contraseña inicial de todas las cuentas: **Tutor2026!**. Los botones de la pantalla de acceso completan el formulario; luego presiona Entrar a mi espacio.

| Perfil | Carné | Persona |
| --- | --- | --- |
| Estudiante | E001 | Ana López |
| Estudiante | E002 | Luis Méndez |
| Tutor | T001 | María Pérez, horas beca |
| Tutor | T002 | Diego Ruiz, Q40/h |
| Profesor o coordinador | P001 | Prof. Rivera |

Los nombres y correos son ficticios. Los horarios iniciales se crean para los cinco días siguientes a la primera ejecución. Si se usan fechas posteriores, el tutor puede publicar más horarios. Para facilitar la demostración académica, el calendario valida fechas y cruces pero no bloquea una sesión solo por estar en el pasado.

## Demostración de cinco minutos

1. Ana: **Buscar tutoría → Programación → María → horario**; escribe un tema y confirma. Consulta el precio antes de enviar.
2. Ana: **Mis solicitudes → Agregar una pregunta**.
3. Salir; María: **Solicitudes → Aceptar tutoría**. Puede leer la pregunta.
4. María: **Sesiones → Registrar seguimiento**; escribe temas, notas y próximo tema. Guarda y finaliza.
5. María: **Compensación** muestra una hora beca. Ana encuentra los mismos datos en **Historial**.
6. Coordinación: **Estudiantes → Ver progreso** y **Demanda**.
7. Para el caso remunerado, Luis solicita Matemática con Diego: 90 minutos a Q40/h = Q60. La remuneración se acredita al finalizar.
8. Para el profesor, Luis solicita Algoritmos avanzados con Prof. Rivera; coordinación acepta y finaliza desde **Impartir tutorías**.

Los estudiantes nuevos pueden abrir **¿Eres nuevo? Registrarme como estudiante** en la página inicial, completar sus datos y crear su contraseña. Después inician sesión con el perfil Estudiante y su carné. Las cuentas nuevas se guardan en la base de datos y no se agregan a las cuentas de demostración.

Para dar de alta usuarios: coordinación registra curso, tutor y estudiante, aprueba al tutor para ese curso y asigna una contraseña inicial. El tutor entra con su nueva cuenta y publica su disponibilidad. No se necesita modificar el código.

## Funciones

- Estudiante: oferta, precio, solicitudes, cancelación, historial, temas, notas, preguntas y tutor alternativo.
- Tutor: cursos, horarios, solicitudes, aceptación, seguimiento, historial, remuneración y horas beca.
- Coordinación: registros, aprobaciones por curso, estudiantes atendidos, progreso, demanda, historial e impartición de tutorías.
- Acceso por rol, cambio de contraseña, sesiones con expiración, validación de pertenencia y formularios protegidos con token CSRF.
- Persistencia transaccional, recuperación al reiniciar y prevención de doble reserva concurrente.

## Datos y configuración

H2 crea `data/tutorias.mv.db`. **Los datos web sí permanecen al reiniciar.** Para una copia de seguridad, detén el servidor y copia `data/` completa. No incluyas esta carpeta ni contraseñas en GitHub. Una nueva carpeta extraída del ZIP inicia una demostración independiente sin borrar datos de otras instalaciones.

El servidor escucha únicamente en `127.0.0.1`: es una aplicación local para presentación, no un sitio público desplegado. No procesa pagos bancarios ni certifica horas beca oficiales. La persistencia guarda una instantánea de los datos en una transacción; es apropiada para el volumen del prototipo.

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| PORT | 8080 | Puerto local |
| DB_URL | jdbc:h2:./data/tutorias;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE | Conexión JDBC |
| DB_USER | sa | Usuario SQL |
| DB_PASSWORD | vacío | Contraseña SQL |
| DEMO_DATA | true | Crear datos de demostración solo si la base está vacía |
| ADMIN_ID | P001 | Coordinador inicial sin demostración |
| ADMIN_EMAIL | coordinacion@example.com | Correo inicial sin demostración |
| ADMIN_PASSWORD | sin valor | Obligatoria con DEMO_DATA=false, mínimo 8 caracteres |

También se incluye controlador y esquema para PostgreSQL. Para usarlo, crea una base vacía y establece `DB_URL=jdbc:postgresql://localhost:5432/tutorias`, `DB_USER` y `DB_PASSWORD`. **Esta entrega se verificó con H2; no se ejecutó contra un servidor PostgreSQL.** No se migra automáticamente una base H2 existente. Usa una base nueva si cambias las opciones iniciales de demostración o administrador.

## Pruebas

Desde PowerShell: `powershell -ExecutionPolicy Bypass -File scripts/probar.ps1`.

Resultado comprobado: 91 comprobaciones del modelo y consola y 107 comprobaciones HTTP, seguridad, registro de estudiantes y persistencia. Las pruebas crean bases temporales; no modifican `data/tutorias`.

Las pruebas de navegador requieren Node.js, Playwright y Chromium o Edge. En una instalación normal: `npm install`, `npx playwright install chromium`, luego `npm run test:web`. Para Edge se puede definir `BROWSER_CHANNEL=msedge`. Resultado comprobado: 40 comprobaciones, incluido el registro en computadora y celular, sin errores JavaScript. No se necesita Node para ejecutar la aplicación, solo para estas pruebas.

## Archivos

| Ruta | Contenido |
| --- | --- |
| src/ | Modelo, controladores, servidor HTTP, autenticación y repositorio SQL |
| web/ | Interfaz del navegador |
| sql/schema.sql | Tablas y restricciones |
| scripts/ | Inicio, dependencias y pruebas |
| lib/ | Controladores y licencias |
| tests/ | Pruebas Java y navegador |
| docs/CUARTA_ENTREGA.md | Cobertura, arquitectura y estado de la entrega |
| docs/PRUEBAS_WEB.md | Resultados, comandos y alcance verificado |
| docs/UML_WEB.md | Relaciones de la capa web y SQL |
| docs/ENTREGA_EQUIPO.md | Evidencias que debe aportar el equipo |
| entrega/ | Informe actualizado, PDF y evidencias para la presentación |

La consola anterior se conserva: `java -cp bin Main`, después de compilar. Esa variante sigue trabajando en memoria. `docs/TERCERA_ENTREGA.md`, `docs/PRUEBAS.md` y `docs/UML.md` son antecedentes de la tercera entrega.
