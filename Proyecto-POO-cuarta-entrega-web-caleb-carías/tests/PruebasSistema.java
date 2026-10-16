import java.util.ArrayList;
import java.util.Scanner;

/** Pruebas de los requisitos sin bibliotecas externas. Ejecutar con java -cp bin PruebasSistema. */
public class PruebasSistema {
    private static int comprobaciones = 0;

    public static void main(String[] args) {
        probarFlujoHorasBeca();
        probarPago();
        probarCancelaciones();
        probarCrucesYPermisos();
        probarAprobacionYProfesor();
        probarValidaciones();
        probarEntrada();
        System.out.println("CORRECTO: " + comprobaciones + " comprobaciones superadas.");
    }

    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) { throw new IllegalStateException("FALLO: " + mensaje); }
        comprobaciones++;
    }

    private static void probarFlujoHorasBeca() {
        SistemaTutorias sistema = Main.crearDemostracion();
        EstudianteController ec = new EstudianteController(sistema);
        TutorController tc = new TutorController(sistema);
        CoordinadorController cc = new CoordinadorController(sistema);
        Estudiante ana = sistema.getEstudiantes().get(0);
        Tutor maria = sistema.getTutores().get(1);
        Curso poo = sistema.getCursos().get(0);
        Horario horario = maria.getDisponibilidad().get(0);
        verificar(ec.consultarCursos().size() == 3, "Oferta inicial de cursos");
        verificar(ec.consultarTutores(poo).get(0) == maria, "Tutor aprobado por curso");
        verificar(ec.consultarPrecio(maria, poo, horario) == 0, "Precio cero por horas beca");
        SolicitudTutoria solicitud = ec.solicitarTutoria(ana, poo, maria, "Clases", horario);
        verificar(solicitud.getEstado() == EstadoSolicitud.PENDIENTE, "Solicitud pendiente");
        verificar(!horario.isDisponible(), "Reserva inmediata");
        verificar(ec.consultarHorarios(maria).size() == 1, "Ocultar bloque reservado");
        verificar(ana.getSolicitudes().get(0) == tc.consultarSolicitudes(maria).get(0), "Misma referencia entre roles");
        ec.registrarPregunta(ana, solicitud, "Como funciona un constructor?");
        verificar(tc.consultarPreguntas(maria).size() == 1, "Tutor recibe pregunta");
        SesionTutoria sesion = tc.aceptarTutoria(maria, solicitud);
        verificar(solicitud.getEstado() == EstadoSolicitud.CONFIRMADA, "Solicitud confirmada");
        verificar(tc.consultarSesionesPendientes(maria).size() == 1, "Sesion por realizar");
        verificar(ana.getHistorialTutorias().isEmpty(), "Historial excluye sesiones por realizar");
        verificar(cc.consultarEstudiantesConTutorias().size() == 1, "Estudiante atendido");
        boolean rechazo = false;
        try { tc.aceptarTutoria(maria, solicitud); }
        catch (IllegalStateException error) { rechazo = true; }
        verificar(rechazo && sistema.getSesiones().size() == 1, "No duplicar aceptacion");
        rechazo = false;
        try { tc.finalizarSesion(maria, sesion, "Clases", " ", "Herencia"); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && !sesion.isFinalizada() && sesion.getTemasVistos().isEmpty(),
                "Validar seguimiento antes de modificar la sesion");
        tc.finalizarSesion(maria, sesion, "Clases y objetos", "Completo los ejercicios", "Herencia");
        verificar(solicitud.getEstado() == EstadoSolicitud.FINALIZADA, "Solicitud finalizada");
        verificar(ec.consultarHistorial(ana).get(0) == tc.consultarHistorial(maria).get(0), "Historial compartido");
        verificar(cc.consultarProgreso(ana).get(0).getNotas().equals("Completo los ejercicios"), "Seguimiento del coordinador");
        verificar(sesion.getTemaProximaTutoria().equals("Herencia"), "Tema proximo");
        verificar(tc.consultarHorasBeca(maria) == 1 && tc.consultarPago(maria) == 0, "Acreditacion exclusiva de beca");
        verificar(tc.consultarSesionesPendientes(maria).isEmpty(), "Retirar sesion de pendientes");
        verificar(cc.consultarHistorial().size() == 1, "Historial general");
        rechazo = false;
        try { tc.finalizarSesion(maria, sesion, "Otro", "Otra nota", ""); }
        catch (IllegalStateException error) { rechazo = true; }
        verificar(rechazo && tc.consultarHorasBeca(maria) == 1, "No duplicar horas");
        rechazo = false;
        try { ec.cancelarTutoria(ana, solicitud); }
        catch (IllegalStateException error) { rechazo = true; }
        verificar(rechazo && !horario.isDisponible(), "No cancelar sesion finalizada");
        rechazo = false;
        try { ec.registrarPregunta(ana, solicitud, "Otra duda"); }
        catch (IllegalStateException error) { rechazo = true; }
        verificar(rechazo && sistema.getPreguntas().size() == 1, "No preguntar en solicitud cerrada");
        ArrayList<SolicitudTutoria> copia = ana.getSolicitudes();
        copia.clear();
        verificar(ana.getSolicitudes().size() == 1, "Una copia no elimina solicitudes internas");
    }

    private static void probarPago() {
        SistemaTutorias sistema = Main.crearDemostracion();
        EstudianteController ec = new EstudianteController(sistema);
        TutorController tc = new TutorController(sistema);
        Estudiante ana = sistema.getEstudiantes().get(0);
        Tutor diego = sistema.getTutores().get(2);
        Curso mate = sistema.getCursos().get(1);
        Horario horario = diego.getDisponibilidad().get(1);
        SolicitudTutoria solicitud = ec.solicitarTutoria(ana, mate, diego, "Funciones", horario);
        verificar(solicitud.getPrecio() == 60, "Precio de hora y media a Q40");
        SesionTutoria sesion = tc.aceptarTutoria(diego, solicitud);
        verificar(tc.consultarPago(diego) == 0, "Aceptar no genera pago");
        tc.finalizarSesion(diego, sesion, "Funciones", "Resuelve funciones", "");
        verificar(sesion.getDuracionHoras() == 1.5, "Duracion fraccionaria");
        verificar(tc.consultarPago(diego) == 60 && tc.consultarHorasBeca(diego) == 0, "Pago sin horas beca");
        boolean rechazo = false;
        try { tc.finalizarSesion(diego, sesion, "Repetido", "Repetido", ""); }
        catch (IllegalStateException error) { rechazo = true; }
        verificar(rechazo && tc.consultarPago(diego) == 60, "No duplicar pago");
    }

    private static void probarCancelaciones() {
        SistemaTutorias sistema = Main.crearDemostracion();
        EstudianteController ec = new EstudianteController(sistema);
        TutorController tc = new TutorController(sistema);
        Estudiante ana = sistema.getEstudiantes().get(0);
        Tutor maria = sistema.getTutores().get(1);
        Curso poo = sistema.getCursos().get(0);
        Horario horario = maria.getDisponibilidad().get(0);
        SolicitudTutoria primera = ec.solicitarTutoria(ana, poo, maria, "Clases", horario);
        ec.cancelarTutoria(ana, primera);
        verificar(horario.isDisponible() && primera.getEstado() == EstadoSolicitud.CANCELADA, "Cancelar libera horario");
        SolicitudTutoria segunda = ec.solicitarTutoria(ana, poo, maria, "Metodos", horario);
        SesionTutoria cancelada = tc.aceptarTutoria(maria, segunda);
        tc.cancelarTutoria(maria, segunda);
        verificar(tc.consultarSesionesPendientes(maria).isEmpty(), "Sesion cancelada no aparece pendiente");
        boolean rechazo = false;
        try { tc.finalizarSesion(maria, cancelada, "Metodos", "Notas", ""); }
        catch (IllegalStateException error) { rechazo = true; }
        verificar(rechazo && tc.consultarHorasBeca(maria) == 0, "Cancelacion no genera horas");
        SolicitudTutoria tercera = ec.solicitarTutoria(ana, poo, maria, "Objetos", horario);
        verificar(tercera.getIdSolicitud() == 3, "IDs no se reutilizan al cancelar");
        rechazo = false;
        try { ec.cancelarTutoria(ana, primera); }
        catch (IllegalStateException error) { rechazo = true; }
        verificar(rechazo && !horario.isDisponible(), "Cancelacion repetida no libera reserva nueva");
        String demanda = new CoordinadorController(sistema).consultarDemanda();
        verificar(demanda.contains("objetos: 1") && !demanda.contains("clases: 1"), "Demanda excluye canceladas");
    }

    private static void probarCrucesYPermisos() {
        SistemaTutorias sistema = Main.crearDemostracion();
        EstudianteController ec = new EstudianteController(sistema);
        TutorController tc = new TutorController(sistema);
        Estudiante ana = sistema.getEstudiantes().get(0);
        Estudiante luis = sistema.getEstudiantes().get(1);
        Tutor maria = sistema.getTutores().get(1);
        Tutor diego = sistema.getTutores().get(2);
        Curso poo = sistema.getCursos().get(0);
        Curso mate = sistema.getCursos().get(1);
        Horario horario = maria.getDisponibilidad().get(0);
        SolicitudTutoria solicitud = ec.solicitarTutoria(ana, poo, maria, "POO", horario);
        boolean rechazo = false;
        try { ec.solicitarTutoria(luis, poo, maria, "POO", horario); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "No reservar dos veces al tutor");
        rechazo = false;
        try { ec.solicitarTutoria(ana, mate, diego, "Mate", diego.getDisponibilidad().get(0)); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && sistema.getSolicitudes().size() == 1, "Cruce del estudiante entre tutores");
        rechazo = false;
        try { tc.aceptarTutoria(diego, solicitud); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && solicitud.getEstado() == EstadoSolicitud.PENDIENTE, "Tutor ajeno no acepta");
        rechazo = false;
        try { ec.cancelarTutoria(luis, solicitud); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && !horario.isDisponible(), "Estudiante ajeno no cancela");
        rechazo = false;
        try { ec.registrarPregunta(luis, solicitud, "Pregunta ajena"); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && sistema.getPreguntas().isEmpty(), "Estudiante ajeno no pregunta");
        SesionTutoria sesion = tc.aceptarTutoria(maria, solicitud);
        rechazo = false;
        try { tc.finalizarSesion(diego, sesion, "Temas", "Notas", ""); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && !sesion.isFinalizada(), "Tutor ajeno no finaliza");
        rechazo = false;
        try { ec.solicitarTutoria(luis, mate, maria, "Mate", maria.getDisponibilidad().get(1)); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "Tutor sin aprobacion en ese curso");
        rechazo = false;
        try { ec.solicitarTutoria(luis, poo, maria, "POO", new Horario("2026-10-09", "10:00", "11:00")); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "Horario ajeno a la disponibilidad");
        Estudiante mariaEstudiante = new Estudiante("Maria", "T001", "maria@example.com", 3);
        sistema.agregarEstudiante(mariaEstudiante);
        rechazo = false;
        try { ec.solicitarTutoria(mariaEstudiante, mate, diego, "Mate", diego.getDisponibilidad().get(0)); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "Cruce entre rol estudiante y rol tutor de una persona");
        rechazo = false;
        try { ec.solicitarTutoria(mariaEstudiante, poo, maria, "POO", maria.getDisponibilidad().get(1)); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "No ser tutor de si mismo");
        ec.cancelarTutoria(ana, solicitud);
        verificar(!sistema.tieneConflicto(ana.getCarnet(), horario), "Cancelacion elimina conflicto");
        rechazo = false;
        try { ec.solicitarTutoria(ana, poo, maria, " ", horario); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && horario.isDisponible(), "Tema vacio no reserva horario");
    }

    private static void probarAprobacionYProfesor() {
        SistemaTutorias sistema = Main.crearDemostracion();
        CoordinadorController cc = new CoordinadorController(sistema);
        EstudianteController ec = new EstudianteController(sistema);
        TutorController tc = new TutorController(sistema);
        cc.registrarCurso("NUEVO", "Nuevo curso");
        Curso nuevo = sistema.getCursos().get(3);
        cc.registrarTutor("Elena", "T003", "elena@example.com", 4, true, 0);
        Tutor elena = sistema.getTutores().get(3);
        tc.agregarHorario(elena, "2026-10-07", "10:00", "11:00");
        verificar(!ec.consultarCursos().contains(nuevo), "Curso sin tutor aprobado no se ofrece");
        cc.aprobarTutor(elena, nuevo);
        cc.aprobarTutor(elena, nuevo);
        verificar(ec.consultarTutores(nuevo).size() == 1 && elena.getCursosAsignados().size() == 1,
                "Aprobacion conecta curso y tutor sin duplicados");
        verificar(ec.consultarCursos().contains(nuevo), "Curso aprobado aparece en oferta");
        Estudiante ana = sistema.getEstudiantes().get(0);
        verificar(cc.buscarTutorAlternativo(nuevo, ana) == elena, "Encontrar tutor de anio superior");
        Estudiante luis = sistema.getEstudiantes().get(1);
        Curso avanzado = sistema.getCursos().get(2);
        Tutor diego = sistema.getTutores().get(2);
        Tutor profesor = sistema.getCoordinador().getPerfilTutor();
        verificar(cc.buscarTutorAlternativo(avanzado, luis) == diego, "Preferir par antes del profesor");
        for (Horario horario : diego.getDisponibilidad()) {
            ec.solicitarTutoria(ana, avanzado, diego, "Tema avanzado", horario);
        }
        verificar(cc.buscarTutorAlternativo(avanzado, luis) == profesor, "Profesor cuando los pares no tienen horario");
        SolicitudTutoria solicitud = ec.solicitarTutoria(luis, avanzado, profesor, "Duda avanzada",
                profesor.getDisponibilidad().get(0));
        SesionTutoria sesion = tc.aceptarTutoria(profesor, solicitud);
        tc.finalizarSesion(profesor, sesion, "Tema avanzado", "Duda resuelta", "");
        verificar(sistema.getCoordinador().getTutoriasImpartidas().size() == 1, "Historial del profesor");
        verificar(tc.consultarPago(profesor) == 0 && tc.consultarHorasBeca(profesor) == 0,
                "Profesor gratuito sin beca");
        verificar(cc.buscarTutorAlternativo(avanzado, luis) == null, "Sin candidatos disponibles");
        boolean rechazo = false;
        try { cc.registrarCurso("nuevo", "Duplicado"); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && sistema.getCursos().size() == 4, "Codigo de curso duplicado");
        rechazo = false;
        try { cc.registrarEstudiante("Otra Ana", "E001", "otra@example.com", 2); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && sistema.getEstudiantes().size() == 2, "Carne estudiante duplicado");
        rechazo = false;
        try { cc.registrarTutor("Otra Elena", "T003", "otra@example.com", 4, true, 0); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && sistema.getTutores().size() == 4, "Carne tutor duplicado");
    }

    private static void probarValidaciones() {
        Horario bisiesto = new Horario("2028-02-29", "09:30", "10:45");
        verificar(bisiesto.getDuracionHoras() == 1.25, "Fecha bisiesta y minutos");
        verificar(!bisiesto.seCruzaCon(new Horario("2028-02-29", "10:45", "11:30")), "Bloques contiguos permitidos");
        verificar(bisiesto.seCruzaCon(new Horario("2028-02-29", "10:30", "11:30")), "Cruce parcial");
        verificar(!bisiesto.seCruzaCon(new Horario("2028-03-01", "09:30", "10:45")), "Fechas distintas");
        String[] fechasMalas = {"2026-02-29", "1900-02-29", "2026-04-31", "2026-13-01", "2026-01-00", "0000-01-01", "2026-1-01", "abcd-ef-gh"};
        for (String fecha : fechasMalas) {
            boolean rechazo = false;
            try { new Horario(fecha, "09:00", "10:00"); }
            catch (IllegalArgumentException error) { rechazo = true; }
            verificar(rechazo, "Rechazar fecha " + fecha);
        }
        String[] horasMalas = {"24:00", "09:60", "9:00", "-1:00", "ab:cd"};
        for (String hora : horasMalas) {
            boolean rechazo = false;
            try { new Horario("2026-10-01", hora, "23:59"); }
            catch (IllegalArgumentException error) { rechazo = true; }
            verificar(rechazo, "Rechazar hora " + hora);
        }
        boolean rechazo = false;
        try { new Horario("2026-10-01", "10:00", "09:00"); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "Rechazar intervalo invertido");
        rechazo = false;
        try { new Horario("2026-10-01", "10:00", "10:00"); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "Rechazar duracion cero");
        Tutor tutor = new Tutor("Tutor", "T", "t@example.com", 3, true, 0);
        tutor.agregarHorario(bisiesto);
        rechazo = false;
        try { tutor.agregarHorario(new Horario("2028-02-29", "10:00", "11:00")); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo && tutor.getDisponibilidad().size() == 1, "No crear disponibilidad superpuesta");
        tutor.agregarHorario(new Horario("2028-02-29", "10:45", "11:45"));
        verificar(tutor.getDisponibilidad().size() == 2, "Crear bloques contiguos");
        double[] tarifasMalas = {-1, Double.NaN, Double.POSITIVE_INFINITY};
        for (double tarifa : tarifasMalas) {
            rechazo = false;
            try { new Tutor("Tutor", "T", "t@example.com", 3, false, tarifa); }
            catch (IllegalArgumentException error) { rechazo = true; }
            verificar(rechazo, "Rechazar tarifa invalida");
        }
        rechazo = false;
        try { new Tutor("Tutor", "T", "t@example.com", 3, true, 25); }
        catch (IllegalArgumentException error) { rechazo = true; }
        verificar(rechazo, "Horas beca no permite tarifa");
    }

    private static void probarEntrada() {
        Scanner scanner = new Scanner("abc\n-1\n2\nNaN\n-4\n12.5\nx\ns\n\nAna\n0\n");
        Entrada entrada = new Entrada(scanner);
        verificar(entrada.leerEntero("Entero: ", 0, 3) == 2, "Recuperar entrada entera invalida");
        verificar(entrada.leerDecimal("Monto: ") == 12.5, "Recuperar entrada decimal invalida");
        verificar(entrada.leerSiNo("Confirmar"), "Validar respuesta s/n");
        verificar(entrada.leerTexto("Nombre: ").equals("Ana"), "Rechazar texto vacio");
        verificar(entrada.elegirIndice(2) == -1, "Volver desde seleccion");
        verificar(entrada.elegirIndice(0) == -1, "Lista vacia no pide indice");
        scanner.close();
    }
}
