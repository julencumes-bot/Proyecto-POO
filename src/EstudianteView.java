import java.util.ArrayList;

public class EstudianteView {
    private EstudianteController controlador;
    private CoordinadorController coordinadorController;
    private Entrada entrada;
    private Estudiante estudiante;

    public EstudianteView(EstudianteController controlador, CoordinadorController coordinadorController,
            Entrada entrada, Estudiante estudiante) {
        this.controlador = controlador;
        this.coordinadorController = coordinadorController;
        this.entrada = entrada;
        this.estudiante = estudiante;
    }
    public void mostrarMenu() {
        int opcion;
        do {
            System.out.println("\nESTUDIANTE: " + estudiante.getNombre());
            System.out.println("1. Consultar cursos, tutores, horarios y precios\n2. Solicitar tutoria"
                    + "\n3. Consultar solicitudes\n4. Cancelar solicitud\n5. Historial, notas y tema proximo"
                    + "\n6. Registrar pregunta\n7. Consultar mis preguntas\n8. Buscar tutor alternativo\n0. Volver");
            opcion = entrada.leerEntero("Opcion: ", 0, 8);
            try {
                switch (opcion) {
                    case 1: consultarOferta(); break;
                    case 2: solicitarTutoria(); break;
                    case 3: mostrarSolicitudes(controlador.consultarSolicitudes(estudiante)); break;
                    case 4:
                        SolicitudTutoria cancelar = elegirSolicitud();
                        if (cancelar != null) {
                            controlador.cancelarTutoria(estudiante, cancelar);
                            System.out.println("Solicitud cancelada. El horario queda libre.");
                        }
                        break;
                    case 5: mostrarHistorial(controlador.consultarHistorial(estudiante)); break;
                    case 6:
                        SolicitudTutoria solicitud = elegirSolicitud();
                        if (solicitud != null) {
                            System.out.println(controlador.registrarPregunta(estudiante, solicitud,
                                    entrada.leerTexto("Pregunta: ")));
                        }
                        break;
                    case 7:
                        ArrayList<Pregunta> preguntas = controlador.consultarPreguntas(estudiante);
                        if (preguntas.isEmpty()) { System.out.println("No tiene preguntas registradas."); }
                        for (Pregunta pregunta : preguntas) { System.out.println(pregunta); }
                        break;
                    case 8:
                        Curso curso = elegirCurso();
                        if (curso != null) {
                            Tutor tutor = coordinadorController.buscarTutorAlternativo(curso, estudiante);
                            if (tutor == null) { System.out.println("No hay un tutor con horario compatible."); }
                            else { System.out.println("Tutor sugerido: " + tutor); }
                        }
                        break;
                    default: break;
                }
            } catch (IllegalArgumentException error) {
                System.out.println("No se pudo realizar la operacion: " + error.getMessage());
            } catch (IllegalStateException error) {
                System.out.println("No se pudo realizar la operacion: " + error.getMessage());
            }
        } while (opcion != 0);
    }
    private Curso elegirCurso() {
        ArrayList<Curso> cursos = controlador.consultarCursos();
        for (int i = 0; i < cursos.size(); i++) { System.out.println((i + 1) + ". " + cursos.get(i)); }
        int indice = entrada.elegirIndice(cursos.size());
        if (indice == -1) { return null; }
        return cursos.get(indice);
    }
    private void consultarOferta() {
        Curso curso = elegirCurso();
        if (curso == null) { return; }
        for (Tutor tutor : controlador.consultarTutores(curso)) {
            System.out.println(tutor);
            for (Horario horario : controlador.consultarHorarios(tutor)) {
                System.out.println("  " + horario + " | Precio total Q"
                        + controlador.consultarPrecio(tutor, curso, horario));
            }
        }
    }
    private void solicitarTutoria() {
        Curso curso = elegirCurso();
        if (curso == null) { return; }
        ArrayList<Tutor> tutores = controlador.consultarTutores(curso);
        for (int i = 0; i < tutores.size(); i++) { System.out.println((i + 1) + ". " + tutores.get(i)); }
        int indiceTutor = entrada.elegirIndice(tutores.size());
        if (indiceTutor == -1) { return; }
        Tutor tutor = tutores.get(indiceTutor);
        ArrayList<Horario> horarios = controlador.consultarHorarios(tutor);
        for (int i = 0; i < horarios.size(); i++) { System.out.println((i + 1) + ". " + horarios.get(i)); }
        int indiceHorario = entrada.elegirIndice(horarios.size());
        if (indiceHorario == -1) { return; }
        Horario horario = horarios.get(indiceHorario);
        System.out.println("Precio total: Q" + controlador.consultarPrecio(tutor, curso, horario));
        String tema = entrada.leerTexto("Tema que desea trabajar: ");
        if (entrada.leerSiNo("Confirmar solicitud")) {
            System.out.println(controlador.solicitarTutoria(estudiante, curso, tutor, tema, horario));
        }
    }
    private SolicitudTutoria elegirSolicitud() {
        ArrayList<SolicitudTutoria> solicitudes = controlador.consultarSolicitudes(estudiante);
        mostrarSolicitudes(solicitudes);
        int indice = entrada.elegirIndice(solicitudes.size());
        if (indice == -1) { return null; }
        return solicitudes.get(indice);
    }
    public void mostrarSolicitudes(ArrayList<SolicitudTutoria> solicitudes) {
        if (solicitudes.isEmpty()) { System.out.println("No tiene solicitudes."); }
        for (int i = 0; i < solicitudes.size(); i++) {
            System.out.println((i + 1) + ". " + solicitudes.get(i));
        }
    }
    public void mostrarHistorial(ArrayList<SesionTutoria> sesiones) {
        if (sesiones.isEmpty()) { System.out.println("No hay tutorias finalizadas."); }
        for (SesionTutoria sesion : sesiones) { System.out.println(sesion); }
    }
}
