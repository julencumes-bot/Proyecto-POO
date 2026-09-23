import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Entrada entrada = new Entrada(scanner);
        SistemaTutorias sistema = crearDemostracion();
        EstudianteController estudiantes = new EstudianteController(sistema);
        TutorController tutores = new TutorController(sistema);
        CoordinadorController coordinador = new CoordinadorController(sistema);
        System.out.println("SISTEMA DE GESTION DE ASESORIAS ACADEMICAS");
        System.out.println("Prototipo de consola. Datos de ejemplo; los cambios se pierden al salir.");
        System.out.println("Seleccione un perfil para la demostracion. No es un inicio de sesion con contrasena.");
        try {
            int opcion;
            do {
                System.out.println("\n1. Estudiante\n2. Tutor\n3. Profesor o coordinador\n0. Salir");
                opcion = entrada.leerEntero("Perfil: ", 0, 3);
                switch (opcion) {
                    case 1:
                        ArrayList<Estudiante> listaEstudiantes = sistema.getEstudiantes();
                        for (int i = 0; i < listaEstudiantes.size(); i++) {
                            System.out.println((i + 1) + ". " + listaEstudiantes.get(i));
                        }
                        int indiceEstudiante = entrada.elegirIndice(listaEstudiantes.size());
                        if (indiceEstudiante != -1) {
                            EstudianteView vista = new EstudianteView(estudiantes, coordinador,
                                    entrada, listaEstudiantes.get(indiceEstudiante));
                            vista.mostrarMenu();
                        }
                        break;
                    case 2:
                        ArrayList<Tutor> listaTutores = new ArrayList<Tutor>();
                        for (Tutor tutor : sistema.getTutores()) {
                            if (tutor != sistema.getCoordinador().getPerfilTutor()) { listaTutores.add(tutor); }
                        }
                        for (int i = 0; i < listaTutores.size(); i++) {
                            System.out.println((i + 1) + ". " + listaTutores.get(i));
                        }
                        int indiceTutor = entrada.elegirIndice(listaTutores.size());
                        if (indiceTutor != -1) {
                            TutorView vista = new TutorView(tutores, entrada, listaTutores.get(indiceTutor));
                            vista.mostrarMenu();
                        }
                        break;
                    case 3:
                        CoordinadorView vista = new CoordinadorView(coordinador, tutores,
                                sistema.getCoordinador(), entrada);
                        vista.mostrarMenu();
                        break;
                    default: break;
                }
            } while (opcion != 0);
        } catch (NoSuchElementException error) {
            System.out.println("\nSe termino la entrada de consola.");
        } finally {
            scanner.close();
        }
        System.out.println("Programa finalizado.");
    }

    public static SistemaTutorias crearDemostracion() {
        ProfesorCoordinador profesor = new ProfesorCoordinador("Prof. Rivera", "P001", "rivera@example.com");
        SistemaTutorias sistema = new SistemaTutorias(profesor);
        Curso poo = new Curso("CC2006", "Programacion Orientada a Objetos");
        Curso mate = new Curso("MM101", "Matematica");
        Curso avanzado = new Curso("CC5001", "Curso avanzado de ultimo anio");
        sistema.agregarCurso(poo);
        sistema.agregarCurso(mate);
        sistema.agregarCurso(avanzado);
        sistema.agregarEstudiante(new Estudiante("Ana", "E001", "ana@example.com", 2));
        sistema.agregarEstudiante(new Estudiante("Luis", "E002", "luis@example.com", 5));
        Tutor maria = new Tutor("Maria", "T001", "maria@example.com", 3, true, 0);
        Tutor diego = new Tutor("Diego", "T002", "diego@example.com", 5, false, 40);
        sistema.agregarTutor(maria);
        sistema.agregarTutor(diego);
        profesor.aprobarTutor(maria, poo);
        profesor.aprobarTutor(diego, mate);
        profesor.aprobarTutor(diego, avanzado);
        profesor.aprobarTutor(profesor.getPerfilTutor(), avanzado);
        maria.agregarHorario(new Horario("2026-10-01", "14:00", "15:00"));
        maria.agregarHorario(new Horario("2026-10-02", "14:00", "15:30"));
        diego.agregarHorario(new Horario("2026-10-01", "14:00", "15:00"));
        diego.agregarHorario(new Horario("2026-10-03", "09:00", "10:30"));
        profesor.getPerfilTutor().agregarHorario(new Horario("2026-10-04", "10:00", "11:00"));
        return sistema;
    }
}
