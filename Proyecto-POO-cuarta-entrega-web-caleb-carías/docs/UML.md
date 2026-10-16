# UML de la implementacion

Atributos y metodos declarados en las 21 clases o enumeraciones de `src/`. Se incluyen constructores y metodos privados; los miembros heredados no se repiten. Los tres diagramas son vistas del mismo sistema. `+` indica public, `-` private; los miembros estaticos se marcan con `$` en Mermaid. Los tipos ArrayList se muestran con su tipo de objeto.

Las relaciones se indican por referencias y listas existentes. El rombo lleno representa composicion; el vacio, agregacion. Las flechas discontinuas representan dependencias. Los objetos se comparten por referencia: una sesion aparece en ambos historiales sin duplicarse. No se usan interfaces ni herencia multiple.

## Modelo

```mermaid
classDiagram
    class Usuario {
        -String nombre
        -String carnet
        -String correo
        +Usuario(String nombre, String carnet, String correo)
        +getNombre() String
        +getCarnet() String
        +getCorreo() String
        +setNombre(String nombre) void
        +setCorreo(String correo) void
        +toString() String
    }
    class Estudiante {
        -int anioCarrera
        -ArrayList~SolicitudTutoria~ solicitudes
        -ArrayList~SesionTutoria~ historialTutorias
        +Estudiante(String nombre, String carnet, String correo, int anioCarrera)
        +getAnioCarrera() int
        +setAnioCarrera(int anio) void
        +getSolicitudes() ArrayList~SolicitudTutoria~
        +getHistorialTutorias() ArrayList~SesionTutoria~
        +agregarSolicitud(SolicitudTutoria solicitud) void
        +agregarTutoriaHistorial(SesionTutoria sesion) void
    }
    class Tutor {
        -ArrayList~Curso~ cursosAsignados
        -ArrayList~Horario~ disponibilidad
        -ArrayList~SesionTutoria~ historialTutorias
        -RegistroHorasBeca registroHorasBeca
        -double remuneracionAcumulada
        -boolean aprobado
        -boolean recibeHorasBeca
        -double tarifaHora
        -int anioCarrera
        +Tutor(String nombre, String carnet, String correo, int anioCarrera, boolean recibeHorasBeca, double tarifaHora)
        +getAnioCarrera() int
        +isRecibeHorasBeca() boolean
        +getTarifaHora() double
        +isAprobado() boolean
        +setAprobado(boolean aprobado) void
        +getRemuneracionAcumulada() double
        +getRegistroHorasBeca() RegistroHorasBeca
        +getCursosAsignados() ArrayList~Curso~
        +getDisponibilidad() ArrayList~Horario~
        +getHistorialTutorias() ArrayList~SesionTutoria~
        +agregarCurso(Curso curso) void
        +agregarHorario(Horario horario) void
        +agregarSesion(SesionTutoria sesion) void
        +toString() String
    }
    class ProfesorCoordinador {
        -ArrayList~Tutor~ tutoresSupervisados
        -ArrayList~Curso~ cursos
        -Tutor perfilTutor
        +ProfesorCoordinador(String nombre, String carnet, String correo)
        +getPerfilTutor() Tutor
        +getTutoresSupervisados() ArrayList~Tutor~
        +getCursos() ArrayList~Curso~
        +getTutoriasImpartidas() ArrayList~SesionTutoria~
        +agregarCurso(Curso curso) void
        +aprobarTutor(Tutor tutor, Curso curso) void
    }
    class Curso {
        -String codigo
        -String nombre
        -ArrayList~Tutor~ tutoresDisponibles
        +Curso(String codigo, String nombre)
        +getCodigo() String
        +getNombre() String
        +getTutoresDisponibles() ArrayList~Tutor~
        +agregarTutor(Tutor tutor) void
        +toString() String
    }
    class Horario {
        -String fecha
        -String horaInicio
        -String horaFin
        -boolean disponible
        +Horario(String fecha, String horaInicio, String horaFin)
        +getFecha() String
        +getHoraInicio() String
        +getHoraFin() String
        +isDisponible() boolean
        +setDisponible(boolean disponible) void
        +getDuracionHoras() double
        +seCruzaCon(Horario otro) boolean
        +toString() String
    }
    class SolicitudTutoria {
        -int idSolicitud
        -Estudiante estudiante
        -Tutor tutor
        -Curso curso
        -String tema
        -Horario horario
        -EstadoSolicitud estado
        -double precio
        +SolicitudTutoria(int id, Estudiante estudiante, Tutor tutor, Curso curso, String tema, Horario horario)
        +getIdSolicitud() int
        +getEstudiante() Estudiante
        +getTutor() Tutor
        +getCurso() Curso
        +getTema() String
        +getHorario() Horario
        +getEstado() EstadoSolicitud
        +getPrecio() double
        +confirmar() void
        +cancelar() void
        +finalizar() void
        +toString() String
    }
    class EstadoSolicitud {
        <<enumeration>>
        PENDIENTE
        CONFIRMADA
        CANCELADA
        FINALIZADA
    }
    class SesionTutoria {
        -int idSesion
        -SolicitudTutoria solicitud
        -String temasVistos
        -String notas
        -String temaProximaTutoria
        -double duracionHoras
        -boolean finalizada
        +SesionTutoria(int id, SolicitudTutoria solicitud)
        +getIdSesion() int
        +getSolicitud() SolicitudTutoria
        +getTemasVistos() String
        +getNotas() String
        +getTemaProximaTutoria() String
        +getDuracionHoras() double
        +isFinalizada() boolean
        +finalizar(String temas, String notas, String proximoTema) void
        +toString() String
    }
    class Pregunta {
        -int idPregunta
        -String contenido
        -Estudiante estudiante
        -SolicitudTutoria solicitud
        +Pregunta(int id, String contenido, Estudiante estudiante, SolicitudTutoria solicitud)
        +getIdPregunta() int
        +getContenido() String
        +getEstudiante() Estudiante
        +getSolicitud() SolicitudTutoria
        +toString() String
    }
    class RegistroHorasBeca {
        -double horasAcumuladas
        -ArrayList~SesionTutoria~ sesionesRegistradas
        +RegistroHorasBeca()
        +getHorasAcumuladas() double
        +getSesionesRegistradas() ArrayList~SesionTutoria~
        +registrarSesion(SesionTutoria sesion) void
    }
    Usuario <|-- Estudiante
    Usuario <|-- Tutor
    Usuario <|-- ProfesorCoordinador
    Estudiante "1" o-- "0..*" SolicitudTutoria : solicitudes
    Estudiante "1" o-- "0..*" SesionTutoria : historial
    Tutor "0..*" -- "0..*" Curso : autorizacion
    Tutor "1" o-- "0..*" Horario : disponibilidad
    Tutor "1" o-- "0..*" SesionTutoria : historial
    Tutor "1" *-- "1" RegistroHorasBeca : registro
    RegistroHorasBeca "1" o-- "0..*" SesionTutoria : sesiones acreditadas
    ProfesorCoordinador "1" *-- "1" Tutor : perfilTutor
    ProfesorCoordinador "1" --> "0..*" Tutor : supervisa
    ProfesorCoordinador "1" --> "0..*" Curso : coordina
    SolicitudTutoria "0..*" --> "1" Tutor : tutor
    SolicitudTutoria "0..*" --> "1" Curso : curso
    SolicitudTutoria "0..*" --> "1" Horario : bloque
    SolicitudTutoria --> EstadoSolicitud : estado
    SolicitudTutoria "1" <-- "0..1" SesionTutoria : solicitud
    Pregunta "0..*" --> "1" SolicitudTutoria : solicitud
    Pregunta "0..*" --> "1" Estudiante : autor
```

