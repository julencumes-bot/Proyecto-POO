/** Opinión estudiantil: exactamente una por sesión y participante. */
public record Valoracion(int id, int sesion, String autor, String tutor, String fecha,
        int puntuacion, String comentario) {
    public Valoracion {
        Validacion.positivo(id, "Valoración");
        validarPuntuacion(puntuacion);
        comentario = comentario == null ? "" : comentario.trim();
        if (comentario.length() > 2000) throw new IllegalArgumentException("Comentario demasiado largo.");
    }
    public static void validarPuntuacion(int valor) {
        if (valor < 1 || valor > 5) throw new IllegalArgumentException("La puntuación debe estar entre 1 y 5.");
    }
}
