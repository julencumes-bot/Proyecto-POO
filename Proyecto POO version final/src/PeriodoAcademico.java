import java.time.LocalDate;

/** Fechas inclusivas definidas por coordinación, nunca semestres de 180 días. */
public record PeriodoAcademico(String id, LocalDate inicio, LocalDate fin) {
    public PeriodoAcademico {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,80}"))
            throw new IllegalArgumentException("Identificador de semestre inválido.");
        if (inicio == null || fin == null || inicio.getYear() < 1 || fin.getYear() > 9999 || fin.isBefore(inicio))
            throw new IllegalArgumentException("Fechas de semestre inválidas.");
    }
    public boolean contiene(LocalDate dia) { return !dia.isBefore(inicio) && !dia.isAfter(fin); }
}
