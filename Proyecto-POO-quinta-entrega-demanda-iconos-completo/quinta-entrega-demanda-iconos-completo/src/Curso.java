import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

public class Curso {
    private String codigo;
    private String nombre;
    private final String icono;
    private ArrayList<Tutor> tutoresDisponibles;
    private static final Set<String> ICONOS = Set.of(
            "book", "code", "math", "nodes", "atom", "flask", "leaf",
            "globe", "landmark", "palette", "music", "money");

    public Curso(String codigo, String nombre) {
        this(codigo, nombre, null);
    }
    public Curso(String codigo, String nombre, String icono) {
        this.codigo = Validacion.texto(codigo, "Codigo");
        this.nombre = Validacion.texto(nombre, "Nombre del curso");
        // Los cursos anteriores conservan el icono que se sugería por su nombre.
        this.icono = icono == null ? sugerirIcono(this.nombre) : Validacion.texto(icono, "Icono");
        if (!ICONOS.contains(this.icono)) {
            throw new IllegalArgumentException("Selecciona un icono válido para el curso.");
        }
        tutoresDisponibles = new ArrayList<Tutor>();
    }
    private static String sugerirIcono(String nombre) {
        String texto = nombre.toLowerCase(Locale.ROOT);
        if (texto.matches(".*(matem|álgebra|algebra|cálculo|calculo).*")) { return "math"; }
        if (texto.matches(".*(algorit|estructura).*")) { return "nodes"; }
        if (texto.matches(".*(program|inform|comput).*")) { return "code"; }
        return "book";
    }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getIcono() { return icono; }
    public ArrayList<Tutor> getTutoresDisponibles() {
        return new ArrayList<Tutor>(tutoresDisponibles);
    }
    public void agregarTutor(Tutor tutor) {
        if (tutor == null) { throw new IllegalArgumentException("Tutor requerido."); }
        if (!tutoresDisponibles.contains(tutor)) { tutoresDisponibles.add(tutor); }
    }
    public String toString() { return codigo + " - " + nombre; }
}