## Controladores y vistas

```mermaid
classDiagram
    class EstudianteController {
        -SistemaTutorias sistema
        +EstudianteController(SistemaTutorias sistema)
        +consultarCursos() ArrayList~Curso~
        +consultarTutores(Curso curso) ArrayList~Tutor~
        +consultarHorarios(Tutor tutor) ArrayList~Horario~
        +consultarPrecio(Tutor tutor, Curso curso, Horario horario) double
        +solicitarTutoria(Estudiante estudiante, Curso curso, Tutor tutor, String tema, Horario horario) SolicitudTutoria
        +consultarSolicitudes(Estudiante estudiante) ArrayList~SolicitudTutoria~
        +consultarHistorial(Estudiante estudiante) ArrayList~SesionTutoria~
        +cancelarTutoria(Estudiante estudiante, SolicitudTutoria solicitud) void
        +registrarPregunta(Estudiante estudiante, SolicitudTutoria solicitud, String contenido) Pregunta
        +consultarPreguntas(Estudiante estudiante) ArrayList~Pregunta~
        -verificarSolicitud(Estudiante estudiante, SolicitudTutoria solicitud) void
    }
    class TutorController {
        -SistemaTutorias sistema
        +TutorController(SistemaTutorias sistema)
        +consultarCursosAsignados(Tutor tutor) ArrayList~Curso~
        +consultarSolicitudes(Tutor tutor) ArrayList~SolicitudTutoria~
        +aceptarTutoria(Tutor tutor, SolicitudTutoria solicitud) SesionTutoria
        +cancelarTutoria(Tutor tutor, SolicitudTutoria solicitud) void
        +consultarSesionesPendientes(Tutor tutor) ArrayList~SesionTutoria~
        +finalizarSesion(Tutor tutor, SesionTutoria sesion, String temas, String notas, String proximo) void
        +consultarHistorial(Tutor tutor) ArrayList~SesionTutoria~
        +consultarRemuneracion(Tutor tutor) double
        +consultarHorasBeca(Tutor tutor) double
        +consultarPago(Tutor tutor) double
        +consultarPreguntas(Tutor tutor) ArrayList~Pregunta~
        +agregarHorario(Tutor tutor, String fecha, String inicio, String fin) void
        -verificarSolicitud(Tutor tutor, SolicitudTutoria solicitud) void
    }
    class CoordinadorController {
        -SistemaTutorias sistema
        +CoordinadorController(SistemaTutorias sistema)
        +consultarTutores() ArrayList~Tutor~
        +consultarCursos() ArrayList~Curso~
        +consultarEstudiantes() ArrayList~Estudiante~
        +consultarEstudiantesConTutorias() ArrayList~Estudiante~
        +consultarProgreso(Estudiante estudiante) ArrayList~SesionTutoria~
        +consultarSolicitudes() ArrayList~SolicitudTutoria~
        +consultarHistorial() ArrayList~SesionTutoria~
        +aprobarTutor(Tutor tutor, Curso curso) void
        +registrarEstudiante(String nombre, String carnet, String correo, int anio) void
        +registrarTutor(String nombre, String carnet, String correo, int anio, boolean beca, double tarifa) void
        +registrarCurso(String codigo, String nombre) void
        +buscarTutorAlternativo(Curso curso, Estudiante estudiante) Tutor
        +consultarDemanda() String
    }
    class EstudianteView {
        -EstudianteController controlador
        -CoordinadorController coordinadorController
        -Entrada entrada
        -Estudiante estudiante
        +EstudianteView(EstudianteController controlador, CoordinadorController coordinadorController, Entrada entrada, Estudiante estudiante)
        +mostrarMenu() void
        -elegirCurso() Curso
        -consultarOferta() void
        -solicitarTutoria() void
        -elegirSolicitud() SolicitudTutoria
        +mostrarSolicitudes(ArrayList~SolicitudTutoria~ solicitudes) void
        +mostrarHistorial(ArrayList~SesionTutoria~ sesiones) void
    }
    class TutorView {
        -TutorController controlador
        -Entrada entrada
        -Tutor tutor
        +TutorView(TutorController controlador, Entrada entrada, Tutor tutor)
        +mostrarMenu() void
        -elegirSolicitud() SolicitudTutoria
        -finalizarSesion() void
        +mostrarSolicitudes(ArrayList~SolicitudTutoria~ solicitudes) void
        +mostrarHistorial(ArrayList~SesionTutoria~ sesiones) void
    }
    class CoordinadorView {
        -CoordinadorController controlador
        -TutorController tutorController
        -ProfesorCoordinador coordinador
        -Entrada entrada
        +CoordinadorView(CoordinadorController controlador, TutorController tutorController, ProfesorCoordinador coordinador, Entrada entrada)
        +mostrarMenu() void
        -aprobarTutor() void
        -registrarTutor() void
        +mostrarEstudiantes(ArrayList~Estudiante~ estudiantes) void
        +mostrarHistorial(ArrayList~SesionTutoria~ sesiones) void
    }
    EstudianteController --> SistemaTutorias
    TutorController --> SistemaTutorias
    CoordinadorController --> SistemaTutorias
    EstudianteView --> EstudianteController
    EstudianteView --> CoordinadorController
    TutorView --> TutorController
    CoordinadorView --> CoordinadorController
    CoordinadorView --> TutorController
    EstudianteView --> Entrada
    TutorView --> Entrada
    CoordinadorView --> Entrada
    EstudianteView --> Estudiante
    TutorView --> Tutor
    CoordinadorView --> ProfesorCoordinador
    CoordinadorView ..> TutorView : usa para el profesor
```

