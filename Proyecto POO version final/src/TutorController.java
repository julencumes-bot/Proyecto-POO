import java.util.ArrayList;

public class TutorController {
    private SistemaTutorias sistema;

    public TutorController(SistemaTutorias sistema) { this.sistema = sistema; }
    public ArrayList<Curso> consultarCursosAsignados(Tutor tutor) {
        ArrayList<Curso> cursos = tutor.getCursosAsignados();
        cursos.removeIf(c -> !c.isActivo());
        return cursos;
    }
    public ArrayList<SolicitudTutoria> consultarSolicitudes(Tutor tutor) {
        ArrayList<SolicitudTutoria> resultado = new ArrayList<SolicitudTutoria>();
        for (SolicitudTutoria solicitud : sistema.getSolicitudes()) {
            if (solicitud.getTutor() == tutor) { resultado.add(solicitud); }
        }
        return resultado;
    }
    public SesionTutoria aceptarTutoria(Tutor tutor, SolicitudTutoria solicitud) {
        verificarSolicitud(tutor, solicitud);
        if (!tutor.isAprobado() || !solicitud.getCurso().isActivo()
                || !tutor.getCursosAsignados().contains(solicitud.getCurso())) {
            throw new IllegalArgumentException("El tutor no esta aprobado para este curso.");
        }
        sistema.getSeguimiento().verificarSesion(solicitud);
        solicitud.confirmar();
        SesionTutoria sesion = new SesionTutoria(sistema.getSesiones().size() + 1, solicitud);
        sistema.agregarSesion(sesion);
        return sesion;
    }
    public void cancelarTutoria(Tutor tutor, SolicitudTutoria solicitud) {
        verificarSolicitud(tutor, solicitud);
        sistema.getSeguimiento().verificarCierre(solicitud);
        solicitud.cancelar();
    }
    public ArrayList<SesionTutoria> consultarSesionesPendientes(Tutor tutor) {
        ArrayList<SesionTutoria> resultado = new ArrayList<SesionTutoria>();
        for (SesionTutoria sesion : sistema.getSesiones()) {
            if (sesion.getSolicitud().getTutor() == tutor
                    && sesion.getSolicitud().getEstado() == EstadoSolicitud.CONFIRMADA) {
                resultado.add(sesion);
            }
        }
        return resultado;
    }
    public void finalizarSesion(Tutor tutor, SesionTutoria sesion, String temas, String notas, String proximo) {
        finalizarSesion(tutor, sesion, temas, notas, proximo, sesion.getSolicitud().getHorario().getDuracionHoras());
    }
    public void finalizarSesion(Tutor tutor, SesionTutoria sesion, String temas, String notas,
            String proximo, double duracionReal) {
        if (!sistema.getSesiones().contains(sesion)) {
            throw new IllegalArgumentException("Sesion no registrada.");
        }
        verificarSolicitud(tutor, sesion.getSolicitud());
        sistema.getSeguimiento().verificarSesion(sesion.getSolicitud());
        sistema.getSeguimiento().verificarCierre(sesion.getSolicitud());
        sesion.finalizar(temas, notas, proximo, duracionReal);
        sesion.getSolicitud().getEstudiante().agregarTutoriaHistorial(sesion);
        tutor.agregarSesion(sesion);
    }
    public ArrayList<SesionTutoria> consultarHistorial(Tutor tutor) { return tutor.getHistorialTutorias(); }
    public double consultarRemuneracion(Tutor tutor) { return tutor.getRemuneracionAcumulada(); }
    public double consultarHorasBeca(Tutor tutor) { return tutor.getRegistroHorasBeca().getHorasAcumuladas(); }
    public double consultarPago(Tutor tutor) { return consultarRemuneracion(tutor); }
    public ArrayList<Pregunta> consultarPreguntas(Tutor tutor) {
        ArrayList<Pregunta> resultado = new ArrayList<Pregunta>();
        for (Pregunta pregunta : sistema.getPreguntas()) {
            if (pregunta.getSolicitud().getTutor() == tutor) { resultado.add(pregunta); }
        }
        return resultado;
    }
    public void agregarHorario(Tutor tutor, String fecha, String inicio, String fin) {
        agregarHorario(tutor, fecha, inicio, fin, null, null);
    }
    public void agregarHorario(Tutor tutor, String fecha, String inicio, String fin,
            ModalidadTutoria modalidad, String ubicacion) {
        verificarTutor(tutor);
        // Solo un campo omitido usa el predeterminado. Un campo vacío se rechaza.
        if (modalidad == ModalidadTutoria.EN_LINEA && ubicacion == null) {
            ubicacion = tutor.getEnlacePermanente();
        }
        Horario horario = new Horario(fecha, inicio, fin, modalidad, ubicacion);
        sistema.getSeguimiento().verificarHorario(tutor.getCarnet(), "tutor", horario);
        tutor.agregarHorario(horario);
    }
    public void completarHorario(Tutor tutor, Horario horario, ModalidadTutoria modalidad, String ubicacion) {
        verificarTutor(tutor);
        if (!tutor.getDisponibilidad().contains(horario)
                || sistema.tieneConflicto(tutor.getCarnet(), horario)) {
            throw new IllegalArgumentException("Horario ajeno o reservado.");
        }
        sistema.getSeguimiento().verificarHorario(tutor.getCarnet(), "tutor", horario);
        horario.actualizarCondiciones(modalidad, ubicacion);
    }
    public void registrarAusencia(Tutor tutor, SesionTutoria sesion) {
        verificarSolicitud(tutor, sesion.getSolicitud());
        if (!sistema.getSesiones().contains(sesion)) {
            throw new IllegalArgumentException("Sesión no registrada.");
        }
        sistema.getSeguimiento().reportar(sesion.getSolicitud(), tutor.getCarnet(), "tutor",
                "Ausencia reportada por el tutor desde consola.", "");
    }
    private void verificarTutor(Tutor tutor) {
        if (!sistema.getTutores().contains(tutor) || !tutor.isActivo()) {
            throw new IllegalArgumentException("Tutor no registrado.");
        }
    }
    private void verificarSolicitud(Tutor tutor, SolicitudTutoria solicitud) {
        if (!sistema.getTutores().contains(tutor) || !sistema.getSolicitudes().contains(solicitud)
                || !tutor.isActivo() || solicitud.getTutor() != tutor) {
            throw new IllegalArgumentException("La solicitud no pertenece al tutor.");
        }
    }
}
