import java.time.LocalDate;

/** Suspensión temporal por rol. Revocarla conserva fechas y origen para auditoría. */
public record Sancion(int id, int reporte, int decision, String usuario, String rol,
        String periodo, String inicio, String fin, String motivo, boolean vigente,
        String revisionFecha, String revisionMotivo) {
    public boolean contiene(LocalDate dia) {
        return vigente && !dia.isBefore(LocalDate.parse(inicio)) && !dia.isAfter(LocalDate.parse(fin));
    }
    public Sancion revisar(boolean aplicable, String fecha, String razon) {
        if (vigente == aplicable) return this;
        return new Sancion(id, reporte, decision, usuario, rol, periodo, inicio, fin,
                motivo, aplicable, fecha, razon);
    }
}
