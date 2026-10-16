import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/** Traduce tablas relacionales a objetos compartidos y conserva una revisión transaccional.
 * Para el volumen del proyecto, guarda una instantánea completa en una sola transacción.
 * La revisión impide que dos procesos sobrescriban cambios leídos al mismo tiempo.
 */
public class RepositorioTutorias {
    public record Estado(SistemaTutorias sistema, long revision) { }
    private final BaseDatos db;
    public RepositorioTutorias(BaseDatos db) { this.db = db; }

    public Estado cargar() throws SQLException {
        try (Connection c = db.conectar()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            long revision;
            try (var st = c.createStatement(); var r = st.executeQuery("SELECT revision FROM app_version WHERE id=1")) {
                r.next(); revision = r.getLong(1);
            }
            ProfesorCoordinador profesor;
            try (var st = c.createStatement(); var r = st.executeQuery("SELECT * FROM coordinadores")) {
                if (!r.next()) { c.commit(); return new Estado(null, revision); }
                profesor = new ProfesorCoordinador(r.getString("nombre"), r.getString("carnet"), r.getString("correo"));
            }
            SistemaTutorias s = new SistemaTutorias(profesor);
            Map<String, Estudiante> estudiantes = new HashMap<>();
            Map<String, Tutor> tutores = new HashMap<>();
            Map<String, Curso> cursos = new HashMap<>();
            Map<Integer, Horario> horarios = new HashMap<>();
            Map<Integer, SolicitudTutoria> solicitudes = new HashMap<>();
            Map<Integer, String> estados = new HashMap<>();
            try (var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM estudiantes ORDER BY carnet")) {
                while(r.next()) {
                    Estudiante e=new Estudiante(r.getString("nombre"),r.getString("carnet"),r.getString("correo"),r.getInt("anio"));
                    s.agregarEstudiante(e); estudiantes.put(e.getCarnet(),e);
                }
            }
            try(var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM tutores ORDER BY carnet")) {
                while(r.next()) {
                    String id=r.getString("carnet");
                    Tutor t=id.equals(profesor.getCarnet()) ? profesor.getPerfilTutor() : new Tutor(r.getString("nombre"),id,r.getString("correo"),r.getInt("anio"),r.getBoolean("beca"),r.getDouble("tarifa"));
                    if(t!=profesor.getPerfilTutor()) { s.agregarTutor(t); }
                    t.setAprobado(r.getBoolean("aprobado")); tutores.put(id,t);
                }
            }
            try(var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM cursos ORDER BY codigo")) {
                while(r.next()) { Curso curso=new Curso(r.getString("codigo"),r.getString("nombre")); s.agregarCurso(curso); cursos.put(curso.getCodigo(),curso); }
            }
            try(var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM aprobaciones ORDER BY tutor,curso")) {
                while(r.next()) { profesor.aprobarTutor(tutores.get(r.getString("tutor")),cursos.get(r.getString("curso"))); }
            }
            try(var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM horarios ORDER BY id")) {
                while(r.next()) { Horario h=new Horario(r.getString("fecha"),r.getString("inicio"),r.getString("fin")); tutores.get(r.getString("tutor")).agregarHorario(h); horarios.put(r.getInt("id"),h); }
            }
            try(var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM solicitudes ORDER BY id")) {
                while(r.next()) {
                    SolicitudTutoria sol=new SolicitudTutoria(r.getInt("id"),estudiantes.get(r.getString("estudiante")),tutores.get(r.getString("tutor")),cursos.get(r.getString("curso")),r.getString("tema"),horarios.get(r.getInt("horario")));
                    sol.restaurarPrecio(r.getDouble("precio"));
                    s.agregarSolicitud(sol); sol.getEstudiante().agregarSolicitud(sol); solicitudes.put(sol.getIdSolicitud(),sol); estados.put(sol.getIdSolicitud(),r.getString("estado"));
                }
            }
            try(var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM sesiones ORDER BY id")) {
                while(r.next()) {
                    SolicitudTutoria sol=solicitudes.get(r.getInt("solicitud")); sol.confirmar();
                    SesionTutoria ses=new SesionTutoria(r.getInt("id"),sol); s.agregarSesion(ses);
                    if(r.getBoolean("finalizada")) {
                        ses.finalizar(r.getString("temas"),r.getString("notas"),r.getString("proximo"));
                        sol.getEstudiante().agregarTutoriaHistorial(ses); sol.getTutor().agregarSesion(ses);
                    }
                }
            }
            for(SolicitudTutoria sol:s.getSolicitudes()) {
                if(estados.get(sol.getIdSolicitud()).equals("CANCELADA")) { sol.cancelar(); }
                if(!sol.getEstado().name().equals(estados.get(sol.getIdSolicitud()))) { throw new SQLException("Estado persistido inconsistente."); }
            }
            // Se calcula al final: una solicitud cancelada no libera una reserva posterior.
            for(SolicitudTutoria sol:s.getSolicitudes()) {
                if(sol.getEstado()!=EstadoSolicitud.CANCELADA) { sol.getHorario().setDisponible(false); }
            }
            try(var st=c.createStatement(); var r=st.executeQuery("SELECT * FROM preguntas ORDER BY id")) {
                while(r.next()) { s.agregarPregunta(new Pregunta(r.getInt("id"),r.getString("contenido"),estudiantes.get(r.getString("estudiante")),solicitudes.get(r.getInt("solicitud")))); }
            }
            c.commit(); return new Estado(s,revision);
        }
    }

