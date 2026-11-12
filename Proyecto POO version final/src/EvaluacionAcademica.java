/** Evaluación docente independiente del promedio estudiantil. */
public record EvaluacionAcademica(int id, String tutor, String evaluador, String curso,
        String fecha, int dominio, int claridad, String fundamento) {
    public EvaluacionAcademica {
        Valoracion.validarPuntuacion(dominio);
        Valoracion.validarPuntuacion(claridad);
        fundamento = Validacion.texto(fundamento, "Fundamento");
        if (fundamento.length() > 2000) throw new IllegalArgumentException("Fundamento demasiado largo.");
        if (tutor.equals(evaluador)) throw new SecurityException("No puedes evaluar tu propio perfil docente.");
    }
}
