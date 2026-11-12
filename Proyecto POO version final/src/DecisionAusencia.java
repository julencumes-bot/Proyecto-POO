/** Registro inmutable: una corrección agrega una decisión sin borrar la anterior. */
public record DecisionAusencia(int id, int reporte, String estado, String coordinador,
        String fecha, String motivo, String inicioPropuesto, String finPropuesto,
        String politicaEstudiante, String politicaTutor) { }
