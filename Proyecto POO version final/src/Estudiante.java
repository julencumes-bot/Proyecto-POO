import java.util.ArrayList;

public class Estudiante extends Usuario {
    private int anioCarrera;
    private ArrayList<ComentarioAcademico> comentarios = new ArrayList<ComentarioAcademico>();

    public int getTutoriasFinalizadas() { return historialTutorias.size(); }
    public ArrayList<ComentarioAcademico> getComentarios() {
        return new ArrayList<ComentarioAcademico>(comentarios);
    }
    public void agregarComentario(ComentarioAcademico comentario) {
        if (comentario == null || comentario.getEstudiante() != this || comentarios.contains(comentario)) {
            throw new IllegalArgumentException("Comentario ajeno o repetido.");
        }
        comentarios.add(comentario);
    }
    private ArrayList<SolicitudTutoria> solicitudes;
    private ArrayList<SesionTutoria> historialTutorias;

    public Estudiante(String nombre, String carnet, String correo, int anioCarrera) {
        super(nombre, carnet, correo);
        Validacion.positivo(anioCarrera, "Anio de carrera");
        this.anioCarrera = anioCarrera;
        solicitudes = new ArrayList<SolicitudTutoria>();
        historialTutorias = new ArrayList<SesionTutoria>();
    }

    public int getAnioCarrera() { return anioCarrera; }
    public void setAnioCarrera(int anio) {
        Validacion.positivo(anio, "Anio de carrera");
        anioCarrera = anio;
    }
    public ArrayList<SolicitudTutoria> getSolicitudes() {
        return new ArrayList<SolicitudTutoria>(solicitudes);
    }
    public ArrayList<SesionTutoria> getHistorialTutorias() {
        return new ArrayList<SesionTutoria>(historialTutorias);
    }
    public void agregarSolicitud(SolicitudTutoria solicitud) {
        if (solicitud.getEstudiante() != this || solicitudes.contains(solicitud)) {
            throw new IllegalArgumentException("Solicitud ajena o repetida.");
        }
        solicitudes.add(solicitud);
    }
    public void agregarTutoriaHistorial(SesionTutoria sesion) {
        if (sesion.getSolicitud().getEstudiante() != this || !sesion.isFinalizada()
                || historialTutorias.contains(sesion)) {
            throw new IllegalArgumentException("La sesion no puede agregarse al historial.");
        }
        historialTutorias.add(sesion);
    }
}
