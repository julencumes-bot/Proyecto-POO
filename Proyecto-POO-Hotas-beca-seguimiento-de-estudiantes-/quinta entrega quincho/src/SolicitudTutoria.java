public class SolicitudTutoria {
    private int idSolicitud;
    private Estudiante estudiante;
    private Tutor tutor;
    private Curso curso;
    private String tema;
    private Horario horario;
    private EstadoSolicitud estado;
    private double precio;
    private final boolean recibeHorasBeca;
    private final ModalidadTutoria modalidad;
    private final String ubicacion;
    private final int multiplicadorBeca;

    public SolicitudTutoria(int id, Estudiante estudiante, Tutor tutor, Curso curso,
            String tema, Horario horario) {
        this(id, estudiante, tutor, curso, tema, horario,
                horario == null ? null : horario.getModalidad());
    }
    public SolicitudTutoria(int id, Estudiante estudiante, Tutor tutor, Curso curso,
            String tema, Horario horario, ModalidadTutoria modalidad) {
        this(id, estudiante, tutor, curso, tema, horario,
                tutor == null || horario == null ? 0 : tutor.getTarifaHora() * horario.getDuracionHoras(),
                tutor != null && tutor.isRecibeHorasBeca(),
                horario == null ? null : horario.getModalidad(),
                horario == null ? null : horario.getUbicacion(),
                tutor != null && tutor.isRecibeHorasBeca() ? tutor.getMultiplicadorBeca() : 1);
        if (!horario.estaCompleto()) {
            throw new IllegalArgumentException("El tutor debe completar la modalidad y ubicación del horario.");
        }
        if (modalidad != null && modalidad != horario.getModalidad()) {
            throw new IllegalArgumentException("La modalidad la establece el tutor en el horario.");
        }
    }
    // Recuperar una reserva no recalcula su precio con la tarifa actual del tutor.
    SolicitudTutoria(int id, Estudiante estudiante, Tutor tutor, Curso curso,
            String tema, Horario horario, double precioPactado, boolean becaPactada) {
        this(id, estudiante, tutor, curso, tema, horario, precioPactado, becaPactada, null);
    }
    SolicitudTutoria(int id, Estudiante estudiante, Tutor tutor, Curso curso,
            String tema, Horario horario, double precioPactado, boolean becaPactada,
            ModalidadTutoria modalidad) {
        this(id, estudiante, tutor, curso, tema, horario, precioPactado, becaPactada, modalidad, null, 1);
    }
    // La copia de la reserva no depende de cambios posteriores al horario o al perfil.
    SolicitudTutoria(int id, Estudiante estudiante, Tutor tutor, Curso curso,
            String tema, Horario horario, double precioPactado, boolean becaPactada,
            ModalidadTutoria modalidad, String ubicacion, int multiplicadorBeca) {
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
        precio = precioPactado;
        recibeHorasBeca = becaPactada;
        this.modalidad = modalidad;
        this.ubicacion = ubicacion;
        Tutor.validarMultiplicador(multiplicadorBeca);
        this.multiplicadorBeca = becaPactada ? multiplicadorBeca : 1;
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
    public boolean isRecibeHorasBeca() { return recibeHorasBeca; }
    public ModalidadTutoria getModalidad() { return modalidad; }
    public String getUbicacion() { return ubicacion; }
    public int getMultiplicadorBeca() { return multiplicadorBeca; }
    // Exclusivo del repositorio: conserva el precio pactado al recuperar la reserva.
    void restaurarPrecio(double precioGuardado) {
        Validacion.noNegativo(precioGuardado, "Precio guardado");
        precio = precioGuardado;
    }
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
                + " | Tema: " + tema + " | " + horario.getFecha() + " "
                + horario.getHoraInicio() + "-" + horario.getHoraFin()
                + " | " + (modalidad == null ? "Pendiente de completar" : modalidad.getEtiqueta())
                + " | " + (ubicacion == null ? "Ubicación pendiente de completar" : ubicacion)
                + " | Precio total: Q" + precio;
    }
}
