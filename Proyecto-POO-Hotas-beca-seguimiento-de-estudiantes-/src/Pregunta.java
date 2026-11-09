public class Pregunta {
    private int idPregunta;
    private String contenido;
    private Estudiante estudiante;
    private SolicitudTutoria solicitud;

    public Pregunta(int id, String contenido, Estudiante estudiante, SolicitudTutoria solicitud) {
        Validacion.positivo(id, "Identificador");
        if (solicitud == null || estudiante == null || solicitud.getEstudiante() != estudiante) {
            throw new IllegalArgumentException("La pregunta debe pertenecer al estudiante de la solicitud.");
        }
        idPregunta = id;
        this.contenido = Validacion.texto(contenido, "Pregunta");
        this.estudiante = estudiante;
        this.solicitud = solicitud;
    }
    public int getIdPregunta() { return idPregunta; }
    public String getContenido() { return contenido; }
    public Estudiante getEstudiante() { return estudiante; }
    public SolicitudTutoria getSolicitud() { return solicitud; }
    public String toString() {
        return "Pregunta " + idPregunta + " | Solicitud " + solicitud.getIdSolicitud() + " | " + contenido;
    }
}
