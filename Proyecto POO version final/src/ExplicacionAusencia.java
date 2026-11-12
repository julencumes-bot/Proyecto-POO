/** Descargo escrito por la persona reportada o registrado por coordinación. */
public record ExplicacionAusencia(int id, int reporte, String autor, String rolAutor,
        String fecha, String contenido) { }
