import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Reglas compartidas por web y controladores. No recibe contadores del navegador. */
public class Seguimiento {
    public static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private Clock reloj = Clock.system(ZONA);
    final List<Valoracion> valoraciones = new ArrayList<>();
    final List<EvaluacionAcademica> evaluaciones = new ArrayList<>();
    final List<PeriodoAcademico> periodos = new ArrayList<>();
    final List<ReporteAusencia> reportes = new ArrayList<>();
    final List<DecisionAusencia> decisiones = new ArrayList<>();
    final List<ExplicacionAusencia> explicaciones = new ArrayList<>();
    final List<Sancion> sanciones = new ArrayList<>();
    final List<ResolucionReserva> resoluciones = new ArrayList<>();
    String politicaEstudiante = "";
    String politicaTutor = "";
    String politicaAutor = "";
    String politicaFecha = "";

    public void usarReloj(Clock reloj) { this.reloj = reloj.withZone(ZONA); }
    public LocalDate hoy() { return LocalDate.now(reloj); }
    public String ahora() { return java.time.OffsetDateTime.now(reloj).toString(); }
    public List<Valoracion> getValoraciones() { return List.copyOf(valoraciones); }
    public List<Sancion> getSanciones() { return List.copyOf(sanciones); }
    public List<ReporteAusencia> getReportes() { return List.copyOf(reportes); }
    public PeriodoAcademico periodo(LocalDate dia) {
        return periodos.stream().filter(p -> p.contiene(dia)).findFirst().orElse(null);
    }
    public void configurarPeriodo(PeriodoAcademico nuevo) {
        PeriodoAcademico anterior = periodos.stream().filter(p -> p.id().equals(nuevo.id())).findFirst().orElse(null);
        if (anterior != null && !anterior.equals(nuevo)
                && (reportes.stream().anyMatch(r -> r.periodo().equals(nuevo.id()))
                    || sanciones.stream().anyMatch(s -> !LocalDate.parse(s.fin()).isBefore(anterior.inicio())
                        && !LocalDate.parse(s.inicio()).isAfter(anterior.fin()))))
            throw new IllegalArgumentException("No se pueden cambiar fechas de un semestre con reportes o sanciones vinculados.");
        for (PeriodoAcademico p : periodos) {
            if (!p.id().equals(nuevo.id()) && !nuevo.fin().isBefore(p.inicio()) && !nuevo.inicio().isAfter(p.fin()))
                throw new IllegalArgumentException("Los semestres no pueden superponerse.");
        }
        periodos.removeIf(p -> p.id().equals(nuevo.id()));
        periodos.add(nuevo);
        periodos.sort(Comparator.comparing(PeriodoAcademico::inicio));
    }
    public void configurarPolitica(String estudiante, String tutor, String autor) {
        if (!List.of("SIGUIENTE_PERIODO", "FECHAS_CONFIRMACION").contains(estudiante == null ? "" : estudiante)
                || !List.of("DESDE_CONFIRMACION", "ACUMULAR_AL_FINAL").contains(tutor == null ? "" : tutor))
            throw new IllegalArgumentException("Selecciona reglas válidas para ambos roles.");
        politicaEstudiante = estudiante; politicaTutor = tutor;
        politicaAutor = autor; politicaFecha = ahora();
    }
    public DecisionAusencia decision(int reporte) {
        return decisiones.stream().filter(d -> d.reporte() == reporte)
                .max(Comparator.comparingInt(DecisionAusencia::id)).orElse(null);
    }
    public String estado(int reporte) {
        DecisionAusencia d = decision(reporte);
        return d == null ? "PENDIENTE" : d.estado();
    }
    public ReporteAusencia reporte(int id) {
        return reportes.stream().filter(r -> r.id() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Reporte no encontrado."));
    }
    public int strikes(String usuario, String rol, String periodo) {
        return (int) reportes.stream().filter(r -> r.reportado().equals(usuario)
                && r.rolReportado().equals(rol) && r.periodo().equals(periodo)
                && estado(r.id()).equals("CONFIRMADO")).count();
    }
    public Integer strikesActuales(String usuario, String rol) {
        PeriodoAcademico p = periodo(hoy());
        return p == null ? null : strikes(usuario, rol, p.id());
    }
    public boolean suspendido(String usuario, String rol, LocalDate dia) {
        return sanciones.stream().anyMatch(s -> s.usuario().equals(usuario) && s.rol().equals(rol) && s.contiene(dia));
    }
    public boolean bloqueado(String usuario, String rol, Horario h) {
        return suspendido(usuario, rol, hoy()) || suspendido(usuario, rol, LocalDate.parse(h.getFecha()));
    }
    public void verificarHorario(String usuario, String rol, Horario h) {
        if (bloqueado(usuario, rol, h)) throw new IllegalStateException(
                "El rol de " + rol + " tiene una suspensión vigente o que incluye la fecha de la tutoría.");
    }
    public boolean afectada(SolicitudTutoria s) {
        LocalDate dia = LocalDate.parse(s.getHorario().getFecha());
        return (s.getEstado() == EstadoSolicitud.PENDIENTE || s.getEstado() == EstadoSolicitud.CONFIRMADA)
                && (suspendido(s.getEstudiante().getCarnet(), "estudiante", dia)
                    || suspendido(s.getTutor().getCarnet(), "tutor", dia));
    }
    public void verificarSesion(SolicitudTutoria s) {
        verificarHorario(s.getEstudiante().getCarnet(), "estudiante", s.getHorario());
        verificarHorario(s.getTutor().getCarnet(), "tutor", s.getHorario());
    }
    public boolean enRevision(int solicitud) {
        return reportes.stream().anyMatch(r -> r.solicitud() == solicitud && estado(r.id()).equals("PENDIENTE"));
    }
    public void verificarCierre(SolicitudTutoria sol) {
        if (enRevision(sol.getIdSolicitud()))
            throw new IllegalStateException("Esta sesión tiene un reporte pendiente. Coordinación debe revisarlo antes de cerrarla.");
    }
    public boolean termino(Horario h) {
        return !LocalDateTime.now(reloj).isBefore(LocalDateTime.parse(h.getFecha() + "T" + h.getHoraFin()));
    }
    public void valorar(SesionTutoria ses, String autor, int puntos, String comentario) {
        SolicitudTutoria sol = ses.getSolicitud();
        if (!sol.getEstudiante().getCarnet().equals(autor)) throw new SecurityException("La sesión pertenece a otro estudiante.");
        if (!ses.isFinalizada() || sol.getEstado() != EstadoSolicitud.FINALIZADA)
            throw new IllegalStateException("Solo se pueden valorar sesiones finalizadas.");
        if (valoraciones.stream().anyMatch(v -> v.sesion() == ses.getIdSesion() && v.autor().equals(autor)))
            throw new IllegalStateException("Ya valoraste esta sesión.");
        valoraciones.add(new Valoracion(valoraciones.size()+1, ses.getIdSesion(), autor,
                sol.getTutor().getCarnet(), ahora(), puntos, comentario));
    }
    public void reportar(SolicitudTutoria sol, String autor, String rol, String motivo, String evidencia) {
        boolean estudiante = rol.equals("estudiante") && sol.getEstudiante().getCarnet().equals(autor);
        boolean tutor = (rol.equals("tutor") || rol.equals("coordinador")) && sol.getTutor().getCarnet().equals(autor);
        if (!estudiante && !tutor) throw new SecurityException("Solo un participante puede reportar esta sesión.");
        if (sol.getEstado() != EstadoSolicitud.CONFIRMADA && sol.getEstado() != EstadoSolicitud.INASISTENCIA)
            throw new IllegalStateException("Se requiere una tutoría confirmada; una cancelación no es una ausencia.");
        if (!termino(sol.getHorario())) throw new IllegalStateException("Espera a que termine el horario de la tutoría.");
        PeriodoAcademico p = periodo(LocalDate.parse(sol.getHorario().getFecha()));
        if (p == null) throw new IllegalStateException("Coordinación debe configurar el semestre de la fecha de la tutoría.");
        String reportado = estudiante ? sol.getTutor().getCarnet() : sol.getEstudiante().getCarnet();
        String rolReportado = estudiante ? "tutor" : "estudiante";
        if (reportes.stream().anyMatch(r -> r.solicitud() == sol.getIdSolicitud()
                && r.reportado().equals(reportado) && r.rolReportado().equals(rolReportado)))
            throw new IllegalStateException("Ya existe un reporte contra esta persona por esta sesión.");
        reportes.add(new ReporteAusencia(reportes.size()+1, sol.getIdSolicitud(), autor, rol,
                reportado, rolReportado, p.id(), sol.getHorario().getFecha(), ahora(), texto(motivo), opcional(evidencia)));
    }
    public static LocalDate fecha(String valor) {
        try { Validacion.fecha(valor); return LocalDate.parse(valor); }
        catch (java.time.DateTimeException error) { throw new IllegalArgumentException("Escribe una fecha válida con formato AAAA-MM-DD."); }
    }
    public static String texto(String valor) {
        String resultado = Validacion.texto(valor, "Texto");
        if (resultado.length() > 2000) throw new IllegalArgumentException("El texto admite hasta 2000 caracteres.");
        return resultado;
    }
    public static String opcional(String valor) {
        return valor == null || valor.isBlank() ? "" : texto(valor);
    }
    public void explicar(ReporteAusencia r, String autor, String rol, String contenido) {
        if (!rol.equals("coordinador") && !(r.reportado().equals(autor) && r.rolReportado().equals(rol)))
            throw new SecurityException("Solo la persona reportada o coordinación puede registrar su explicación.");
        if (!estado(r.id()).equals("PENDIENTE")) throw new IllegalStateException("El reporte ya tiene una decisión.");
        explicaciones.add(new ExplicacionAusencia(explicaciones.size()+1, r.id(), autor, rol, ahora(), texto(contenido)));
    }
    public void resolver(SistemaTutorias sistema, ReporteAusencia r, String coordinador, String resultado,
            String motivo, boolean correccion, String desde, String hasta) {
        SolicitudTutoria sol = ServicioWeb.solicitud(sistema, Integer.toString(r.solicitud()));
        if (!sistema.getCoordinador().getCarnet().equals(coordinador)
                || sol.getEstudiante().getCarnet().equals(coordinador) || sol.getTutor().getCarnet().equals(coordinador))
            throw new SecurityException("Un participante no puede resolver ni corregir su propio reporte.");
        if (!List.of("CONFIRMADO", "DESCARTADO").contains(resultado == null ? "" : resultado)) throw new IllegalArgumentException("Decisión inválida.");
        if (correccion == estado(r.id()).equals("PENDIENTE"))
            throw new IllegalStateException(correccion ? "El reporte todavía no tiene decisión." : "El reporte ya fue resuelto; usa corregir decisión.");
        if (resultado.equals(estado(r.id()))) throw new IllegalStateException("La decisión ya tiene ese estado.");
        String inicio = "", fin = "";
        if (resultado.equals("CONFIRMADO")) {
            if (sol.getEstado() != EstadoSolicitud.CONFIRMADA && sol.getEstado() != EstadoSolicitud.INASISTENCIA)
                throw new IllegalStateException("No se puede confirmar ausencia de una sesión finalizada o cancelada.");
            if (politicaEstudiante.isEmpty() || politicaTutor.isEmpty())
                throw new IllegalStateException("Coordinación debe acordar y guardar primero la política de sanciones.");
            LocalDate a = hoy(), b;
            if (r.rolReportado().equals("tutor")) {
                if (politicaTutor.equals("ACUMULAR_AL_FINAL")) {
                    for (Sancion s : sanciones) if (s.vigente() && s.usuario().equals(r.reportado())
                            && s.rol().equals("tutor") && !LocalDate.parse(s.fin()).isBefore(a))
                        a = LocalDate.parse(s.fin()).plusDays(1);
                }
                b = a.plusMonths(1).minusDays(1);
            } else if (politicaEstudiante.equals("SIGUIENTE_PERIODO")) {
                PeriodoAcademico siguiente = periodos.stream().filter(p -> p.inicio().isAfter(hoy()))
                        .min(Comparator.comparing(PeriodoAcademico::inicio)).orElseThrow(() ->
                            new IllegalStateException("Configura el siguiente semestre completo antes de confirmar una ausencia estudiantil."));
                a = siguiente.inicio(); b = siguiente.fin();
            } else {
                a = fecha(desde); b = fecha(hasta);
                if (!a.equals(hoy()) || b.isBefore(a)) throw new IllegalArgumentException(
                        "La suspensión debe iniciar hoy y tener una fecha final acordada para un semestre.");
            }
            inicio = a.toString(); fin = b.toString();
        }
        decisiones.add(new DecisionAusencia(decisiones.size()+1, r.id(), resultado, coordinador, ahora(),
                texto(motivo), inicio, fin, politicaEstudiante, politicaTutor));
        recalcularSanciones();
        boolean confirmada = reportes.stream().anyMatch(x -> x.solicitud() == r.solicitud() && estado(x.id()).equals("CONFIRMADO"));
        SesionTutoria ses = sistema.getSesiones().stream().filter(x -> x.getSolicitud() == sol).findFirst().orElseThrow();
        if (confirmada && sol.getEstado() == EstadoSolicitud.CONFIRMADA) ses.registrarAusencia();
        if (!confirmada && sol.getEstado() == EstadoSolicitud.INASISTENCIA) ses.reabrirTrasCorreccion();
    }
    private void recalcularSanciones() {
        Set<Integer> aplicables = new HashSet<>();
        Map<String, Integer> conteos = new HashMap<>();
        List<DecisionAusencia> actuales = reportes.stream().map(r -> decision(r.id()))
                .filter(d -> d != null && d.estado().equals("CONFIRMADO"))
                .sorted(Comparator.comparingInt(DecisionAusencia::id)).toList();
        for (DecisionAusencia d : actuales) {
            ReporteAusencia r = reporte(d.reporte());
            int cantidad = conteos.merge(r.reportado()+"|"+r.rolReportado()+"|"+r.periodo(), 1, Integer::sum);
            if (r.rolReportado().equals("tutor") || cantidad == 3) {
                aplicables.add(d.id());
                if (sanciones.stream().noneMatch(s -> s.decision() == d.id()))
                    sanciones.add(new Sancion(sanciones.size()+1, r.id(), d.id(), r.reportado(), r.rolReportado(),
                            r.periodo(), d.inicioPropuesto(), d.finPropuesto(), d.motivo(), true, "", ""));
            }
        }
        for (int i=0; i<sanciones.size(); i++) {
            Sancion s = sanciones.get(i);
            sanciones.set(i, s.revisar(aplicables.contains(s.decision()), ahora(),
                    "Recalculada por decisión #" + decisiones.get(decisiones.size()-1).id()));
        }
    }
}
