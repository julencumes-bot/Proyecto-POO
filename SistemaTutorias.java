import java.util.ArrayList;

/** Mantiene los objetos compartidos durante una ejecucion del programa. */
public class SistemaTutorias {
    private ArrayList<Estudiante> estudiantes;
    private ArrayList<Tutor> tutores;
    private ArrayList<Curso> cursos;
    private ArrayList<SolicitudTutoria> solicitudes;
    private ArrayList<SesionTutoria> sesiones;
    private ArrayList<Pregunta> preguntas;
    private ProfesorCoordinador coordinador;

    public SistemaTutorias(ProfesorCoordinador coordinador) {
        if (coordinador == null) { throw new IllegalArgumentException("Coordinador requerido."); }
        this.coordinador = coordinador;
        estudiantes = new ArrayList<Estudiante>();
        tutores = new ArrayList<Tutor>();
        cursos = new ArrayList<Curso>();
        solicitudes = new ArrayList<SolicitudTutoria>();
        sesiones = new ArrayList<SesionTutoria>();
        preguntas = new ArrayList<Pregunta>();
        tutores.add(coordinador.getPerfilTutor());
    }
    public ProfesorCoordinador getCoordinador() { return coordinador; }
    public ArrayList<Estudiante> getEstudiantes() { return new ArrayList<Estudiante>(estudiantes); }
    public ArrayList<Tutor> getTutores() { return new ArrayList<Tutor>(tutores); }
    public ArrayList<Curso> getCursos() { return new ArrayList<Curso>(cursos); }
    public ArrayList<SolicitudTutoria> getSolicitudes() {
        return new ArrayList<SolicitudTutoria>(solicitudes);
    }
    public ArrayList<SesionTutoria> getSesiones() { return new ArrayList<SesionTutoria>(sesiones); }
    public ArrayList<Pregunta> getPreguntas() { return new ArrayList<Pregunta>(preguntas); }
    public void agregarEstudiante(Estudiante estudiante) {
        if (estudiante == null) { throw new IllegalArgumentException("Estudiante requerido."); }
        if (estudiante.getCarnet().equals(coordinador.getCarnet())) {
            throw new IllegalArgumentException("Ese carne pertenece al coordinador.");
        }
        for (Estudiante existente : estudiantes) {
            if (existente.getCarnet().equals(estudiante.getCarnet())) {
                throw new IllegalArgumentException("Ya existe un estudiante con ese carne.");
            }
        }
        estudiantes.add(estudiante);
    }
    public void agregarTutor(Tutor tutor) {
        if (tutor == null) { throw new IllegalArgumentException("Tutor requerido."); }
        for (Tutor existente : tutores) {
            if (existente.getCarnet().equals(tutor.getCarnet())) {
                throw new IllegalArgumentException("Ya existe un tutor con ese carne.");
            }
        }
        tutores.add(tutor);
    }
    public void agregarCurso(Curso curso) {
        if (curso == null) { throw new IllegalArgumentException("Curso requerido."); }
        for (Curso existente : cursos) {
            if (existente.getCodigo().equalsIgnoreCase(curso.getCodigo())) {
                throw new IllegalArgumentException("Ya existe un curso con ese codigo.");
            }
        }
        cursos.add(curso);
        coordinador.agregarCurso(curso);
    }
    public void agregarSolicitud(SolicitudTutoria solicitud) {
        if (solicitud == null || solicitud.getIdSolicitud() != solicitudes.size() + 1) {
            throw new IllegalArgumentException("Identificador de solicitud invalido.");
        }
        solicitudes.add(solicitud);
    }
    public void agregarSesion(SesionTutoria sesion) {
        if (sesion == null || sesion.getIdSesion() != sesiones.size() + 1) {
            throw new IllegalArgumentException("Identificador de sesion invalido.");
        }
        for (SesionTutoria existente : sesiones) {
            if (existente.getSolicitud() == sesion.getSolicitud()) {
                throw new IllegalArgumentException("La solicitud ya tiene una sesion.");
            }
        }
        sesiones.add(sesion);
    }
    public void agregarPregunta(Pregunta pregunta) {
        if (pregunta == null || pregunta.getIdPregunta() != preguntas.size() + 1) {
            throw new IllegalArgumentException("Identificador de pregunta invalido.");
        }
        preguntas.add(pregunta);
    }
    public boolean tieneConflicto(String carnet, Horario horario) {
        for (SolicitudTutoria solicitud : solicitudes) {
            boolean participa = solicitud.getEstudiante().getCarnet().equals(carnet)
                    || solicitud.getTutor().getCarnet().equals(carnet);
            // Una sesion finalizada tampoco puede volver a ocupar el mismo bloque.
            if (participa && solicitud.getEstado() != EstadoSolicitud.CANCELADA
                    && solicitud.getHorario().seCruzaCon(horario)) {
                return true;
            }
        }
        return false;
    }
}
