import java.util.ArrayList;

public class CoordinadorController {
    private SistemaTutorias sistema;

    public CoordinadorController(SistemaTutorias sistema) { this.sistema = sistema; }
    public ArrayList<Tutor> consultarTutores() { return sistema.getTutores(); }
    public ArrayList<Curso> consultarCursos() { return sistema.getCursos(); }
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
        if (!sistema.getTutores().contains(tutor) || !sistema.getCursos().contains(curso)) {
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
    public String consultarDemanda() {
        String resultado = "Demanda por curso (solicitudes no canceladas):\n";
        for (Curso curso : sistema.getCursos()) {
            int cantidad = 0;
            ArrayList<String> temas = new ArrayList<String>();
            for (SolicitudTutoria solicitud : sistema.getSolicitudes()) {
                if (solicitud.getCurso() == curso && solicitud.getEstado() != EstadoSolicitud.CANCELADA) {
                    cantidad++;
                    String tema = solicitud.getTema().toLowerCase();
                    if (!temas.contains(tema)) { temas.add(tema); }
                }
            }
            resultado += curso + ": " + cantidad + " solicitudes\n";
            for (String tema : temas) {
                int total = 0;
                for (SolicitudTutoria solicitud : sistema.getSolicitudes()) {
                    if (solicitud.getCurso() == curso && solicitud.getEstado() != EstadoSolicitud.CANCELADA
                            && solicitud.getTema().equalsIgnoreCase(tema)) { total++; }
                }
                resultado += "  " + tema + ": " + total + "\n";
            }
        }
        return resultado;
    }
}
