import java.util.ArrayList;

public class Curso {
    private String codigo;
    private String nombre;
    private ArrayList<Tutor> tutoresDisponibles;

    public Curso(String codigo, String nombre) {
        this.codigo = Validacion.texto(codigo, "Codigo");
        this.nombre = Validacion.texto(nombre, "Nombre del curso");
        tutoresDisponibles = new ArrayList<Tutor>();
    }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public ArrayList<Tutor> getTutoresDisponibles() {
        return new ArrayList<Tutor>(tutoresDisponibles);
    }
    public void agregarTutor(Tutor tutor) {
        if (tutor == null) { throw new IllegalArgumentException("Tutor requerido."); }
        if (!tutoresDisponibles.contains(tutor)) { tutoresDisponibles.add(tutor); }
    }
    public String toString() { return codigo + " - " + nombre; }
}
