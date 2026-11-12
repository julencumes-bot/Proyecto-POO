/** Reporte privado. Su estado se obtiene de la última decisión auditada. */
public record ReporteAusencia(int id, int solicitud, String autor, String rolAutor,
        String reportado, String rolReportado, String periodo, String fechaSesion,
        String fecha, String motivo, String evidencia) { }
