import java.util.ArrayList;

public class ProfesorCoordinador extends Usuario {
    private ArrayList<Tutor> tutoresSupervisados;
    private ArrayList<Curso> cursos;
    private Tutor perfilTutor;

    public ProfesorCoordinador(String nombre, String carnet, String correo) {
        super(nombre, carnet, correo);
        tutoresSupervisados = new ArrayList<Tutor>();
        cursos = new ArrayList<Curso>();
        // El perfil comparte la identidad del profesor; permite usar el flujo de tutorias.
        perfilTutor = new Tutor(nombre, carnet, correo, 1, false, 0);
    }
    public Tutor getPerfilTutor() { return perfilTutor; }
    public ArrayList<Tutor> getTutoresSupervisados() {
        return new ArrayList<Tutor>(tutoresSupervisados);
    }
    public ArrayList<Curso> getCursos() { return new ArrayList<Curso>(cursos); }
    public ArrayList<SesionTutoria> getTutoriasImpartidas() {
        return perfilTutor.getHistorialTutorias();
    }
    public void agregarCurso(Curso curso) {
        if (curso == null) { throw new IllegalArgumentException("Curso requerido."); }
        if (!cursos.contains(curso)) { cursos.add(curso); }
    }
    public void aprobarTutor(Tutor tutor, Curso curso) {
        if (tutor == null || !cursos.contains(curso)) {
            throw new IllegalArgumentException("El curso debe estar a cargo del coordinador.");
        }
        tutor.agregarCurso(curso);
        curso.agregarTutor(tutor);
        tutor.setAprobado(true);
        if (!tutoresSupervisados.contains(tutor)) { tutoresSupervisados.add(tutor); }
    }
}
