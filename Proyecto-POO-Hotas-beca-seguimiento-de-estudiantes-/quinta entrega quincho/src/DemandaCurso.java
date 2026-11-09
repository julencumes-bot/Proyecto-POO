import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Resumen de solicitudes no canceladas de un curso, agrupadas por tema. */
public record DemandaCurso(Curso curso, int cantidad, Map<String, Integer> temas) {
    public DemandaCurso {
        temas = Collections.unmodifiableMap(new LinkedHashMap<>(temas));
    }
}