## Datos compartidos, entrada y arranque

```mermaid
classDiagram
    class SistemaTutorias {
        -ArrayList~Estudiante~ estudiantes
        -ArrayList~Tutor~ tutores
        -ArrayList~Curso~ cursos
        -ArrayList~SolicitudTutoria~ solicitudes
        -ArrayList~SesionTutoria~ sesiones
        -ArrayList~Pregunta~ preguntas
        -ProfesorCoordinador coordinador
        +SistemaTutorias(ProfesorCoordinador coordinador)
        +getCoordinador() ProfesorCoordinador
        +getEstudiantes() ArrayList~Estudiante~
        +getTutores() ArrayList~Tutor~
        +getCursos() ArrayList~Curso~
        +getSolicitudes() ArrayList~SolicitudTutoria~
        +getSesiones() ArrayList~SesionTutoria~
        +getPreguntas() ArrayList~Pregunta~
        +agregarEstudiante(Estudiante estudiante) void
        +agregarTutor(Tutor tutor) void
        +agregarCurso(Curso curso) void
        +agregarSolicitud(SolicitudTutoria solicitud) void
        +agregarSesion(SesionTutoria sesion) void
        +agregarPregunta(Pregunta pregunta) void
        +tieneConflicto(String carnet, Horario horario) boolean
    }
    class Entrada {
        -Scanner scanner
        +Entrada(Scanner scanner)
        +leerTexto(String mensaje) String
        +leerOpcional(String mensaje) String
        +leerEntero(String mensaje, int minimo, int maximo) int
        +leerDecimal(String mensaje) double
        +leerSiNo(String mensaje) boolean
        +elegirIndice(int cantidad) int
    }
    class Validacion {
        +texto(String valor, String campo) String$
        +positivo(int valor, String campo) void$
        +noNegativo(double valor, String campo) void$
        +minutos(String hora) int$
        +fecha(String fecha) void$
    }
    class Main {
        +main(String[] args) void$
        +crearDemostracion() SistemaTutorias$
    }
    SistemaTutorias "1" o-- "0..*" Estudiante
    SistemaTutorias "1" o-- "1..*" Tutor
    SistemaTutorias "1" o-- "0..*" Curso
    SistemaTutorias "1" o-- "0..*" SolicitudTutoria
    SistemaTutorias "1" o-- "0..*" SesionTutoria
    SistemaTutorias "1" o-- "0..*" Pregunta
    SistemaTutorias "1" --> "1" ProfesorCoordinador
    Entrada --> Scanner
    Entrada ..> Validacion
    Horario ..> Validacion
    Main ..> SistemaTutorias
    Main ..> EstudianteView
    Main ..> TutorView
    Main ..> CoordinadorView
    Main ..> Entrada
```
