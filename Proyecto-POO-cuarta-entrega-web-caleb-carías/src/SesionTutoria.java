public class SesionTutoria {
    private int idSesion;
    private SolicitudTutoria solicitud;
    private String temasVistos;
    private String notas;
    private String temaProximaTutoria;
    private double duracionHoras;
    private boolean finalizada;

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
        if (finalizada || solicitud.getEstado() != EstadoSolicitud.CONFIRMADA) {
            throw new IllegalStateException("Esta sesion ya esta cerrada.");
        }
        String temasValidos = Validacion.texto(temas, "Temas vistos");
        String notasValidas = Validacion.texto(notas, "Notas");
        if (proximoTema == null) { throw new IllegalArgumentException("Tema proximo invalido."); }
        temasVistos = temasValidos;
        this.notas = notasValidas;
        temaProximaTutoria = proximoTema.trim();
        duracionHoras = solicitud.getHorario().getDuracionHoras();
        solicitud.finalizar();
        finalizada = true;
    }
    public String toString() {
        return "Sesion " + idSesion + " | " + solicitud + "\nTemas vistos: " + temasVistos
                + "\nNotas: " + notas + "\nTema proximo: " + temaProximaTutoria
                + "\nDuracion registrada: " + duracionHoras + " horas";
    }
}
