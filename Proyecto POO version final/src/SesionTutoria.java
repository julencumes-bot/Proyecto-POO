public class SesionTutoria {
    private int idSesion;
    private SolicitudTutoria solicitud;
    private String temasVistos;
    private String notas;
    private String temaProximaTutoria;
    private double duracionHoras;
    private boolean finalizada;
    private boolean ausente;

    public boolean isAusente() { return ausente; }
    public void registrarAusencia() {
        if (finalizada || solicitud.getEstado() != EstadoSolicitud.CONFIRMADA) {
            throw new IllegalStateException("La sesión debe estar confirmada.");
        }
        solicitud.cerrarPorInasistencia();
        ausente = true;
    }
    void reabrirTrasCorreccion() {
        solicitud.reabrirTrasCorreccion();
        ausente = false;
    }
    public double getHorasAcreditadas() {
        return finalizada && solicitud.isRecibeHorasBeca()
                ? duracionHoras * solicitud.getMultiplicadorBeca() : 0;
    }

    public SesionTutoria(int id, SolicitudTutoria solicitud) {
        Validacion.positivo(id, "Identificador");
        if (solicitud == null || solicitud.getEstado() != EstadoSolicitud.CONFIRMADA) {
            throw new IllegalArgumentException("Se necesita una solicitud confirmada.");
        }
        idSesion = id;
        this.solicitud = solicitud;
        temasVistos = "";
        notas = "";
        temaProximaTutoria = "";
    }
    public int getIdSesion() { return idSesion; }
    public SolicitudTutoria getSolicitud() { return solicitud; }
    public String getTemasVistos() { return temasVistos; }
    public String getNotas() { return notas; }
    public String getTemaProximaTutoria() { return temaProximaTutoria; }
    public double getDuracionHoras() { return duracionHoras; }
    public boolean isFinalizada() { return finalizada; }
    public void finalizar(String temas, String notas, String proximoTema) {
        finalizar(temas, notas, proximoTema, solicitud.getHorario().getDuracionHoras());
    }
    public void finalizar(String temas, String notas, String proximoTema, double duracionReal) {
        if (!Double.isFinite(duracionReal) || duracionReal <= 0 || duracionReal > 24) {
            throw new IllegalArgumentException("La duración real debe ser mayor que 0 y hasta 24 horas.");
        }
        if (finalizada || solicitud.getEstado() != EstadoSolicitud.CONFIRMADA) {
            throw new IllegalStateException("Esta sesion ya esta cerrada.");
        }
        String temasValidos = Validacion.texto(temas, "Temas vistos");
        String notasValidas = Validacion.texto(notas, "Notas");
        if (proximoTema == null) { throw new IllegalArgumentException("Tema proximo invalido."); }
        temasVistos = temasValidos;
        this.notas = notasValidas;
        temaProximaTutoria = proximoTema.trim();
        duracionHoras = duracionReal;
        solicitud.finalizar();
        finalizada = true;
    }
    public String toString() {
        return "Sesion " + idSesion + " | " + solicitud + "\nTemas vistos: " + temasVistos
                + "\nNotas: " + notas + "\nTema proximo: " + temaProximaTutoria
                + "\nDuracion registrada: " + duracionHoras + " horas";
    }
}
