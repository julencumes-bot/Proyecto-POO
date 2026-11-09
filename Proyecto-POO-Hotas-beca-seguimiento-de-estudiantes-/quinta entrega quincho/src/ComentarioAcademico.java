import java.time.Instant;

/** Observación interna de aprendizaje, separada de las notas del estudiante. */
public class ComentarioAcademico {
    private final int id;
    private final SesionTutoria sesion;
    private final Tutor autor;
    private final String fecha;
    private final String contenido;

    public ComentarioAcademico(int id, SesionTutoria sesion, Tutor autor, String contenido) {
        this(id, sesion, autor, Instant.now().toString(), contenido);
    }
    ComentarioAcademico(int id, SesionTutoria sesion, Tutor autor, String fecha, String contenido) {
        Validacion.positivo(id, "Comentario");
        if (sesion == null || !sesion.isFinalizada() || autor != sesion.getSolicitud().getTutor()) {
            throw new IllegalArgumentException("Solo quien impartió la sesión finalizada puede escribir.");
        }
        this.contenido = Validacion.texto(contenido, "Observación académica");
        if (this.contenido.length() > 2000) {
            throw new IllegalArgumentException("La observación admite hasta 2000 caracteres.");
        }
        Instant.parse(fecha);
        this.id = id;
        this.sesion = sesion;
        this.autor = autor;
        this.fecha = fecha;
    }
    public int getId() { return id; }
    public SesionTutoria getSesion() { return sesion; }
    public Estudiante getEstudiante() { return sesion.getSolicitud().getEstudiante(); }
    public Tutor getAutor() { return autor; }
    public String getFecha() { return fecha; }
    public String getContenido() { return contenido; }
}
