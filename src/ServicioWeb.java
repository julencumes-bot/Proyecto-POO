import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Capa de aplicación: autorización, validación HTTP, controladores y transacción. */
public class ServicioWeb {
    private final RepositorioTutorias repo;
    public ServicioWeb(RepositorioTutorias repo) { this.repo=repo; }
    public synchronized void inicializar(boolean demo) throws SQLException {
        var estado=repo.cargar();
        if(estado.sistema()!=null) { return; }
        SistemaTutorias s;
        if(demo) {
            ProfesorCoordinador p=new ProfesorCoordinador("Prof. Rivera","P001","rivera@example.com");
            s=new SistemaTutorias(p);
            Curso poo=new Curso("CC2006","Programación Orientada a Objetos"), mate=new Curso("MM101","Matemática"), avanzado=new Curso("CC5001","Algoritmos avanzados");
            s.agregarCurso(poo);s.agregarCurso(mate);s.agregarCurso(avanzado);
            s.agregarEstudiante(new Estudiante("Ana López","E001","ana@example.com",2));
            s.agregarEstudiante(new Estudiante("Luis Méndez","E002","luis@example.com",5));
            Tutor maria=new Tutor("María Pérez","T001","maria@example.com",3,true,0);
            Tutor diego=new Tutor("Diego Ruiz","T002","diego@example.com",5,false,40);
            s.agregarTutor(maria);s.agregarTutor(diego);
            p.aprobarTutor(maria,poo);p.aprobarTutor(diego,mate);p.aprobarTutor(diego,avanzado);p.aprobarTutor(p.getPerfilTutor(),avanzado);
            LocalDate dia=LocalDate.now(ZoneId.of("America/Guatemala")).plusDays(1);
            for(int i=0;i<5;i++) {
                maria.agregarHorario(new Horario(dia.plusDays(i).toString(),"14:00","15:00"));
                diego.agregarHorario(new Horario(dia.plusDays(i).toString(),"09:00","10:30"));
                p.getPerfilTutor().agregarHorario(new Horario(dia.plusDays(i).toString(),"16:00","17:00"));
            }
        } else { s=new SistemaTutorias(new ProfesorCoordinador("Coordinación",BaseDatos.env("ADMIN_ID","P001"),BaseDatos.env("ADMIN_EMAIL","coordinacion@example.com"))); }
        repo.guardar(new RepositorioTutorias.Estado(s,estado.revision()),null);
    }
    public static Map<String,Object> obj(Object... kv) {
        Map<String,Object> r=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2) { r.put((String)kv[i],kv[i+1]); }return r;
    }
    static Estudiante estudiante(SistemaTutorias s,String id) {
        for(Estudiante e:s.getEstudiantes()) { if(e.getCarnet().equals(id)) { return e; } }throw new IllegalArgumentException("Estudiante no encontrado.");
    }
    static Tutor tutor(SistemaTutorias s,String id) {
        for(Tutor t:s.getTutores()) { if(t.getCarnet().equals(id)) { return t; } }throw new IllegalArgumentException("Tutor no encontrado.");
    }
    static Curso curso(SistemaTutorias s,String id) {
        for(Curso c:s.getCursos()) { if(c.getCodigo().equals(id)) { return c; } }throw new IllegalArgumentException("Curso no encontrado.");
    }
    static SolicitudTutoria solicitud(SistemaTutorias s,String id) {
        for(SolicitudTutoria sol:s.getSolicitudes()) { if(Integer.toString(sol.getIdSolicitud()).equals(id)) { return sol; } }throw new IllegalArgumentException("Solicitud no encontrada.");
    }
    static SesionTutoria sesion(SistemaTutorias s,String id) {
        for(SesionTutoria ses:s.getSesiones()) { if(Integer.toString(ses.getIdSesion()).equals(id)) { return ses; } }throw new IllegalArgumentException("Sesión no encontrada.");
    }
    static String horarioId(Horario h) { return h.getFecha()+"|"+h.getHoraInicio()+"|"+h.getHoraFin(); }
    static Horario horario(Tutor t,String id) {
        for(Horario h:t.getDisponibilidad()) { if(horarioId(h).equals(id)) { return h; } }throw new IllegalArgumentException("Horario no encontrado.");
    }
    private static void rol(Seguridad.Identidad id,String requerido) {
        if(!id.rol().equals(requerido)) { throw new SecurityException("Esta acción no está disponible para tu perfil."); }
    }
    private static Tutor tutorActual(SistemaTutorias s,Seguridad.Identidad id) {
        if(!id.rol().equals("tutor") && !id.rol().equals("coordinador")) { throw new SecurityException("Se requiere un perfil de tutor."); }
        return tutor(s,id.carnet());
    }
    private static String texto(Map<String,String> f,String campo,int max) {
        String v=Validacion.texto(f.get(campo),campo);
        if(v.length()>max) { throw new IllegalArgumentException(campo+" excede el máximo de "+max+" caracteres."); }return v;
    }
    private static String identificador(Map<String,String> f,String campo) {
        String v=texto(f,campo,80);
        if(!v.matches("[A-Za-z0-9_-]+")) { throw new IllegalArgumentException("El identificador solo admite letras, números, guion y guion bajo."); }return v;
    }
    private static String correo(Map<String,String> f) {
        String v=texto(f,"correo",200);
        if(!v.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) { throw new IllegalArgumentException("Escribe un correo válido."); }return v;
    }
    public synchronized void ejecutar(Seguridad.Identidad id,String accion,Map<String,String> f) throws SQLException {
        var estado=repo.cargar(); SistemaTutorias s=estado.sistema();
        var ec=new EstudianteController(s);var tc=new TutorController(s);var cc=new CoordinadorController(s);
        Seguridad.Cuenta nueva=null;
        switch(accion) {
            case "solicitar" -> {
                rol(id,"estudiante");Tutor t=tutor(s,f.get("tutor"));
                ec.solicitarTutoria(estudiante(s,id.carnet()),curso(s,f.get("curso")),t,texto(f,"tema",2000),horario(t,f.get("horario")));
            }
            case "cancelar" -> {
                var sol=solicitud(s,f.get("solicitud"));
                if(id.rol().equals("estudiante")) { ec.cancelarTutoria(estudiante(s,id.carnet()),sol); }
                else { tc.cancelarTutoria(tutorActual(s,id),sol); }
            }
            case "preguntar" -> { rol(id,"estudiante");ec.registrarPregunta(estudiante(s,id.carnet()),solicitud(s,f.get("solicitud")),texto(f,"contenido",2000)); }
            case "aceptar" -> tc.aceptarTutoria(tutorActual(s,id),solicitud(s,f.get("solicitud")));
            case "finalizar" -> {
                String proximo=f.getOrDefault("proximo","").trim();if(proximo.length()>2000) { throw new IllegalArgumentException("El próximo tema es demasiado largo."); }
                tc.finalizarSesion(tutorActual(s,id),sesion(s,f.get("sesion")),texto(f,"temas",2000),texto(f,"notas",2000),proximo);
            }
            case "horario" -> tc.agregarHorario(tutorActual(s,id),texto(f,"fecha",10),texto(f,"inicio",5),texto(f,"fin",5));
            case "aprobar" -> { rol(id,"coordinador");cc.aprobarTutor(tutor(s,f.get("tutor")),curso(s,f.get("curso"))); }
            case "curso" -> { rol(id,"coordinador");cc.registrarCurso(identificador(f,"codigo"),texto(f,"nombre",200)); }
            case "estudiante", "tutor" -> {
                rol(id,"coordinador");String carnet=identificador(f,"carnet"),nombre=texto(f,"nombre",200),email=correo(f);
                int anio=Integer.parseInt(texto(f,"anio",2));if(anio<1 || anio>10) { throw new IllegalArgumentException("El año de carrera debe estar entre 1 y 10."); }
                nueva=Seguridad.crear(accion,carnet,f.get("clave"));
                if(accion.equals("estudiante")) { cc.registrarEstudiante(nombre,carnet,email,anio); }
                else { cc.registrarTutor(nombre,carnet,email,anio,"on".equals(f.get("beca")),Double.parseDouble(texto(f,"tarifa",20))); }
            }
            default -> throw new IllegalArgumentException("Acción desconocida.");
        }
        repo.guardar(estado,nueva);
    }
    private Map<String,Object> solicitudJson(SolicitudTutoria sol) {
        return obj("id",sol.getIdSolicitud(),"estudiante",sol.getEstudiante().getCarnet(),"nombreEstudiante",sol.getEstudiante().getNombre(),"tutor",sol.getTutor().getCarnet(),"nombreTutor",sol.getTutor().getNombre(),"curso",sol.getCurso().getCodigo(),"nombreCurso",sol.getCurso().getNombre(),"tema",sol.getTema(),"fecha",sol.getHorario().getFecha(),"inicio",sol.getHorario().getHoraInicio(),"fin",sol.getHorario().getHoraFin(),"estado",sol.getEstado().name(),"precio",sol.getPrecio());
    }
    private boolean visible(SolicitudTutoria sol,Seguridad.Identidad id) {
        return id.rol().equals("coordinador") || (id.rol().equals("estudiante") ? sol.getEstudiante().getCarnet().equals(id.carnet()) : sol.getTutor().getCarnet().equals(id.carnet()));
    }
    public synchronized Map<String,Object> datos(Seguridad.Identidad id) throws SQLException {
        SistemaTutorias s=repo.cargar().sistema();var ec=new EstudianteController(s);var cc=new CoordinadorController(s);
        Usuario u=id.rol().equals("coordinador") ? s.getCoordinador() : id.rol().equals("estudiante") ? estudiante(s,id.carnet()) : tutor(s,id.carnet());
        Estudiante e=id.rol().equals("estudiante") ? (Estudiante)u : null;
        List<Object> cursos=new ArrayList<>(),tutores=new ArrayList<>(),estudiantes=new ArrayList<>(),solicitudes=new ArrayList<>(),sesiones=new ArrayList<>(),preguntas=new ArrayList<>();
        for(Curso c:s.getCursos()) {
            Tutor alternativa=e==null ? null : cc.buscarTutorAlternativo(c,e);
            cursos.add(obj("codigo",c.getCodigo(),"nombre",c.getNombre(),"alternativa",alternativa==null ? "" : alternativa.getCarnet()));
        }
        for(Tutor t:s.getTutores()) {
            if(e!=null && (!t.isAprobado() || t.getCarnet().equals(e.getCarnet()))) { continue; }
            if(id.rol().equals("tutor") && !t.getCarnet().equals(id.carnet())) { continue; }
            List<Object> horarios=new ArrayList<>();
            for(Horario h:t.getDisponibilidad()) {
                boolean libre=h.isDisponible() && !s.tieneConflicto(t.getCarnet(),h) && (e==null || !s.tieneConflicto(e.getCarnet(),h));
                if(e==null || libre) { horarios.add(obj("id",horarioId(h),"fecha",h.getFecha(),"inicio",h.getHoraInicio(),"fin",h.getHoraFin(),"duracion",h.getDuracionHoras(),"disponible",libre)); }
            }
            List<String> asignados=new ArrayList<>();for(Curso c:t.getCursosAsignados()) { asignados.add(c.getCodigo()); }
            Map<String,Object> tj=obj("carnet",t.getCarnet(),"nombre",t.getNombre(),"anio",t.getAnioCarrera(),"beca",t.isRecibeHorasBeca(),"tarifa",t.getTarifaHora(),"aprobado",t.isAprobado(),"profesor",t==s.getCoordinador().getPerfilTutor(),"cursos",asignados,"horarios",horarios);
            if(e==null) { tj.put("correo",t.getCorreo());tj.put("remuneracion",t.getRemuneracionAcumulada());tj.put("horasBeca",t.getRegistroHorasBeca().getHorasAcumuladas()); }
            tutores.add(tj);
        }
        if(id.rol().equals("coordinador")) {
            for(Estudiante est:s.getEstudiantes()) { estudiantes.add(obj("carnet",est.getCarnet(),"nombre",est.getNombre(),"correo",est.getCorreo(),"anio",est.getAnioCarrera(),"recibeTutorias",cc.consultarEstudiantesConTutorias().contains(est))); }
        }
        for(SolicitudTutoria sol:s.getSolicitudes()) { if(visible(sol,id)) { solicitudes.add(solicitudJson(sol)); } }
        for(SesionTutoria ses:s.getSesiones()) { if(visible(ses.getSolicitud(),id)) { sesiones.add(obj("id",ses.getIdSesion(),"solicitud",ses.getSolicitud().getIdSolicitud(),"temas",ses.getTemasVistos(),"notas",ses.getNotas(),"proximo",ses.getTemaProximaTutoria(),"finalizada",ses.isFinalizada(),"duracion",ses.getDuracionHoras())); } }
        for(Pregunta q:s.getPreguntas()) { if(visible(q.getSolicitud(),id)) { preguntas.add(obj("id",q.getIdPregunta(),"solicitud",q.getSolicitud().getIdSolicitud(),"contenido",q.getContenido(),"estudiante",q.getEstudiante().getNombre())); } }
        return obj("usuario",obj("nombre",u.getNombre(),"carnet",u.getCarnet(),"rol",id.rol()),"cursos",cursos,"tutores",tutores,"estudiantes",estudiantes,"solicitudes",solicitudes,"sesiones",sesiones,"preguntas",preguntas,"demanda",id.rol().equals("coordinador") ? cc.consultarDemanda() : "");
    }
}
