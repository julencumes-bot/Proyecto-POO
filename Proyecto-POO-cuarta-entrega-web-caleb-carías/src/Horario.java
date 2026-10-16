public class Horario {
    private String fecha;
    private String horaInicio;
    private String horaFin;
    private boolean disponible;

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
                + (disponible ? " [Disponible]" : " [Reservado]");
    }
}
