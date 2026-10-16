import java.util.ArrayList;

public class EstudianteController {
    private SistemaTutorias sistema;

    public EstudianteController(SistemaTutorias sistema) { this.sistema = sistema; }
    public ArrayList<Curso> consultarCursos() {
        ArrayList<Curso> resultado = new ArrayList<Curso>();
        for (Curso curso : sistema.getCursos()) {
            if (!consultarTutores(curso).isEmpty()) { resultado.add(curso); }
        }
        return resultado;
    }
    public ArrayList<Tutor> consultarTutores(Curso curso) {
        ArrayList<Tutor> resultado = new ArrayList<Tutor>();
        if (!sistema.getCursos().contains(curso)) {
            throw new IllegalArgumentException("Curso no registrado.");
        }
        for (Tutor tutor : curso.getTutoresDisponibles()) {
            if (tutor.isAprobado() && !consultarHorarios(tutor).isEmpty()) { resultado.add(tutor); }
        }
        return resultado;
    }
    public ArrayList<Horario> consultarHorarios(Tutor tutor) {
        ArrayList<Horario> resultado = new ArrayList<Horario>();
        if (!sistema.getTutores().contains(tutor)) {
            throw new IllegalArgumentException("Tutor no registrado.");
        }
        for (Horario horario : tutor.getDisponibilidad()) {
            if (tutor.isAprobado() && horario.isDisponible()
                    && !sistema.tieneConflicto(tutor.getCarnet(), horario)) {
                resultado.add(horario);
            }
        }
        return resultado;
    }
    public double consultarPrecio(Tutor tutor, Curso curso, Horario horario) {
        if (!sistema.getTutores().contains(tutor) || !sistema.getCursos().contains(curso)
                || !tutor.isAprobado() || !tutor.getCursosAsignados().contains(curso)
                || !tutor.getDisponibilidad().contains(horario)) {
            throw new IllegalArgumentException("El tutor no ofrece esa combinacion de curso y horario.");
        }
        double precio = tutor.getTarifaHora() * horario.getDuracionHoras();
        Validacion.noNegativo(precio, "Precio total");
        return precio;
    }
    public SolicitudTutoria solicitarTutoria(Estudiante estudiante, Curso curso, Tutor tutor,
            String tema, Horario horario) {
        if (!sistema.getEstudiantes().contains(estudiante)) {
            throw new IllegalArgumentException("Estudiante no registrado.");
        }
        consultarPrecio(tutor, curso, horario);
        if (estudiante.getCarnet().equals(tutor.getCarnet())) {
            throw new IllegalArgumentException("No puede solicitarse una tutoria a si mismo.");
        }
        if (!horario.isDisponible() || sistema.tieneConflicto(tutor.getCarnet(), horario)
                || sistema.tieneConflicto(estudiante.getCarnet(), horario)) {
            throw new IllegalArgumentException("El horario esta ocupado para el estudiante o tutor.");
        }
        SolicitudTutoria solicitud = new SolicitudTutoria(sistema.getSolicitudes().size() + 1,
                estudiante, tutor, curso, tema, horario);
        sistema.agregarSolicitud(solicitud);
        estudiante.agregarSolicitud(solicitud);
        // Se reserva desde la solicitud para impedir dos solicitudes del mismo bloque.
        horario.setDisponible(false);
        return solicitud;
    }
    public ArrayList<SolicitudTutoria> consultarSolicitudes(Estudiante estudiante) {
        return estudiante.getSolicitudes();
    }
    public ArrayList<SesionTutoria> consultarHistorial(Estudiante estudiante) {
        return estudiante.getHistorialTutorias();
    }
    public void cancelarTutoria(Estudiante estudiante, SolicitudTutoria solicitud) {
        verificarSolicitud(estudiante, solicitud);
        solicitud.cancelar();
    }
    public Pregunta registrarPregunta(Estudiante estudiante, SolicitudTutoria solicitud, String contenido) {
        verificarSolicitud(estudiante, solicitud);
        if (solicitud.getEstado() == EstadoSolicitud.CANCELADA
                || solicitud.getEstado() == EstadoSolicitud.FINALIZADA) {
            throw new IllegalStateException("Solo puede preguntar sobre una solicitud activa.");
        }
        Pregunta pregunta = new Pregunta(sistema.getPreguntas().size() + 1, contenido, estudiante, solicitud);
        sistema.agregarPregunta(pregunta);
        return pregunta;
    }
    public ArrayList<Pregunta> consultarPreguntas(Estudiante estudiante) {
        ArrayList<Pregunta> resultado = new ArrayList<Pregunta>();
        for (Pregunta pregunta : sistema.getPreguntas()) {
            if (pregunta.getEstudiante() == estudiante) { resultado.add(pregunta); }
        }
        return resultado;
    }
    private void verificarSolicitud(Estudiante estudiante, SolicitudTutoria solicitud) {
        if (!sistema.getEstudiantes().contains(estudiante) || !sistema.getSolicitudes().contains(solicitud)
                || solicitud.getEstudiante() != estudiante) {
            throw new IllegalArgumentException("La solicitud no pertenece al estudiante.");
        }
    }
}
