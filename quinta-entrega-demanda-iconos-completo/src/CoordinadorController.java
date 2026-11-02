import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class CoordinadorController {
    private SistemaTutorias sistema;

    public CoordinadorController(SistemaTutorias sistema) { this.sistema = sistema; }
    public ArrayList<Tutor> consultarTutores() {
        ArrayList<Tutor> resultado = sistema.getTutores();
        resultado.removeIf(t -> !t.isActivo());
        return resultado;
    }
    public ArrayList<Curso> consultarCursos() {
        ArrayList<Curso> resultado = sistema.getCursos();
        resultado.removeIf(c -> !c.isActivo());
        return resultado;
    }
    public ArrayList<Estudiante> consultarEstudiantes() { return sistema.getEstudiantes(); }
    public ArrayList<Estudiante> consultarEstudiantesConTutorias() {
        ArrayList<Estudiante> resultado = new ArrayList<Estudiante>();
        for (Estudiante estudiante : sistema.getEstudiantes()) {
            for (SolicitudTutoria solicitud : estudiante.getSolicitudes()) {
                if ((solicitud.getEstado() == EstadoSolicitud.CONFIRMADA
                        || solicitud.getEstado() == EstadoSolicitud.FINALIZADA)
                        && !resultado.contains(estudiante)) {
                    resultado.add(estudiante);
                }
            }
        }
        return resultado;
    }
    public ArrayList<SesionTutoria> consultarProgreso(Estudiante estudiante) {
        return estudiante.getHistorialTutorias();
    }
    public ArrayList<SolicitudTutoria> consultarSolicitudes() { return sistema.getSolicitudes(); }
    public ArrayList<SesionTutoria> consultarHistorial() {
        ArrayList<SesionTutoria> resultado = new ArrayList<SesionTutoria>();
        for (SesionTutoria sesion : sistema.getSesiones()) {
            if (sesion.isFinalizada()) { resultado.add(sesion); }
        }
        return resultado;
    }
    public void aprobarTutor(Tutor tutor, Curso curso) {
        if (!sistema.getTutores().contains(tutor) || !sistema.getCursos().contains(curso)
                || !tutor.isActivo() || !curso.isActivo()) {
            throw new IllegalArgumentException("El tutor y el curso deben estar registrados.");
        }
        sistema.getCoordinador().aprobarTutor(tutor, curso);
    }
    public void registrarEstudiante(String nombre, String carnet, String correo, int anio) {
        sistema.agregarEstudiante(new Estudiante(nombre, carnet, correo, anio));
    }
    public void registrarTutor(String nombre, String carnet, String correo, int anio, boolean beca, double tarifa) {
        sistema.agregarTutor(new Tutor(nombre, carnet, correo, anio, beca, tarifa));
    }
    public void registrarCurso(String codigo, String nombre) { sistema.agregarCurso(new Curso(codigo, nombre)); }
    public void registrarCurso(String codigo, String nombre, String icono) {
        sistema.agregarCurso(new Curso(codigo, nombre, icono));
    }
    public void modificarTutor(Tutor tutor, String nombre, String correo, int anio, boolean beca, double tarifa) {
        if (!sistema.getTutores().contains(tutor) || !tutor.isActivo()) {
            throw new IllegalArgumentException("Tutor o profesor no disponible.");
        }
        tutor.actualizarDatos(nombre, correo, anio, beca, tarifa);
        ProfesorCoordinador profesor = sistema.getCoordinador();
        if (tutor == profesor.getPerfilTutor()) {
            profesor.setNombre(tutor.getNombre());
            profesor.setCorreo(tutor.getCorreo());
        }
    }
    public void eliminarCurso(Curso curso) {
        if (!sistema.getCursos().contains(curso) || !curso.isActivo()) {
            throw new IllegalArgumentException("Curso no disponible.");
        }
        for (SolicitudTutoria solicitud : sistema.getSolicitudes()) {
            if (solicitud.getCurso() == curso && estaAbierta(solicitud)) {
                throw new IllegalStateException("Resuelve las solicitudes pendientes o confirmadas del curso antes de eliminarlo.");
            }
        }
        curso.retirar();
    }
    public void eliminarTutor(Tutor tutor) {
        if (!sistema.getTutores().contains(tutor) || !tutor.isActivo()) {
            throw new IllegalArgumentException("Tutor o profesor no disponible.");
        }
        for (SolicitudTutoria solicitud : sistema.getSolicitudes()) {
            if (solicitud.getTutor() == tutor && estaAbierta(solicitud)) {
                throw new IllegalStateException("Resuelve las solicitudes pendientes o confirmadas del tutor o profesor antes de quitarlo.");
            }
        }
        tutor.retirar();
    }
    private static boolean estaAbierta(SolicitudTutoria solicitud) {
        return solicitud.getEstado() == EstadoSolicitud.PENDIENTE
                || solicitud.getEstado() == EstadoSolicitud.CONFIRMADA;
    }
    public Tutor buscarTutorAlternativo(Curso curso, Estudiante estudiante) {
        if (!sistema.getEstudiantes().contains(estudiante)) {
            throw new IllegalArgumentException("Estudiante no registrado.");
        }
        EstudianteController controlador = new EstudianteController(sistema);
        ArrayList<Tutor> candidatos = controlador.consultarTutores(curso);
        Tutor perfilProfesor = sistema.getCoordinador().getPerfilTutor();
        // Prioridad: tutor de anio superior, par, otro autorizado y profesor.
        for (int prioridad = 1; prioridad <= 4; prioridad++) {
            for (Tutor tutor : candidatos) {
                int nivel = 3;
                if (tutor == perfilProfesor) { nivel = 4; }
                else if (tutor.getAnioCarrera() > estudiante.getAnioCarrera()) { nivel = 1; }
                else if (tutor.getAnioCarrera() == estudiante.getAnioCarrera()) { nivel = 2; }
                if (nivel == prioridad && !tutor.getCarnet().equals(estudiante.getCarnet())) {
                    for (Horario horario : controlador.consultarHorarios(tutor)) {
                        if (!sistema.tieneConflicto(estudiante.getCarnet(), horario)) { return tutor; }
                    }
                }
            }
        }
        return null;
    }
    public ArrayList<DemandaCurso> consultarDemandaPorCurso() {
        ArrayList<DemandaCurso> resultado = new ArrayList<>();
        for (Curso curso : sistema.getCursos()) {
            int cantidad = 0;
            Map<String, Integer> temas = new LinkedHashMap<>();
            for (SolicitudTutoria solicitud : sistema.getSolicitudes()) {
                if (solicitud.getCurso() == curso && solicitud.getEstado() != EstadoSolicitud.CANCELADA) {
                    cantidad++;
                    String tema = solicitud.getTema().toLowerCase(Locale.ROOT);
                    temas.merge(tema, 1, Integer::sum);
                }
            }
            resultado.add(new DemandaCurso(curso, cantidad, temas));
        }
        return resultado;
    }
    public String consultarDemanda() {
        StringBuilder resultado = new StringBuilder("Demanda por curso (solicitudes no canceladas):\n");
        for (DemandaCurso demanda : consultarDemandaPorCurso()) {
            resultado.append(demanda.curso()).append(": ").append(demanda.cantidad()).append(" solicitudes\n");
            for (var tema : demanda.temas().entrySet()) {
                resultado.append("  ").append(tema.getKey()).append(": ").append(tema.getValue()).append('\n');
            }
        }
        return resultado.toString();
    }
}
