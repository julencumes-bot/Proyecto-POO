import java.util.ArrayList;

public class TutorController {
    private SistemaTutorias sistema;

    public TutorController(SistemaTutorias sistema) { this.sistema = sistema; }
    public ArrayList<Curso> consultarCursosAsignados(Tutor tutor) { return tutor.getCursosAsignados(); }
    public ArrayList<SolicitudTutoria> consultarSolicitudes(Tutor tutor) {
        ArrayList<SolicitudTutoria> resultado = new ArrayList<SolicitudTutoria>();
        for (SolicitudTutoria solicitud : sistema.getSolicitudes()) {
            if (solicitud.getTutor() == tutor) { resultado.add(solicitud); }
        }
        return resultado;
    }
    public SesionTutoria aceptarTutoria(Tutor tutor, SolicitudTutoria solicitud) {
        verificarSolicitud(tutor, solicitud);
        if (!tutor.isAprobado() || !tutor.getCursosAsignados().contains(solicitud.getCurso())) {
            throw new IllegalArgumentException("El tutor no esta aprobado para este curso.");
        }
        solicitud.confirmar();
        SesionTutoria sesion = new SesionTutoria(sistema.getSesiones().size() + 1, solicitud);
        sistema.agregarSesion(sesion);
        return sesion;
    }
    public void cancelarTutoria(Tutor tutor, SolicitudTutoria solicitud) {
        verificarSolicitud(tutor, solicitud);
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
        if (!sistema.getSesiones().contains(sesion)) {
            throw new IllegalArgumentException("Sesion no registrada.");
        }
        verificarSolicitud(tutor, sesion.getSolicitud());
        sesion.finalizar(temas, notas, proximo);
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
        if (!sistema.getTutores().contains(tutor)) {
            throw new IllegalArgumentException("Tutor no registrado.");
        }
        tutor.agregarHorario(new Horario(fecha, inicio, fin));
    }
    private void verificarSolicitud(Tutor tutor, SolicitudTutoria solicitud) {
        if (!sistema.getTutores().contains(tutor) || !sistema.getSolicitudes().contains(solicitud)
                || solicitud.getTutor() != tutor) {
            throw new IllegalArgumentException("La solicitud no pertenece al tutor.");
        }
    }
}