    public void guardar(Estado estado, Seguridad.Cuenta nueva) throws SQLException {
        try(Connection c=db.conectar()) {
            c.setAutoCommit(false);
            try {
                int actualizados=ejecutar(c,"UPDATE app_version SET revision=revision+1 WHERE id=1 AND revision=?",estado.revision());
                if(actualizados!=1) { throw new SQLException("La información cambió en otra ventana. Recarga e inténtalo de nuevo.","40001"); }
                for(String tabla:new String[]{"preguntas","sesiones","solicitudes","aprobaciones","horarios","cursos","estudiantes","tutores","coordinadores"}) {
                    ejecutar(c,"DELETE FROM "+tabla);
                }
                SistemaTutorias s=estado.sistema(); ProfesorCoordinador p=s.getCoordinador();
                ejecutar(c,"INSERT INTO coordinadores VALUES(?,?,?)",p.getCarnet(),p.getNombre(),p.getCorreo());
                for(Estudiante e:s.getEstudiantes()) { ejecutar(c,"INSERT INTO estudiantes VALUES(?,?,?,?)",e.getCarnet(),e.getNombre(),e.getCorreo(),e.getAnioCarrera()); }
                for(Tutor t:s.getTutores()) { ejecutar(c,"INSERT INTO tutores VALUES(?,?,?,?,?,?,?)",t.getCarnet(),t.getNombre(),t.getCorreo(),t.getAnioCarrera(),t.isRecibeHorasBeca(),t.getTarifaHora(),t.isAprobado()); }
                for(Curso curso:s.getCursos()) { ejecutar(c,"INSERT INTO cursos VALUES(?,?)",curso.getCodigo(),curso.getNombre()); }
                Map<Horario,Integer> ids=new IdentityHashMap<>(); int id=0;
                for(Tutor t:s.getTutores()) {
                    for(Curso curso:t.getCursosAsignados()) { ejecutar(c,"INSERT INTO aprobaciones VALUES(?,?)",t.getCarnet(),curso.getCodigo()); }
                    for(Horario h:t.getDisponibilidad()) { ids.put(h,++id); ejecutar(c,"INSERT INTO horarios VALUES(?,?,?,?,?)",id,t.getCarnet(),h.getFecha(),h.getHoraInicio(),h.getHoraFin()); }
                }
                for(SolicitudTutoria sol:s.getSolicitudes()) { ejecutar(c,"INSERT INTO solicitudes VALUES(?,?,?,?,?,?,?,?)",sol.getIdSolicitud(),sol.getEstudiante().getCarnet(),sol.getTutor().getCarnet(),sol.getCurso().getCodigo(),ids.get(sol.getHorario()),sol.getTema(),sol.getEstado().name(),sol.getPrecio()); }
                for(SesionTutoria ses:s.getSesiones()) { ejecutar(c,"INSERT INTO sesiones VALUES(?,?,?,?,?,?)",ses.getIdSesion(),ses.getSolicitud().getIdSolicitud(),ses.getTemasVistos(),ses.getNotas(),ses.getTemaProximaTutoria(),ses.isFinalizada()); }
                for(Pregunta q:s.getPreguntas()) { ejecutar(c,"INSERT INTO preguntas VALUES(?,?,?,?)",q.getIdPregunta(),q.getSolicitud().getIdSolicitud(),q.getEstudiante().getCarnet(),q.getContenido()); }
                if(nueva!=null) { Seguridad.insertar(c,nueva); }
                c.commit();
            } catch(SQLException|RuntimeException e) { c.rollback(); throw e; }
        }
    }
    static int ejecutar(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement p=c.prepareStatement(sql)) {
            for(int i=0;i<args.length;i++) { p.setObject(i+1,args[i]); }
            return p.executeUpdate();
        }
    }
}
