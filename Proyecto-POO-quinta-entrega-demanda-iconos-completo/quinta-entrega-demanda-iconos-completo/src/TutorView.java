import java.util.ArrayList;

public class TutorView {
    private TutorController controlador;
    private Entrada entrada;
    private Tutor tutor;

    public TutorView(TutorController controlador, Entrada entrada, Tutor tutor) {
        this.controlador = controlador;
        this.entrada = entrada;
        this.tutor = tutor;
    }
    public void mostrarMenu() {
        int opcion;
        do {
            System.out.println("\nTUTOR: " + tutor.getNombre());
            System.out.println("1. Cursos asignados\n2. Consultar solicitudes, temas y horarios"
                    + "\n3. Aceptar solicitud\n4. Cancelar solicitud\n5. Sesiones por realizar"
                    + "\n6. Finalizar sesion y registrar seguimiento\n7. Historial"
                    + "\n8. Pago y horas beca\n9. Consultar preguntas\n10. Agregar horario"
                    + "\n11. Consultar disponibilidad\n0. Volver");
            opcion = entrada.leerEntero("Opcion: ", 0, 11);
            try {
                switch (opcion) {
                    case 1:
                        ArrayList<Curso> cursos = controlador.consultarCursosAsignados(tutor);
                        if (cursos.isEmpty()) { System.out.println("Aun no tiene cursos aprobados."); }
                        for (Curso curso : cursos) { System.out.println(curso); }
                        break;
                    case 2: mostrarSolicitudes(controlador.consultarSolicitudes(tutor)); break;
                    case 3:
                        SolicitudTutoria aceptar = elegirSolicitud();
                        if (aceptar != null) {
                            System.out.println("Sesion creada:\n" + controlador.aceptarTutoria(tutor, aceptar));
                        }
                        break;
                    case 4:
                        SolicitudTutoria cancelar = elegirSolicitud();
                        if (cancelar != null) {
                            controlador.cancelarTutoria(tutor, cancelar);
                            System.out.println("Solicitud cancelada.");
                        }
                        break;
                    case 5: mostrarHistorial(controlador.consultarSesionesPendientes(tutor)); break;
                    case 6: finalizarSesion(); break;
                    case 7: mostrarHistorial(controlador.consultarHistorial(tutor)); break;
                    case 8:
                        System.out.println("Compensacion acumulada por tutorias finalizadas: Q"
                                + controlador.consultarPago(tutor));
                        System.out.println("Horas beca acumuladas: " + controlador.consultarHorasBeca(tutor));
                        System.out.println("Es un registro de lo generado; no procesa pagos bancarios.");
                        break;
                    case 9:
                        ArrayList<Pregunta> preguntas = controlador.consultarPreguntas(tutor);
                        if (preguntas.isEmpty()) { System.out.println("No hay preguntas registradas."); }
                        for (Pregunta pregunta : preguntas) { System.out.println(pregunta); }
                        break;
                    case 10:
                        controlador.agregarHorario(tutor, entrada.leerTexto("Fecha AAAA-MM-DD: "),
                                entrada.leerTexto("Hora inicial HH:mm: "), entrada.leerTexto("Hora final HH:mm: "));
                        System.out.println("Horario agregado.");
                        break;
                    case 11:
                        if (tutor.getDisponibilidad().isEmpty()) { System.out.println("No tiene horarios."); }
                        for (Horario horario : tutor.getDisponibilidad()) { System.out.println(horario); }
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
    private SolicitudTutoria elegirSolicitud() {
        ArrayList<SolicitudTutoria> solicitudes = controlador.consultarSolicitudes(tutor);
        mostrarSolicitudes(solicitudes);
        int indice = entrada.elegirIndice(solicitudes.size());
        if (indice == -1) { return null; }
        return solicitudes.get(indice);
    }
    private void finalizarSesion() {
        ArrayList<SesionTutoria> sesiones = controlador.consultarSesionesPendientes(tutor);
        mostrarHistorial(sesiones);
        int indice = entrada.elegirIndice(sesiones.size());
        if (indice == -1) { return; }
        String temas = entrada.leerTexto("Temas vistos: ");
        String notas = entrada.leerTexto("Notas sobre el trabajo realizado: ");
        String proximo = entrada.leerOpcional("Tema de proxima tutoria (Enter si no se definio): ");
        controlador.finalizarSesion(tutor, sesiones.get(indice), temas, notas, proximo);
        System.out.println("Sesion finalizada. Historial y compensacion actualizados.");
    }
    public void mostrarSolicitudes(ArrayList<SolicitudTutoria> solicitudes) {
        if (solicitudes.isEmpty()) { System.out.println("No hay solicitudes."); }
        for (int i = 0; i < solicitudes.size(); i++) { System.out.println((i + 1) + ". " + solicitudes.get(i)); }
    }
    public void mostrarHistorial(ArrayList<SesionTutoria> sesiones) {
        if (sesiones.isEmpty()) { System.out.println("No hay sesiones para mostrar."); }
        for (int i = 0; i < sesiones.size(); i++) { System.out.println((i + 1) + ". " + sesiones.get(i)); }
    }
}
