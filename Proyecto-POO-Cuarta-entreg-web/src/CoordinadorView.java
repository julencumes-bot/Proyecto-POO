import java.util.ArrayList;

public class CoordinadorView {
    private CoordinadorController controlador;
    private TutorController tutorController;
    private ProfesorCoordinador coordinador;
    private Entrada entrada;

    public CoordinadorView(CoordinadorController controlador, TutorController tutorController,
            ProfesorCoordinador coordinador, Entrada entrada) {
        this.controlador = controlador;
        this.tutorController = tutorController;
        this.coordinador = coordinador;
        this.entrada = entrada;
    }
    public void mostrarMenu() {
        int opcion;
        do {
            System.out.println("\nCOORDINADOR: " + coordinador.getNombre());
            System.out.println("1. Consultar tutores e informacion\n2. Estudiantes que reciben tutorias"
                    + "\n3. Consultar progreso de un estudiante\n4. Consultar solicitudes y demanda"
                    + "\n5. Historial general\n6. Aprobar tutor para un curso"
                    + "\n7. Registrar estudiante\n8. Registrar tutor\n9. Registrar curso"
                    + "\n10. Impartir tutorias como profesor\n11. Consultar todos los estudiantes\n0. Volver");
            opcion = entrada.leerEntero("Opcion: ", 0, 11);
            try {
                switch (opcion) {
                    case 1:
                        for (Tutor tutor : controlador.consultarTutores()) {
                            System.out.println(tutor);
                            System.out.println("Cursos: " + tutor.getCursosAsignados());
                            System.out.println("Horarios: " + tutor.getDisponibilidad());
                            mostrarHistorial(tutor.getHistorialTutorias());
                        }
                        break;
                    case 2: mostrarEstudiantes(controlador.consultarEstudiantesConTutorias()); break;
                    case 3:
                        ArrayList<Estudiante> estudiantes = controlador.consultarEstudiantes();
                        mostrarEstudiantes(estudiantes);
                        int indice = entrada.elegirIndice(estudiantes.size());
                        if (indice != -1) { mostrarHistorial(controlador.consultarProgreso(estudiantes.get(indice))); }
                        break;
                    case 4:
                        ArrayList<SolicitudTutoria> solicitudes = controlador.consultarSolicitudes();
                        if (solicitudes.isEmpty()) { System.out.println("No hay solicitudes."); }
                        for (SolicitudTutoria solicitud : solicitudes) { System.out.println(solicitud); }
                        System.out.println(controlador.consultarDemanda());
                        break;
                    case 5: mostrarHistorial(controlador.consultarHistorial()); break;
                    case 6: aprobarTutor(); break;
                    case 7:
                        controlador.registrarEstudiante(entrada.leerTexto("Nombre: "), entrada.leerTexto("Carne: "),
                                entrada.leerTexto("Correo: "), entrada.leerEntero("Anio de carrera: ", 1, 20));
                        System.out.println("Estudiante registrado.");
                        break;
                    case 8: registrarTutor(); break;
                    case 9:
                        controlador.registrarCurso(entrada.leerTexto("Codigo: "), entrada.leerTexto("Nombre del curso: "));
                        System.out.println("Curso registrado. Apruebe un tutor y agregue horarios para ofrecerlo.");
                        break;
                    case 10:
                        TutorView vista = new TutorView(tutorController, entrada, coordinador.getPerfilTutor());
                        vista.mostrarMenu();
                        break;
                    case 11: mostrarEstudiantes(controlador.consultarEstudiantes()); break;
                    default: break;
                }
            } catch (IllegalArgumentException error) {
                System.out.println("No se pudo realizar la operacion: " + error.getMessage());
            } catch (IllegalStateException error) {
                System.out.println("No se pudo realizar la operacion: " + error.getMessage());
            }
        } while (opcion != 0);
    }
    private void aprobarTutor() {
        ArrayList<Tutor> tutores = controlador.consultarTutores();
        for (int i = 0; i < tutores.size(); i++) { System.out.println((i + 1) + ". " + tutores.get(i)); }
        int indiceTutor = entrada.elegirIndice(tutores.size());
        if (indiceTutor == -1) { return; }
        ArrayList<Curso> cursos = controlador.consultarCursos();
        for (int i = 0; i < cursos.size(); i++) { System.out.println((i + 1) + ". " + cursos.get(i)); }
        int indiceCurso = entrada.elegirIndice(cursos.size());
        if (indiceCurso == -1) { return; }
        controlador.aprobarTutor(tutores.get(indiceTutor), cursos.get(indiceCurso));
        System.out.println("Tutor aprobado para el curso seleccionado.");
    }
    private void registrarTutor() {
        String nombre = entrada.leerTexto("Nombre: ");
        String carnet = entrada.leerTexto("Carne: ");
        String correo = entrada.leerTexto("Correo: ");
        int anio = entrada.leerEntero("Anio de carrera (egresado: ultimo anio cursado): ", 1, 20);
        boolean beca = entrada.leerSiNo("Recibe horas beca");
        double tarifa = 0;
        if (!beca) { tarifa = entrada.leerDecimal("Tarifa por hora en quetzales (0 si es gratuito): "); }
        controlador.registrarTutor(nombre, carnet, correo, anio, beca, tarifa);
        System.out.println("Tutor registrado. Falta aprobar sus cursos y agregar sus horarios.");
    }
    public void mostrarEstudiantes(ArrayList<Estudiante> estudiantes) {
        if (estudiantes.isEmpty()) { System.out.println("No hay estudiantes para mostrar."); }
        for (int i = 0; i < estudiantes.size(); i++) { System.out.println((i + 1) + ". " + estudiantes.get(i)); }
    }
    public void mostrarHistorial(ArrayList<SesionTutoria> sesiones) {
        if (sesiones.isEmpty()) { System.out.println("No hay tutorias finalizadas."); }
        for (SesionTutoria sesion : sesiones) { System.out.println(sesion); }
    }
}
