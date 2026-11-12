public class Horario {
    private String fecha;
    private String horaInicio;
    private String horaFin;
    private boolean disponible;
    private ModalidadTutoria modalidad;
    private String ubicacion;

    public Horario(String fecha, String inicio, String fin,
            ModalidadTutoria modalidad, String ubicacion) {
        this(fecha, inicio, fin);
        actualizarCondiciones(modalidad, ubicacion);
    }

    public ModalidadTutoria getModalidad() { return modalidad; }
    public String getUbicacion() { return ubicacion; }
    public boolean estaCompleto() { return modalidad != null && ubicacion != null; }

    public final void actualizarCondiciones(ModalidadTutoria modalidad, String ubicacion) {
        if (!disponible) {
            throw new IllegalStateException("No se pueden cambiar las condiciones de un bloque reservado.");
        }
        if (modalidad == null) {
            throw new IllegalArgumentException("Selecciona Virtual o Presencial.");
        }
        String lugar = Validacion.texto(ubicacion,
                modalidad == ModalidadTutoria.EN_LINEA ? "Enlace de reunión" : "Salón o ubicación");
        if (lugar.length() > 2000) {
            throw new IllegalArgumentException("La ubicación admite hasta 2000 caracteres.");
        }
        if (modalidad == ModalidadTutoria.EN_LINEA) { lugar = validarEnlace(lugar); }
        this.modalidad = modalidad;
        this.ubicacion = lugar;
    }

    public static String validarEnlace(String enlace) {
        String valor = Validacion.texto(enlace, "Enlace de reunión");
        try {
            java.net.URI uri = new java.net.URI(valor);
            if (valor.length() > 2000 || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || !("https".equalsIgnoreCase(uri.getScheme())
                        || "http".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("Escribe un enlace de reunión válido con https:// o http://.");
            }
        } catch (java.net.URISyntaxException error) {
            throw new IllegalArgumentException("Escribe un enlace de reunión válido con https:// o http://.");
        }
        return valor;
    }

    public Horario(String fecha, String horaInicio, String horaFin) {
        Validacion.fecha(fecha);
        if (Validacion.minutos(horaInicio) >= Validacion.minutos(horaFin)) {
            throw new IllegalArgumentException("La hora final debe ser posterior a la inicial.");
        }
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        disponible = true;
    }
    public String getFecha() { return fecha; }
    public String getHoraInicio() { return horaInicio; }
    public String getHoraFin() { return horaFin; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public double getDuracionHoras() {
        return (Validacion.minutos(horaFin) - Validacion.minutos(horaInicio)) / 60.0;
    }
    public boolean seCruzaCon(Horario otro) {
        return fecha.equals(otro.fecha)
                && Validacion.minutos(horaInicio) < Validacion.minutos(otro.horaFin)
                && Validacion.minutos(otro.horaInicio) < Validacion.minutos(horaFin);
    }
    public String toString() {
        return fecha + " " + horaInicio + "-" + horaFin
                + (disponible ? " [Disponible]" : " [Reservado]")
                + " | " + (estaCompleto() ? modalidad.getEtiqueta()
                    + (modalidad == ModalidadTutoria.PRESENCIAL ? ": " + ubicacion : "")
                    : "Pendiente de completar");
    }
}
