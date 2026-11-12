import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Casos de aceptación 7/8, con reloj controlado: no cambian el reloj ni los datos reales. */
public class PruebasValoracionesAusencias {
    private static int pruebas;
    private static final Seguridad.Identidad ANA=new Seguridad.Identidad("estudiante","E001");
    private static final Seguridad.Identidad LUIS=new Seguridad.Identidad("estudiante","E002");
    private static final Seguridad.Identidad MARIA=new Seguridad.Identidad("tutor","T001");
    private static final Seguridad.Identidad DIEGO=new Seguridad.Identidad("tutor","T002");
    private static final Seguridad.Identidad COORD=new Seguridad.Identidad("coordinador","P001");
    private interface Accion { void ejecutar() throws Exception; }
    private static void ok(boolean value,String mensaje){if(!value)throw new AssertionError(mensaje);pruebas++;}
    private static void no(Accion accion,String mensaje)throws Exception{
        try{accion.ejecutar();}catch(IllegalArgumentException|IllegalStateException|SecurityException ex){pruebas++;return;}
        throw new AssertionError("Se permitió: "+mensaje);
    }
    private static Map<String,String> f(String... values){Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put(values[i],values[i+1]);return m;}
    private static class Caso {
        final BaseDatos db;
        RepositorioTutorias repo;
        ServicioWeb web;
        String dia="2026-05-15";
        Caso()throws Exception{
            var carpeta=Files.createTempDirectory("pruebas-78-");
            db=new BaseDatos("jdbc:h2:"+carpeta.resolve("datos")+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE","sa","");
            db.inicializar();repo=new RepositorioTutorias(db);fecha(dia);web.inicializar(true);
        }
        void fecha(String valor){dia=valor;web=new ServicioWeb(repo,Clock.fixed(LocalDate.parse(dia).atTime(12,0).atZone(Seguimiento.ZONA).toInstant(),Seguimiento.ZONA));}
        void reiniciar()throws Exception{db.inicializar();repo=new RepositorioTutorias(db);fecha(dia);}
        SistemaTutorias sistema()throws Exception{var s=repo.cargar().sistema();s.getSeguimiento().usarReloj(Clock.fixed(LocalDate.parse(dia).atTime(12,0).atZone(Seguimiento.ZONA).toInstant(),Seguimiento.ZONA));return s;}
        void a(Seguridad.Identidad id,String action,String... values)throws Exception{web.ejecutar(id,action,f(values));}
        void calendario()throws Exception{
            a(COORD,"periodo-academico","periodo","2026-A","inicio","2026-01-12","fin","2026-05-20");
            a(COORD,"periodo-academico","periodo","2026-B","inicio","2026-08-03","fin","2026-12-04");
            a(COORD,"periodo-academico","periodo","2027-A","inicio","2027-01-18","fin","2027-05-22");
        }
        void politica(String estudiante,String tutor)throws Exception{a(COORD,"politica-sanciones","estudiante",estudiante,"tutor",tutor,"acuerdo","on");}
        void politica()throws Exception{politica("SIGUIENTE_PERIODO","DESDE_CONFIRMACION");}
        int reserva(String fecha,Seguridad.Identidad estudiante,Seguridad.Identidad tutor,boolean aceptar)throws Exception{
            a(tutor,"horario","fecha",fecha,"inicio","10:00","fin","11:00","modalidad","PRESENCIAL","ubicacion","B-201");
            String curso=tutor==COORD?"CC5001":tutor==DIEGO?"MM101":"CC2006";
            a(estudiante,"solicitar","tutor",tutor.carnet(),"curso",curso,"horario",fecha+"|10:00|11:00","tema","Objetos");
            int id=sistema().getSolicitudes().size();
            if(aceptar)a(tutor,"aceptar","solicitud",""+id);
            return id;
        }
        int sesion(int sol)throws Exception{return sistema().getSesiones().stream().filter(x->x.getSolicitud().getIdSolicitud()==sol).findFirst().orElseThrow().getIdSesion();}
        SolicitudTutoria solicitud(int id)throws Exception{return ServicioWeb.solicitud(sistema(),""+id);}
        void finalizar(int sol,Seguridad.Identidad tutor)throws Exception{a(tutor,"finalizar","sesion",""+sesion(sol),"temas","Herencia","notas","Avanza","duracion","1");}
        int reportar(int sol,Seguridad.Identidad autor)throws Exception{
            a(autor,"reportar-ausencia","solicitud",""+sol,"motivo","No se presentó <script>alert(1)</script>","evidencia","Registro de sala");
            return sistema().getSeguimiento().reportes.size();
        }
        void resolver(int id,String estado)throws Exception{a(COORD,"resolver-ausencia","reporte",""+id,"decision",estado,"motivo","Revisado con participantes");}
        void corregir(int id,String estado)throws Exception{a(COORD,"corregir-ausencia","reporte",""+id,"decision",estado,"motivo","Nueva evidencia verificada");}
    }
    private static void valoraciones()throws Exception{
        Caso c=new Caso();
        int s1=c.reserva("2026-05-01",ANA,MARIA,true), s2=c.reserva("2026-05-02",LUIS,MARIA,true);
        no(()->c.a(ANA,"valorar","sesion",""+c.sesion(s1),"puntuacion","5"),"Sesión sin finalizar");
        c.finalizar(s1,MARIA);c.finalizar(s2,MARIA);
        for(String n:List.of("0","6","-1","1.5","NaN"))no(()->c.a(ANA,"valorar","sesion",""+c.sesion(s1),"puntuacion",n),"Puntuación inválida "+n);
        no(()->c.a(LUIS,"valorar","sesion",""+c.sesion(s1),"puntuacion","5"),"Sesión ajena");
        no(()->c.a(MARIA,"valorar","sesion",""+c.sesion(s1),"puntuacion","5"),"Tutor no emite opinión estudiantil");
        c.a(ANA,"valorar","sesion",""+c.sesion(s1),"puntuacion","5","comentario","<img src=x onerror=alert(1)>","autor","E002","fecha","1900");
        no(()->c.a(ANA,"valorar","sesion",""+c.sesion(s1),"puntuacion","1"),"Duplicado");
        c.a(LUIS,"valorar","sesion",""+c.sesion(s2),"puntuacion","1");
        var a=c.sistema().getSeguimiento();
        ok(a.valoraciones.size()==2&&a.valoraciones.get(0).autor().equals("E001"),"Autor del servidor");
        ok(a.valoraciones.get(0).fecha().startsWith("2026-05-15"),"Fecha del servidor");
        c.a(COORD,"evaluacion-academica","tutor","T001","curso","CC2006","dominio","1","claridad","2","fundamento","Debe reforzar ejemplos");
        no(()->c.a(ANA,"evaluacion-academica","tutor","T001","curso","CC2006","dominio","5","claridad","5","fundamento","Ajeno"),"Permiso académico");
        no(()->c.a(COORD,"evaluacion-academica","tutor","P001","curso","CC5001","dominio","5","claridad","5","fundamento","Propio"),"Autoevaluación");
        no(()->c.a(COORD,"evaluacion-academica","tutor","T001","curso","MM101","dominio","5","claridad","5","fundamento","Otro curso"),"Curso ajeno");
        String json=Json.escribir(c.web.datos(ANA));
        ok(json.contains("\"promedioValoraciones\":3.0")&&json.contains("\"cantidadValoraciones\":2"),"Promedio estudiantil independiente");
        ok(json.contains("\"promedioValoraciones\":null"),"Sin valoraciones no es cero");
        ok(c.sistema().getSeguimiento().sanciones.isEmpty(),"Notas bajas no crean sanciones");
        c.a(COORD,"eliminar-curso","curso","CC2006");c.a(COORD,"eliminar-tutor","tutor","T001");c.reiniciar();
        ok(c.sistema().getSeguimiento().valoraciones.size()==2&&c.sistema().getSeguimiento().evaluaciones.size()==1,"Retiro y reinicio conservan opiniones/evaluaciones");
    }
    private static void revision()throws Exception{
        Caso c=new Caso();c.calendario();
        no(()->c.a(ANA,"periodo-academico","periodo","X","inicio","2026-01-01","fin","2026-02-01"),"Solo coordinación configura");
        no(()->c.a(COORD,"periodo-academico","periodo","X","inicio","2026-05-20","fin","2026-06-01"),"No solapar semestres");
        no(()->c.a(COORD,"politica-sanciones","estudiante","SIGUIENTE_PERIODO","tutor","DESDE_CONFIRMACION"),"Política requiere acuerdo explícito");
        int futuro=c.reserva("2026-05-16",ANA,MARIA,true);
        no(()->c.reportar(futuro,ANA),"No reportar antes de terminar");
        int pasada=c.reserva("2026-05-10",ANA,MARIA,true);
        no(()->c.reportar(pasada,LUIS),"No reportar sesión ajena");
        int r=c.reportar(pasada,ANA);
        no(()->c.reportar(pasada,ANA),"Reporte duplicado");
        ok(c.sistema().getSeguimiento().strikesActuales("T001","tutor")==0,"Pendiente no suma strikes");
        ok(!c.sistema().getSeguimiento().suspendido("T001","tutor",LocalDate.parse(c.dia)),"Pendiente no suspende");
        no(()->c.finalizar(pasada,MARIA),"Sesión discutida no acredita compensación");
        no(()->c.a(ANA,"cancelar","solicitud",""+pasada),"Cancelación no borra revisión");
        no(()->c.a(MARIA,"resolver-ausencia","reporte",""+r,"decision","CONFIRMADO","motivo","Propio"),"Tutor no decide");
        no(()->c.resolver(r,"CONFIRMADO"),"Confirmación requiere política");
        c.a(MARIA,"explicar-ausencia","reporte",""+r,"contenido","Ausencia justificada por emergencia");
        no(()->c.a(LUIS,"explicar-ausencia","reporte",""+r,"contenido","Ajeno"),"No suplantar descargo");
        c.resolver(r,"DESCARTADO");
        ok(c.sistema().getSeguimiento().strikesActuales("T001","tutor")==0,"Justificación descartada no suma");
        ok(c.solicitud(pasada).getEstado()==EstadoSolicitud.CONFIRMADA,"Descartar conserva confirmada");
        no(()->c.resolver(r,"DESCARTADO"),"No repetir decisión");
        no(()->c.a(COORD,"periodo-academico","periodo","2026-A","inicio","2026-01-01","fin","2026-05-21"),"Semestre con reportes protegido");
        ok(!Json.escribir(c.web.datos(LUIS)).contains("Registro de sala"),"Reportes privados frente a otros alumnos");
        int cancelada=c.reserva("2026-05-09",ANA,MARIA,true);c.a(ANA,"cancelar","solicitud",""+cancelada);
        no(()->c.reportar(cancelada,MARIA),"Cancelación normal no es ausencia");
        c.politica();c.corregir(r,"CONFIRMADO");
        ok(c.solicitud(pasada).getEstado()==EstadoSolicitud.INASISTENCIA,"Nuevo cierre independiente");
        ok(c.sistema().getSeguimiento().strikesActuales("T001","tutor")==1,"Primer strike docente");
        Sancion sancion=c.sistema().getSeguimiento().sanciones.get(0);
        ok(sancion.inicio().equals("2026-05-15")&&sancion.fin().equals("2026-06-14"),"Mes calendario inclusivo");
        ok(c.sistema().getSeguimiento().afectada(c.solicitud(futuro)),"Reserva anterior afectada");
        no(()->c.finalizar(futuro,MARIA),"No impartir durante suspensión");
        no(()->c.a(MARIA,"horario","fecha","2026-05-17","inicio","10:00","fin","11:00","modalidad","PRESENCIAL","ubicacion","A"),"No publicar durante suspensión");
        var antes=c.sistema().getTutores().stream().filter(t->t.getCarnet().equals("T001")).findFirst().orElseThrow();
        ok(antes.isActivo()&&antes.getRegistroHorasBeca().getHorasAcumuladas()==0,"Suspensión no es retiro ni genera horas");
        ok(Json.escribir(c.web.datos(MARIA)).contains("Mis")||c.web.datos(MARIA).containsKey("sesiones"),"Acceso al historial durante suspensión");
        String publico=Json.escribir(c.web.datos(LUIS));
        ok(publico.contains("\"suspendido\":true")&&!publico.contains("Registro de sala"),"Perfil muestra restricción sin motivos");
        c.a(COORD,"resolver-reserva","solicitud",""+futuro,"tipo","CANCELAR","motivo","Tutor suspendido");
        ok(c.solicitud(futuro).getEstado()==EstadoSolicitud.CANCELADA,"Coord resuelve reserva previa");
        c.corregir(r,"DESCARTADO");
        ok(c.sistema().getSeguimiento().strikesActuales("T001","tutor")==0,"Corrección recalcula contador");
        ok(!c.sistema().getSeguimiento().sanciones.get(0).vigente(),"Corrección revoca sanción");
        ok(c.sistema().getSeguimiento().decisiones.size()==3,"No borra decisiones anteriores");
        ok(c.solicitud(pasada).getEstado()==EstadoSolicitud.CONFIRMADA,"Reabre sin acreditar automáticamente");
        c.reiniciar();
        ok(c.sistema().getSeguimiento().explicaciones.size()==1&&c.sistema().getSeguimiento().decisiones.size()==3,"Reinicio conserva auditoría");
        int propia=c.reserva("2026-05-08",LUIS,COORD,true);int rp=c.reportar(propia,LUIS);
        no(()->c.resolver(rp,"CONFIRMADO"),"Coordinador participante no resuelve su perfil");
        ok(c.web.datos(COORD).containsKey("demanda"),"Acceso administrativo se mantiene");
    }
    private static void tercerStrikeYReinicio()throws Exception{
        Caso c=new Caso();c.calendario();c.politica();
        int[] solicitudes=new int[4],reportes=new int[4];
        for(int i=0;i<4;i++){solicitudes[i]=c.reserva("2026-05-0"+(i+1),ANA,MARIA,true);reportes[i]=c.reportar(solicitudes[i],MARIA);}
        int agosto=c.reserva("2026-08-10",ANA,MARIA,true);
        c.resolver(reportes[0],"CONFIRMADO");c.resolver(reportes[1],"CONFIRMADO");
        ok(c.sistema().getSeguimiento().sanciones.isEmpty(),"Dos strikes sin suspensión estudiantil");
        c.resolver(reportes[2],"CONFIRMADO");
        var s=c.sistema().getSeguimiento().sanciones.get(0);
        ok(s.inicio().equals("2026-08-03")&&s.fin().equals("2026-12-04"),"Semestre real completo, no 180 días");
        no(()->c.a(ANA,"solicitar","tutor","T001","curso","CC2006","horario","2026-08-10|10:00|11:00","tema","X"),"Fecha bloqueada por sanción futura");
        no(()->c.finalizar(agosto,MARIA),"No acreditar reserva anterior dentro del semestre suspendido");
        c.resolver(reportes[3],"CONFIRMADO");
        ok(c.sistema().getSeguimiento().sanciones.stream().filter(Sancion::vigente).count()==1,"Cuarto strike no duplica semestre");
        c.corregir(reportes[0],"DESCARTADO");
        ok(c.sistema().getSeguimiento().strikesActuales("E001","estudiante")==3,"Corrección conserva tres restantes");
        ok(c.sistema().getSeguimiento().sanciones.stream().filter(Sancion::vigente).count()==1,"Recalcula origen de umbral");
        c.corregir(reportes[1],"DESCARTADO");
        ok(c.sistema().getSeguimiento().sanciones.stream().noneMatch(Sancion::vigente),"Menos de tres revoca derivadas");
        c.corregir(reportes[0],"CONFIRMADO");
        c.fecha("2026-08-04");c.reiniciar();
        ok(c.sistema().getSeguimiento().strikesActuales("E001","estudiante")==0,"Nuevo semestre estudiante en cero tras servidor apagado");
        ok(c.sistema().getSeguimiento().suspendido("E001","estudiante",LocalDate.parse(c.dia)),"Contador reinicia pero suspensión continúa");
        ok(c.sistema().getSeguimiento().strikes("E001","estudiante","2026-A")==3,"Historial de strikes se conserva");
        no(()->c.reserva("2027-02-01",ANA,DIEGO,false),"Estudiante suspendido no solicita aunque el bloque sea posterior");
        c.fecha("2026-12-05");
        ok(!c.sistema().getSeguimiento().suspendido("E001","estudiante",LocalDate.parse(c.dia)),"Vencimiento posterior al último día");
    }
    private static void tardiosRolesYReasignacion()throws Exception{
        Caso c=new Caso();c.calendario();c.politica();
        int uno=c.reserva("2026-05-01",ANA,MARIA,true), dos=c.reserva("2026-05-02",LUIS,MARIA,true);
        int agosto=c.reserva("2026-08-10",ANA,MARIA,true);
        int r1=c.reportar(uno,ANA),r2=c.reportar(dos,LUIS);
        c.fecha("2026-08-02");c.resolver(r1,"CONFIRMADO");
        ok(c.sistema().getSeguimiento().sanciones.get(0).fin().equals("2026-09-01"),"Mes cruza semestre");
        c.fecha("2026-08-03");c.reiniciar();
        ok(c.sistema().getSeguimiento().strikesActuales("T001","tutor")==0,"Tutor empieza semestre en cero");
        ok(c.sistema().getSeguimiento().suspendido("T001","tutor",LocalDate.parse(c.dia)),"Suspensión docente cruza semestre");
        c.resolver(r2,"CONFIRMADO");
        ok(c.sistema().getSeguimiento().strikes("T001","tutor","2026-A")==2,"Resolución tardía cuenta semestre de la sesión");
        ok(c.sistema().getSeguimiento().strikesActuales("T001","tutor")==0,"Resolución tardía no aumenta semestre nuevo");
        c.a(COORD,"aprobar","tutor","T002","curso","CC2006");
        c.a(DIEGO,"horario","fecha","2026-08-10","inicio","12:00","fin","13:00","modalidad","PRESENCIAL","ubicacion","C-1");
        c.a(COORD,"resolver-reserva","solicitud",""+agosto,"tipo","REASIGNAR","tutor","T002","horario","2026-08-10|12:00|13:00","motivo","Reasignación acordada");
        var nueva=c.sistema().getSolicitudes().get(3);
        ok(c.solicitud(agosto).getEstado()==EstadoSolicitud.CANCELADA&&nueva.getEstado()==EstadoSolicitud.PENDIENTE,"Reasigna sin modificar histórico ni finalizar");
        ok(nueva.getTutor().getCarnet().equals("T002")&&nueva.getPrecio()==40,"Nueva reserva conserva condiciones propias");
        c.a(COORD,"estudiante","carnet","T001","nombre","María","correo","dual@example.com","anio","3","clave","Segura2026!");
        ok(c.sistema().getSeguimiento().strikesActuales("T001","estudiante")==0,"Rol estudiante no hereda strikes docente");
        Seguridad.Identidad dual=new Seguridad.Identidad("estudiante","T001");
        c.reserva("2026-08-11",dual,DIEGO,false);
        ok(c.web.datos(dual).containsKey("solicitudes"),"Tutor suspendido puede usar rol estudiantil sin sanción");
        c.reiniciar();
        ok(c.sistema().getSeguimiento().resoluciones.size()==1,"Reasignación persiste");
    }
    private static void politicasAlternativas()throws Exception{
        Caso c=new Caso();c.calendario();c.politica("FECHAS_CONFIRMACION","ACUMULAR_AL_FINAL");
        int r1=c.reportar(c.reserva("2026-05-01",ANA,MARIA,true),ANA);
        int r2=c.reportar(c.reserva("2026-05-02",LUIS,MARIA,true),LUIS);
        c.resolver(r1,"CONFIRMADO");c.resolver(r2,"CONFIRMADO");
        var sanciones=c.sistema().getSeguimiento().sanciones;
        ok(sanciones.get(1).inicio().equals("2026-06-15")&&sanciones.get(1).fin().equals("2026-07-14"),"Mes adicional solo con política explícita");
        c.corregir(r1,"DESCARTADO");
        ok(!c.sistema().getSeguimiento().sanciones.get(0).vigente()&&c.sistema().getSeguimiento().sanciones.get(1).vigente(),"Corrección conserva fechas exactas de otra sanción");
        Caso e=new Caso();e.calendario();e.politica("FECHAS_CONFIRMACION","DESDE_CONFIRMACION");
        for(int i=1;i<=3;i++){
            int r=e.reportar(e.reserva("2026-05-0"+i,ANA,MARIA,true),MARIA);
            no(()->e.resolver(r,"CONFIRMADO"),"Fechas manuales obligatorias");
            e.a(COORD,"resolver-ausencia","reporte",""+r,"decision","CONFIRMADO","motivo","Fechas acordadas por equipo","inicio","2026-05-15","fin","2026-12-04");
        }
        ok(e.sistema().getSeguimiento().sanciones.get(0).inicio().equals("2026-05-15"),"Alternativa desde confirmación");
        ok(e.sistema().getSeguimiento().suspendido("E001","estudiante",LocalDate.parse(e.dia)),"Tercer strike suspende inmediatamente según política");
    }
    private static void reportesCruzados() throws Exception {
        Caso c=new Caso();c.calendario();c.politica();
        int sol=c.reserva("2026-05-01",ANA,MARIA,true);
        int contraTutor=c.reportar(sol,ANA), contraEstudiante=c.reportar(sol,MARIA);
        c.resolver(contraTutor,"CONFIRMADO");c.resolver(contraEstudiante,"CONFIRMADO");
        c.corregir(contraTutor,"DESCARTADO");
        ok(c.solicitud(sol).getEstado()==EstadoSolicitud.INASISTENCIA,"Otra ausencia confirmada conserva cierre");
        ok(c.sistema().getSeguimiento().strikesActuales("T001","tutor")==0
                && c.sistema().getSeguimiento().strikesActuales("E001","estudiante")==1,"Reportes cruzados se revisan por persona y rol");
        c.corregir(contraEstudiante,"DESCARTADO");
        ok(c.solicitud(sol).getEstado()==EstadoSolicitud.CONFIRMADA,"Reapertura solo al descartar todas las confirmaciones");
        // La sanción docente no debe quitar permisos administrativos del mismo profesor.
        var estado=c.repo.cargar();var a=estado.sistema().getSeguimiento();
        a.sanciones.add(new Sancion(2,contraTutor,1,"P001","tutor","2026-A","2026-05-15","2026-06-14",
                "Fixture de sanción docente previamente resuelta",true,"",""));
        // Este caso se verifica en el modelo sin fabricar un coordinador adicional ni persistir un origen ajeno.
        ok(a.suspendido("P001","tutor",LocalDate.parse(c.dia)),"Sanción del profesor se evalúa como tutor");
        new CoordinadorController(estado.sistema()).registrarCurso("ADMIN78","Administración durante suspensión");
        ok(estado.sistema().getCursos().stream().anyMatch(x->x.getCodigo().equals("ADMIN78")),"La sanción docente no afecta las acciones administrativas");
    }
    private static void migracionAnterior() throws Exception {
        Caso c=new Caso();
        int id=c.reserva("2026-05-01",ANA,MARIA,true);
        try(var conexion=c.db.conectar();var st=conexion.createStatement()) {
            java.util.List<String> restricciones=new java.util.ArrayList<>();
            try(var r=st.executeQuery("SELECT tc.constraint_name FROM information_schema.table_constraints tc "
                    + "JOIN information_schema.check_constraints cc ON tc.constraint_name=cc.constraint_name "
                    + "AND tc.constraint_schema=cc.constraint_schema WHERE tc.table_name='solicitudes' "
                    + "AND UPPER(cc.check_clause) LIKE '%ESTADO%'")) {
                while(r.next()) restricciones.add(r.getString(1));
            }
            for(String nombre:restricciones) st.execute("ALTER TABLE solicitudes DROP CONSTRAINT \""+nombre+"\"");
            st.execute("ALTER TABLE solicitudes ADD CONSTRAINT estado_antiguo CHECK(estado IN ('PENDIENTE','CONFIRMADA','CANCELADA','FINALIZADA'))");
            st.executeUpdate("UPDATE sesiones SET ausente=TRUE WHERE solicitud="+id);
            st.executeUpdate("UPDATE solicitudes SET estado='CANCELADA' WHERE id="+id);
        }
        c.reiniciar(); c.reiniciar();
        ok(c.solicitud(id).getEstado()==EstadoSolicitud.INASISTENCIA,"Migración de ausencia anterior distingue inasistencia");
        ok(c.sistema().getSeguimiento().reportes.isEmpty()&&c.sistema().getSeguimiento().sanciones.isEmpty(),"Migración no inventa responsables ni strikes");
        var registro=c.repo.cargar();c.repo.guardar(registro,null);c.reiniciar();
        ok(!c.sistema().getSesiones().get(0).isFinalizada(),"Ausencia migrada no genera compensación");
        try(var conexion=c.db.conectar();var st=conexion.createStatement()) {
            boolean rechazo=false;
            try{st.executeUpdate("UPDATE solicitudes SET estado='INVALIDO' WHERE id="+id);}catch(java.sql.SQLException ex){rechazo=true;}
            ok(rechazo,"SQL sigue rechazando estados desconocidos");
        }
    }
    public static void main(String[] args)throws Exception{
        valoraciones();revision();tercerStrikeYReinicio();tardiosRolesYReasignacion();politicasAlternativas();migracionAnterior();reportesCruzados();
        System.out.println("CORRECTO: "+pruebas+" comprobaciones de valoraciones, ausencias, sanciones y semestres superadas.");
    }
}
