import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Adaptador de permisos y JSON para los cambios 7 y 8. */
final class SeguimientoWeb {
    private SeguimientoWeb() { }
    private static Map<String,Object> obj(Object... valores) { return ServicioWeb.obj(valores); }
    private static void coordinador(Seguridad.Identidad id) {
        if (!id.rol().equals("coordinador")) throw new SecurityException("Esta acción requiere coordinación.");
    }
    private static int numero(Map<String,String> f, String campo) { return Integer.parseInt(f.get(campo)); }
    static boolean ejecutar(SistemaTutorias s, Seguridad.Identidad id, String accion, Map<String,String> f) {
        Seguimiento a = s.getSeguimiento();
        switch (accion) {
            case "valorar" -> {
                if (!id.rol().equals("estudiante")) throw new SecurityException("Solo estudiantes pueden valorar sesiones.");
                a.valorar(ServicioWeb.sesion(s, f.get("sesion")), id.carnet(), numero(f,"puntuacion"), f.get("comentario"));
            }
            case "evaluacion-academica" -> {
                coordinador(id);
                Tutor t = ServicioWeb.tutor(s, f.get("tutor"));
                Curso c = ServicioWeb.curso(s, f.get("curso"));
                boolean relacionado = t.getCursosAsignados().contains(c) || s.getSolicitudes().stream()
                        .anyMatch(sol -> sol.getTutor() == t && sol.getCurso() == c);
                if (!relacionado) throw new IllegalArgumentException("El curso no corresponde al tutor evaluado.");
                a.evaluaciones.add(new EvaluacionAcademica(a.evaluaciones.size()+1, t.getCarnet(), id.carnet(),
                        c.getCodigo(), a.ahora(), numero(f,"dominio"), numero(f,"claridad"), f.get("fundamento")));
            }
            case "periodo-academico" -> {
                coordinador(id);
                a.configurarPeriodo(new PeriodoAcademico(f.get("periodo"), Seguimiento.fecha(f.get("inicio")), Seguimiento.fecha(f.get("fin"))));
            }
            case "politica-sanciones" -> {
                coordinador(id);
                if (!"on".equals(f.get("acuerdo"))) throw new IllegalArgumentException("Confirma que el equipo acordó estas reglas.");
                a.configurarPolitica(f.get("estudiante"), f.get("tutor"), id.carnet());
            }
            case "ausencia", "reportar-ausencia" -> {
                SolicitudTutoria sol = f.containsKey("solicitud") ? ServicioWeb.solicitud(s, f.get("solicitud"))
                        : ServicioWeb.sesion(s, f.get("sesion")).getSolicitud();
                a.reportar(sol, id.carnet(), id.rol(), f.get("motivo"), f.get("evidencia"));
            }
            case "explicar-ausencia" -> a.explicar(a.reporte(numero(f,"reporte")), id.carnet(), id.rol(), f.get("contenido"));
            case "resolver-ausencia", "corregir-ausencia" -> {
                coordinador(id);
                a.resolver(s, a.reporte(numero(f,"reporte")), id.carnet(), f.get("decision"), f.get("motivo"),
                        accion.equals("corregir-ausencia"), f.getOrDefault("inicio", ""), f.getOrDefault("fin", ""));
            }
            case "resolver-reserva" -> {
                coordinador(id);
                SolicitudTutoria original = ServicioWeb.solicitud(s, f.get("solicitud"));
                if (!a.afectada(original)) throw new IllegalStateException("La solicitud no está afectada por una suspensión.");
                a.verificarCierre(original);
                String motivo = Seguimiento.texto(f.get("motivo"));
                String tipo = f.getOrDefault("tipo", "");
                if (!List.of("CANCELAR", "REASIGNAR").contains(tipo)) throw new IllegalArgumentException("Selecciona una resolución.");
                original.cancelar();
                Integer nueva = null;
                if (tipo.equals("REASIGNAR")) {
                    Tutor t = ServicioWeb.tutor(s, f.get("tutor"));
                    Horario h = ServicioWeb.horario(t, f.get("horario"));
                    SolicitudTutoria sustituta = new EstudianteController(s).solicitarTutoria(original.getEstudiante(),
                            original.getCurso(), t, original.getTema(), h);
                    nueva = sustituta.getIdSolicitud();
                }
                a.resoluciones.add(new ResolucionReserva(a.resoluciones.size()+1, original.getIdSolicitud(), nueva,
                        id.carnet(), a.ahora(), motivo));
            }
            default -> { return false; }
        }
        return true;
    }
    static boolean participante(SolicitudTutoria sol, Seguridad.Identidad id) {
        return id.rol().equals("estudiante") ? sol.getEstudiante().getCarnet().equals(id.carnet())
                : sol.getTutor().getCarnet().equals(id.carnet());
    }
    static Map<String,Object> resumen(Seguimiento a, String carnet, String rol) {
        List<Object> periodos = new ArrayList<>();
        for (Sancion sancion : a.sanciones) if (sancion.vigente() && sancion.usuario().equals(carnet) && sancion.rol().equals(rol))
            periodos.add(obj("inicio", sancion.inicio(), "fin", sancion.fin()));
        return obj("strikesConfirmadosSemestre", a.strikesActuales(carnet, rol),
                "suspendido", a.suspendido(carnet, rol, a.hoy()), "suspensiones", periodos);
    }
    static void perfilTutor(SistemaTutorias s, Tutor t, Map<String,Object> perfil) {
        Seguimiento a = s.getSeguimiento();
        var opiniones = a.valoraciones.stream().filter(v -> v.tutor().equals(t.getCarnet())).toList();
        perfil.putAll(resumen(a, t.getCarnet(), "tutor"));
        perfil.put("cantidadValoraciones", opiniones.size());
        perfil.put("promedioValoraciones", opiniones.isEmpty() ? null
                : opiniones.stream().mapToInt(Valoracion::puntuacion).average().orElseThrow());
        perfil.put("valoraciones", opiniones.stream().map(v -> obj("id",v.id(),"sesion",v.sesion(),
                "autor",ServicioWeb.estudiante(s,v.autor()).getNombre(),"autorCarnet",v.autor(),
                "fecha",v.fecha(),"puntuacion",v.puntuacion(),"comentario",v.comentario())).toList());
        perfil.put("evaluacionesAcademicas", a.evaluaciones.stream().filter(v -> v.tutor().equals(t.getCarnet()))
                .map(v -> obj("id",v.id(),"evaluador",s.getCoordinador().getNombre(),"evaluadorCarnet",v.evaluador(),
                "curso",v.curso(),"nombreCurso",ServicioWeb.curso(s,v.curso()).getNombre(),"fecha",v.fecha(),
                "dominio",v.dominio(),"claridad",v.claridad(),"fundamento",v.fundamento())).toList());
    }
    static void solicitud(SistemaTutorias s, SolicitudTutoria sol, Seguridad.Identidad id, Map<String,Object> json) {
        Seguimiento a = s.getSeguimiento();
        String objetivo = id.rol().equals("estudiante") ? "tutor" : "estudiante";
        boolean existe = a.reportes.stream().anyMatch(r -> r.solicitud() == sol.getIdSolicitud() && r.rolReportado().equals(objetivo));
        json.put("puedeReportar", participante(sol,id) && !existe && a.termino(sol.getHorario())
                && (sol.getEstado() == EstadoSolicitud.CONFIRMADA || sol.getEstado() == EstadoSolicitud.INASISTENCIA));
        json.put("enRevision", a.enRevision(sol.getIdSolicitud()));
        json.put("afectadaPorSuspension", a.afectada(sol));
        json.put("accionesDocentesBloqueadas", a.bloqueado(sol.getTutor().getCarnet(),"tutor",sol.getHorario())
                || a.bloqueado(sol.getEstudiante().getCarnet(),"estudiante",sol.getHorario()));
    }
    static Map<String,Object> datos(SistemaTutorias s, Seguridad.Identidad id) {
        Seguimiento a = s.getSeguimiento();
        boolean coord = id.rol().equals("coordinador");
        String rol = coord ? "tutor" : id.rol();
        List<Object> reportes = new ArrayList<>();
        for (ReporteAusencia r : a.reportes) {
            if (!coord && !(r.autor().equals(id.carnet()) && r.rolAutor().equals(id.rol()))
                    && !(r.reportado().equals(id.carnet()) && r.rolReportado().equals(id.rol()))) continue;
            SolicitudTutoria sol = ServicioWeb.solicitud(s, Integer.toString(r.solicitud()));
            Map<String,Object> reporte = obj("id",r.id(),"solicitud",r.solicitud(),"autor",r.autor(),"rolAutor",r.rolAutor(),
                    "reportado",r.reportado(),"rolReportado",r.rolReportado(),"periodo",r.periodo(),"fechaSesion",r.fechaSesion(),
                    "inicio",sol.getHorario().getHoraInicio(),"fin",sol.getHorario().getHoraFin(),
                    "curso",sol.getCurso().getNombre(),"estudiante",sol.getEstudiante().getNombre(),"tutor",sol.getTutor().getNombre(),
                    "fecha",r.fecha(),"motivo",r.motivo(),"evidencia",r.evidencia(),"estado",a.estado(r.id()),
                    "puedeResolver",coord && !participante(sol,id),
                    "puedeExplicar",a.estado(r.id()).equals("PENDIENTE") && (coord || (r.reportado().equals(id.carnet()) && r.rolReportado().equals(id.rol()))));
            reporte.put("decisiones", a.decisiones.stream().filter(d -> d.reporte()==r.id()).map(d -> obj("id",d.id(),
                    "estado",d.estado(),"coordinador",d.coordinador(),"fecha",d.fecha(),"motivo",d.motivo(),
                    "inicioPropuesto",d.inicioPropuesto(),"finPropuesto",d.finPropuesto(),
                    "politicaEstudiante",d.politicaEstudiante(),"politicaTutor",d.politicaTutor())).toList());
            reporte.put("explicaciones", a.explicaciones.stream().filter(e -> e.reporte()==r.id()).map(e -> obj(
                    "autor",e.autor(),"rolAutor",e.rolAutor(),"fecha",e.fecha(),"contenido",e.contenido())).toList());
            reportes.add(reporte);
        }
        PeriodoAcademico vigente = a.periodo(a.hoy());
        Map<String,Object> resultado = obj("hoy",a.hoy().toString(),"periodoActual",vigente==null?null:vigente.id(),
                "periodos",a.periodos.stream().map(p -> obj("id",p.id(),"inicio",p.inicio().toString(),"fin",p.fin().toString())).toList(),
                "politica",obj("estudiante",a.politicaEstudiante,"tutor",a.politicaTutor,"autor",a.politicaAutor,"fecha",a.politicaFecha),
                "reportes",reportes,"miSituacion",resumen(a,id.carnet(),rol));
        resultado.put("sanciones",a.sanciones.stream().filter(x -> coord || (x.usuario().equals(id.carnet()) && x.rol().equals(rol)))
                .map(x -> obj("id",x.id(),"reporte",x.reporte(),"decision",x.decision(),"usuario",x.usuario(),"rol",x.rol(),
                        "periodo",x.periodo(),"inicio",x.inicio(),"fin",x.fin(),"motivo",x.motivo(),"vigente",x.vigente(),
                        "activa",x.contiene(a.hoy()),"revisionFecha",x.revisionFecha(),"revisionMotivo",x.revisionMotivo())).toList());
        resultado.put("resoluciones",a.resoluciones.stream().filter(x -> coord || participante(ServicioWeb.solicitud(s,""+x.solicitud()),id))
                .map(x -> obj("id",x.id(),"solicitud",x.solicitud(),"nuevaSolicitud",x.nuevaSolicitud(),
                        "coordinador",x.coordinador(),"fecha",x.fecha(),"motivo",x.motivo())).toList());
        resultado.put("misValoraciones",a.valoraciones.stream().filter(x -> id.rol().equals("estudiante") && x.autor().equals(id.carnet()))
                .map(x -> obj("sesion",x.sesion(),"puntuacion",x.puntuacion(),"comentario",x.comentario(),"fecha",x.fecha())).toList());
        resultado.put("contadores",coord ? a.periodos.stream().map(p -> obj("periodo",p.id(),"personas",
                a.reportes.stream().filter(r -> r.periodo().equals(p.id())).map(r -> r.reportado()+"|"+r.rolReportado()).distinct()
                .map(k -> { String[] v=k.split("\\|"); return obj("usuario",v[0],"rol",v[1],"strikes",a.strikes(v[0],v[1],p.id())); }).toList())).toList() : List.of());
        return resultado;
    }
}
