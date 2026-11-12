import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

/** Persistencia de valoraciones, calendario, decisiones y auditoría dentro de la misma transacción. */
final class RepositorioSeguimiento {
    private RepositorioSeguimiento() { }
    static void cargar(Connection c, Seguimiento a) throws SQLException {
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM valoraciones ORDER BY id")) {
            while (r.next()) a.valoraciones.add(new Valoracion(r.getInt("id"), r.getInt("sesion"), r.getString("autor"), r.getString("tutor"), r.getString("fecha"), r.getInt("puntuacion"), r.getString("comentario")));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM evaluaciones_academicas ORDER BY id")) {
            while (r.next()) a.evaluaciones.add(new EvaluacionAcademica(r.getInt("id"), r.getString("tutor"), r.getString("evaluador"), r.getString("curso"), r.getString("fecha"), r.getInt("dominio"), r.getInt("claridad"), r.getString("fundamento")));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM periodos_academicos ORDER BY id")) {
            while (r.next()) a.periodos.add(new PeriodoAcademico(r.getString("id"), LocalDate.parse(r.getString("inicio")), LocalDate.parse(r.getString("fin"))));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM reportes_ausencia ORDER BY id")) {
            while (r.next()) a.reportes.add(new ReporteAusencia(r.getInt("id"), r.getInt("solicitud"), r.getString("autor"), r.getString("rol_autor"), r.getString("reportado"), r.getString("rol_reportado"), r.getString("periodo"), r.getString("fecha_sesion"), r.getString("fecha"), r.getString("motivo"), r.getString("evidencia")));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM decisiones_ausencia ORDER BY id")) {
            while (r.next()) a.decisiones.add(new DecisionAusencia(r.getInt("id"), r.getInt("reporte"), r.getString("estado"), r.getString("coordinador"), r.getString("fecha"), r.getString("motivo"), r.getString("inicio_propuesto"), r.getString("fin_propuesto"), r.getString("politica_estudiante"), r.getString("politica_tutor")));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM explicaciones_ausencia ORDER BY id")) {
            while (r.next()) a.explicaciones.add(new ExplicacionAusencia(r.getInt("id"), r.getInt("reporte"), r.getString("autor"), r.getString("rol_autor"), r.getString("fecha"), r.getString("contenido")));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM sanciones ORDER BY id")) {
            while (r.next()) a.sanciones.add(new Sancion(r.getInt("id"), r.getInt("reporte"), r.getInt("decision"), r.getString("usuario"), r.getString("rol"), r.getString("periodo"), r.getString("inicio"), r.getString("fin"), r.getString("motivo"), r.getBoolean("vigente"), r.getString("revision_fecha"), r.getString("revision_motivo")));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM resoluciones_reserva ORDER BY id")) {
            while (r.next()) a.resoluciones.add(new ResolucionReserva(r.getInt("id"), r.getInt("solicitud"), (Integer) r.getObject("nueva_solicitud"), r.getString("coordinador"), r.getString("fecha"), r.getString("motivo")));
        }
        try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM politica_sanciones WHERE id=1")) {
            if (r.next()) {
                a.politicaEstudiante=r.getString("estudiante"); a.politicaTutor=r.getString("tutor");
                a.politicaAutor=r.getString("autor"); a.politicaFecha=r.getString("fecha");
            }
        }
    }
    static void guardar(Connection c, Seguimiento a) throws SQLException {
        for (Valoracion x : a.valoraciones) RepositorioTutorias.ejecutar(c,
                "INSERT INTO valoraciones(id,sesion,autor,tutor,fecha,puntuacion,comentario) VALUES(?,?,?,?,?,?,?)",
                x.id(), x.sesion(), x.autor(), x.tutor(), x.fecha(), x.puntuacion(), x.comentario());
        for (EvaluacionAcademica x : a.evaluaciones) RepositorioTutorias.ejecutar(c,
                "INSERT INTO evaluaciones_academicas(id,tutor,evaluador,curso,fecha,dominio,claridad,fundamento) VALUES(?,?,?,?,?,?,?,?)",
                x.id(), x.tutor(), x.evaluador(), x.curso(), x.fecha(), x.dominio(), x.claridad(), x.fundamento());
        for (PeriodoAcademico x : a.periodos) RepositorioTutorias.ejecutar(c,
                "INSERT INTO periodos_academicos(id,inicio,fin) VALUES(?,?,?)",
                x.id(), x.inicio().toString(), x.fin().toString());
        for (ReporteAusencia x : a.reportes) RepositorioTutorias.ejecutar(c,
                "INSERT INTO reportes_ausencia(id,solicitud,autor,rol_autor,reportado,rol_reportado,periodo,fecha_sesion,fecha,motivo,evidencia) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                x.id(), x.solicitud(), x.autor(), x.rolAutor(), x.reportado(), x.rolReportado(), x.periodo(), x.fechaSesion(), x.fecha(), x.motivo(), x.evidencia());
        for (DecisionAusencia x : a.decisiones) RepositorioTutorias.ejecutar(c,
                "INSERT INTO decisiones_ausencia(id,reporte,estado,coordinador,fecha,motivo,inicio_propuesto,fin_propuesto,politica_estudiante,politica_tutor) VALUES(?,?,?,?,?,?,?,?,?,?)",
                x.id(), x.reporte(), x.estado(), x.coordinador(), x.fecha(), x.motivo(), x.inicioPropuesto(), x.finPropuesto(), x.politicaEstudiante(), x.politicaTutor());
        for (ExplicacionAusencia x : a.explicaciones) RepositorioTutorias.ejecutar(c,
                "INSERT INTO explicaciones_ausencia(id,reporte,autor,rol_autor,fecha,contenido) VALUES(?,?,?,?,?,?)",
                x.id(), x.reporte(), x.autor(), x.rolAutor(), x.fecha(), x.contenido());
        for (Sancion x : a.sanciones) RepositorioTutorias.ejecutar(c,
                "INSERT INTO sanciones(id,reporte,decision,usuario,rol,periodo,inicio,fin,motivo,vigente,revision_fecha,revision_motivo) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                x.id(), x.reporte(), x.decision(), x.usuario(), x.rol(), x.periodo(), x.inicio(), x.fin(), x.motivo(), x.vigente(), x.revisionFecha(), x.revisionMotivo());
        for (ResolucionReserva x : a.resoluciones) RepositorioTutorias.ejecutar(c,
                "INSERT INTO resoluciones_reserva(id,solicitud,nueva_solicitud,coordinador,fecha,motivo) VALUES(?,?,?,?,?,?)",
                x.id(), x.solicitud(), x.nuevaSolicitud(), x.coordinador(), x.fecha(), x.motivo());
        RepositorioTutorias.ejecutar(c, "INSERT INTO politica_sanciones VALUES(1,?,?,?,?)",
                a.politicaEstudiante, a.politicaTutor, a.politicaAutor, a.politicaFecha);
    }
}
