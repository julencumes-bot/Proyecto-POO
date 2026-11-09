public class SolicitudTutoria {
    private int idSolicitud;
    private Estudiante estudiante;
    private Tutor tutor;
    private Curso curso;
    private String tema;
    private Horario horario;
    private EstadoSolicitud estado;
    private double precio;

    public SolicitudTutoria(int id, Estudiante estudiante, Tutor tutor, Curso curso,
            String tema, Horario horario) {
        Validacion.positivo(id, "Identificador");
        if (estudiante == null || tutor == null || curso == null || horario == null) {
            throw new IllegalArgumentException("La solicitud necesita estudiante, tutor, curso y horario.");
        }
        this.tema = Validacion.texto(tema, "Tema");
        idSolicitud = id;
        this.estudiante = estudiante;
        this.tutor = tutor;
        this.curso = curso;
        this.horario = horario;
        precio = tutor.getTarifaHora() * horario.getDuracionHoras();
        Validacion.noNegativo(precio, "Precio total");
        estado = EstadoSolicitud.PENDIENTE;
    }
    public int getIdSolicitud() { return idSolicitud; }
    public Estudiante getEstudiante() { return estudiante; }
    public Tutor getTutor() { return tutor; }
    public Curso getCurso() { return curso; }
    public String getTema() { return tema; }
    public Horario getHorario() { return horario; }
    public EstadoSolicitud getEstado() { return estado; }
    public double getPrecio() { return precio; }
    public void confirmar() {
        if (estado != EstadoSolicitud.PENDIENTE) {
            throw new IllegalStateException("Solo se puede aceptar una solicitud pendiente.");
        }
        estado = EstadoSolicitud.CONFIRMADA;
    }
    public void cancelar() {
        if (estado != EstadoSolicitud.PENDIENTE && estado != EstadoSolicitud.CONFIRMADA) {
            throw new IllegalStateException("La solicitud ya esta cerrada.");
        }
        estado = EstadoSolicitud.CANCELADA;
        horario.setDisponible(true);
    }
    public void finalizar() {
        if (estado != EstadoSolicitud.CONFIRMADA) {
            throw new IllegalStateException("La solicitud debe estar confirmada.");
        }
        estado = EstadoSolicitud.FINALIZADA;
    }
    public String toString() {
        return "Solicitud " + idSolicitud + " | " + estado + " | " + curso.getNombre()
                + " | Estudiante: " + estudiante.getNombre() + " | Tutor: " + tutor.getNombre()
                + " | Tema: " + tema + " | " + horario + " | Precio total: Q" + precio;
    }
}
