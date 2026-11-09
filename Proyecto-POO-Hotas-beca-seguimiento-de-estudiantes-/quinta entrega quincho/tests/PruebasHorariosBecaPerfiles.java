import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Casos de entrega 4, 5 y 6. Usa una base temporal; no modifica data/tutorias. */
public class PruebasHorariosBecaPerfiles {
    private static int pruebas;
    private static BaseDatos db;
    private static ServicioWeb web;
    private static RepositorioTutorias repo;
    private static final Seguridad.Identidad ANA = new Seguridad.Identidad("estudiante", "E001");
    private static final Seguridad.Identidad LUIS = new Seguridad.Identidad("estudiante", "E002");
    private static final Seguridad.Identidad MARIA = new Seguridad.Identidad("tutor", "T001");
    private static final Seguridad.Identidad DIEGO = new Seguridad.Identidad("tutor", "T002");
    private static final Seguridad.Identidad COORD = new Seguridad.Identidad("coordinador", "P001");
    private interface Accion { void ejecutar() throws Exception; }
    private static void verificar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        pruebas++;
    }
    private static void rechazar(Accion accion, String mensaje) throws Exception {
        boolean rechazo = false;
        try { accion.ejecutar(); }
        catch (IllegalArgumentException | IllegalStateException | SecurityException error) { rechazo = true; }
        verificar(rechazo, mensaje);
    }
    private static Map<String, String> datos(String... pares) {
        Map<String, String> r = new LinkedHashMap<>();
        for (int i = 0; i < pares.length; i += 2) r.put(pares[i], pares[i + 1]);
        return r;
    }
    private static void accion(Seguridad.Identidad id, String accion, String... pares) throws Exception {
        web.ejecutar(id, accion, datos(pares));
    }
    private static Tutor tutor(String carnet) throws Exception { return ServicioWeb.tutor(repo.cargar().sistema(), carnet); }
    private static SolicitudTutoria solicitud(int id) throws Exception {
        return ServicioWeb.solicitud(repo.cargar().sistema(), Integer.toString(id));
    }
    private static String bloque(String dia) { return "2027-02-" + dia + "|10:00|11:30"; }
    private static void publicar(String dia, String modalidad, String lugar) throws Exception {
        accion(MARIA, "horario", "fecha", "2027-02-"+dia, "inicio", "10:00", "fin", "11:30", "modalidad", modalidad, "ubicacion", lugar);
    }
    private static int reservar(Seguridad.Identidad quien, String tutor, String curso, String horario) throws Exception {
        accion(quien, "solicitar", "tutor", tutor, "curso", curso, "horario", horario, "tema", "Clases y objetos");
        return repo.cargar().sistema().getSolicitudes().size();
    }
    private static int aceptar(Seguridad.Identidad quien, int solicitud) throws Exception {
        accion(quien, "aceptar", "solicitud", ""+solicitud);
        return repo.cargar().sistema().getSesiones().size();
    }
    private static void finalizar(Seguridad.Identidad quien, int sesion, String duracion) throws Exception {
        accion(quien, "finalizar", "sesion", ""+sesion, "temas", "Herencia", "notas", "Avance compartido", "proximo", "Polimorfismo", "duracion", duracion);
    }
    private static void factor(int n) throws Exception {
        accion(COORD, "editar-tutor", "tutor", "T001", "nombre", "María Pérez", "correo", "maria@example.com", "anio", "3", "beca", "on", "tarifa", "0", "multiplicador", ""+n);
    }
    public static void main(String[] args) throws Exception {
        var carpeta = Files.createTempDirectory("poo-requisitos-456-");
        db = new BaseDatos("jdbc:h2:"+carpeta.resolve("db")+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        db.inicializar(); repo = new RepositorioTutorias(db); web = new ServicioWeb(repo); web.inicializar(true);
        probarHorarios(); probarBeca(); probarPerfiles(); probarMigracion();
        System.out.println("CORRECTO: " + pruebas + " comprobaciones de horarios, beca y perfiles superadas.");
    }
    private static void probarHorarios() throws Exception {
        rechazar(() -> publicar("01", "", "B-101"), "Modalidad obligatoria");
        rechazar(() -> accion(MARIA, "horario", "fecha", "2027-02-01", "inicio", "10:00", "fin", "11:30"), "Modalidad ausente");
        for (String modo : new String[]{"EN_LINEA", "PRESENCIAL"}) {
            rechazar(() -> publicar("01", modo, " "), "Ubicación obligatoria " + modo);
        }
        for (String url : new String[]{"meet.example.com", "javascript:alert(1)", "https://", "https://meet.example.com/a b", "https://user:pass@meet.example.com"}) {
            rechazar(() -> publicar("01", "EN_LINEA", url), "Enlace inválido " + url);
        }
        accion(MARIA, "enlace-permanente", "enlace", "https://meet.example.com/permanente?clave=privada");
        accion(MARIA, "horario", "fecha", "2027-02-01", "inicio", "10:00", "fin", "11:30", "modalidad", "VIRTUAL");
        publicar("02", "PRESENCIAL", "Salón B-205");
        accion(MARIA, "enlace-permanente", "enlace", "https://meet.example.com/nuevo");
        String original = "https://meet.example.com/permanente?clave=privada";
        verificar(ServicioWeb.horario(tutor("T001"), bloque("01")).getUbicacion().equals(original), "Predeterminado no cambia bloques publicados");
        String directorio = Json.escribir(web.datos(ANA));
        verificar(!directorio.contains("meet.example.com"), "No hay enlaces virtuales en el directorio general");
        verificar(directorio.contains("Salón B-205"), "Ubicación presencial visible al elegir");
        rechazar(() -> accion(ANA, "solicitar", "tutor", "T001", "curso", "CC2006", "horario", bloque("01"), "tema", "A", "modalidad", "PRESENCIAL"), "Alumno no cambia modalidad");
        rechazar(() -> accion(ANA, "solicitar", "tutor", "T001", "curso", "CC2006", "horario", bloque("01"), "tema", "A", "ubicacion", "https://otro.example.com"), "Alumno no cambia ubicación");
        int virtual = reservar(ANA, "T001", "CC2006", bloque("01"));
        int presencial = reservar(ANA, "T001", "CC2006", bloque("02"));
        verificar(solicitud(virtual).getUbicacion().equals(original), "Copia completa de enlace");
        verificar(solicitud(presencial).getUbicacion().equals("Salón B-205"), "Copia de salón");
        for (Seguridad.Identidad id : List.of(ANA, MARIA, COORD)) {
            String vista = Json.escribir(web.datos(id));
            verificar(vista.contains(original) && vista.contains("Salón B-205"), "Ambos participantes y coordinación ven los detalles: " + id.rol());
        }
        verificar(!Json.escribir(web.datos(LUIS)).contains(original), "Otro estudiante no recibe el enlace");
        rechazar(() -> reservar(LUIS, "T001", "CC2006", bloque("01")), "Impedir doble reserva");
        rechazar(() -> accion(MARIA, "completar-horario", "horario", bloque("01"), "modalidad", "PRESENCIAL", "ubicacion", "Nuevo salón"), "Bloque reservado no cambia");
        rechazar(() -> solicitud(virtual).getHorario().actualizarCondiciones(ModalidadTutoria.PRESENCIAL, "Nuevo"), "Modelo bloquea cambios reservados");
        accion(MARIA, "cancelar", "solicitud", ""+virtual);
        accion(MARIA, "completar-horario", "horario", bloque("01"), "modalidad", "EN_LINEA", "ubicacion", "https://meet.example.com/bloque-editado");
        verificar(solicitud(virtual).getUbicacion().equals(original), "Reserva cancelada conserva copia al editar bloque libre");
        int nueva = reservar(ANA, "T001", "CC2006", bloque("01"));
        verificar(solicitud(nueva).getUbicacion().endsWith("bloque-editado"), "Nueva reserva usa bloque editado");
        accion(MARIA, "cancelar", "solicitud", ""+nueva);
        accion(MARIA, "cancelar", "solicitud", ""+presencial);
        verificar(tutor("T001").getRegistroHorasBeca().getHorasAcumuladas() == 0, "Cancelaciones sin horas beca");
    }
    private static void probarBeca() throws Exception {
        verificar(tutor("T001").getMultiplicadorBeca()==1, "Factor inicial 1");
        for (int n : new int[]{0, -1, 4, 6}) {
            rechazar(() -> tutor("T001").setMultiplicadorBeca(n), "Modelo valida factor " + n);
            rechazar(() -> factor(n), "Servidor valida factor " + n);
        }
        rechazar(() -> accion(MARIA, "editar-tutor", "tutor", "T001", "multiplicador", "5"), "Solo coordinación cambia el factor");
        accion(MARIA, "perfil-academico", "multiplicador", "5");
        verificar(tutor("T001").getMultiplicadorBeca()==1, "Perfil académico no cambia beca");
        double total = 0;
        int i = 3;
        for (int n : new int[]{1, 2, 3, 5}) {
            factor(n); String dia = String.format("%02d", i++);
            publicar(dia, "PRESENCIAL", "Biblioteca");
            int sol = reservar(ANA, "T001", "CC2006", bloque(dia));
            factor(n == 5 ? 1 : 5);
            verificar(solicitud(sol).getMultiplicadorBeca()==n && solicitud(sol).getPrecio()==0, "Factor pactado y gratuidad " + n);
            int ses = aceptar(MARIA, sol);
            for (String duracion : new String[]{"0", "-1", "NaN", "Infinity", "25"}) {
                rechazar(() -> finalizar(MARIA, ses, duracion), "Duración inválida " + duracion);
            }
            finalizar(MARIA, ses, "1.5"); total += 1.5*n;
            verificar(tutor("T001").getRegistroHorasBeca().getHorasAcumuladas()==total, "Crédito 1.5 × " + n);
            verificar(ServicioWeb.sesion(repo.cargar().sistema(), ""+ses).getDuracionHoras()==1.5, "Duración no multiplicada");
            rechazar(() -> finalizar(MARIA, ses, "1.5"), "No acreditar por segunda vez");
            verificar(tutor("T001").getRegistroHorasBeca().getHorasAcumuladas()==total, "Repetición conserva crédito");
        }
        publicar("07", "PRESENCIAL", "A-101");
        int ausente=aceptar(MARIA, reservar(ANA,"T001","CC2006",bloque("07")));
        accion(MARIA, "ausencia", "sesion", ""+ausente);
        rechazar(() -> finalizar(MARIA, ausente, "1.5"), "Ausencia no finaliza con crédito");
        verificar(tutor("T001").getRegistroHorasBeca().getHorasAcumuladas()==total, "Ausencia sin crédito");
        factor(3); publicar("08", "PRESENCIAL", "A-102");
        int antigua = reservar(ANA,"T001","CC2006",bloque("08"));
        accion(COORD,"editar-tutor","tutor","T001","nombre","María Pérez","correo","maria@example.com","anio","3","tarifa","40","multiplicador","5");
        finalizar(MARIA,aceptar(MARIA,antigua),"1.5"); total += 4.5;
        verificar(solicitud(antigua).isRecibeHorasBeca() && solicitud(antigua).getPrecio()==0, "Reserva beca se conserva al cambiar a pago");
        publicar("09", "PRESENCIAL", "A-102");
        int pago=reservar(ANA,"T001","CC2006",bloque("09"));
        verificar(!solicitud(pago).isRecibeHorasBeca() && solicitud(pago).getMultiplicadorBeca()==1 && solicitud(pago).getPrecio()==60, "Tutor remunerado no multiplica precio");
        finalizar(MARIA,aceptar(MARIA,pago),"1.25");
        verificar(tutor("T001").getRemuneracionAcumulada()==60 && tutor("T001").getRegistroHorasBeca().getHorasAcumuladas()==total, "Pago pactado y beca histórica separados");
        db.inicializar(); web=new ServicioWeb(new RepositorioTutorias(db));
        verificar(tutor("T001").getRegistroHorasBeca().getHorasAcumuladas()==21 && tutor("T001").getRemuneracionAcumulada()==60, "Reinicio reconstruye 21 horas y Q60");
    }
    private static void probarPerfiles() throws Exception {
        verificar(web.perfilEstudiante(MARIA,"E001").get("finalizadas").equals(6), "Contador usa seis sesiones finalizadas");
        rechazar(() -> web.perfilEstudiante(MARIA,"E002"), "Tutor no consulta alumno ajeno");
        rechazar(() -> web.perfilEstudiante(ANA,"E002"), "Alumno no consulta perfiles internos");
        verificar(web.perfilEstudiante(COORD,"E002").get("finalizadas").equals(0), "Coordinación consulta a todos");
        int ses=repo.cargar().sistema().getSesiones().get(0).getIdSesion();
        accion(MARIA,"comentario-academico","sesion",""+ses,"contenido","Dificultad con herencia; recomendar ejercicios","autor","P001","fecha","1900-01-01");
        var comentario=ServicioWeb.estudiante(repo.cargar().sistema(),"E001").getComentarios().get(0);
        verificar(comentario.getAutor().getCarnet().equals("T001") && !comentario.getFecha().startsWith("1900"), "Autor y fecha se generan en servidor");
        verificar(comentario.getEstudiante().getCarnet().equals("E001") && comentario.getSesion().getIdSesion()==ses, "Comentario vincula estudiante y sesión");
        rechazar(() -> accion(DIEGO,"comentario-academico","sesion",""+ses,"contenido","Ajeno"), "Tutor ajeno no comenta");
        rechazar(() -> accion(COORD,"comentario-academico","sesion",""+ses,"contenido","Ajeno"), "Coordinador tampoco comenta sesiones que no impartió");
        rechazar(() -> accion(ANA,"comentario-academico","sesion",""+ses,"contenido","Ajeno"), "Alumno no escribe observaciones internas");
        verificar(!Json.escribir(web.datos(ANA)).contains("Dificultad con herencia"), "Observación interna no se filtra al estudiante");
        verificar(Json.escribir(web.datos(ANA)).contains("Avance compartido"), "Notas compartidas visibles al alumno");
        var h=tutor("T002").getDisponibilidad().get(0);
        int diego=reservar(ANA,"T002","MM101",ServicioWeb.horarioId(h));
        verificar(Json.escribir(web.perfilEstudiante(DIEGO,"E001")).contains("Dificultad con herencia"), "Tutor relacionado lee comentarios previos");
        int pendiente=aceptar(DIEGO,diego);
        rechazar(() -> accion(DIEGO,"comentario-academico","sesion",""+pendiente,"contenido","Antes de impartir"), "Solo sesiones impartidas");
        finalizar(DIEGO,pendiente,"1.5");
        accion(DIEGO,"comentario-academico","sesion",""+pendiente,"contenido","Avance en funciones");
        var hp=tutor("P001").getDisponibilidad().get(0);
        int profesor=aceptar(COORD,reservar(ANA,"P001","CC5001",ServicioWeb.horarioId(hp)));
        finalizar(COORD,profesor,"1");
        accion(COORD,"comentario-academico","sesion",""+profesor,"contenido","Recomiendo repasar recursión");
        verificar(web.perfilEstudiante(MARIA,"E001").get("finalizadas").equals(8), "Total incluye otros tutores y profesor");
        verificar(Json.escribir(web.perfilEstudiante(MARIA,"E001")).contains("repasar recursión"), "Comentarios del profesor disponibles");
        db.inicializar(); web=new ServicioWeb(new RepositorioTutorias(db));
        verificar(ServicioWeb.estudiante(repo.cargar().sistema(),"E001").getComentarios().size()==3, "Comentarios persisten al reiniciar");
        verificar(web.perfilEstudiante(COORD,"E001").get("strikesConfirmadosSemestre")==null, "No inventar strikes antes del paso 8");
    }
    private static void probarMigracion() throws Exception {
        // Una copia antigua sin las nuevas columnas, incluyendo reservas históricas finalizadas.
        try (var c=db.conectar();var st=c.createStatement()) {
            st.execute("DROP TABLE comentarios_academicos");
            for (String columna : new String[]{"modalidad","ubicacion"}) st.execute("ALTER TABLE horarios DROP COLUMN "+columna);
            for (String columna : new String[]{"modalidad","ubicacion","multiplicador_beca"}) st.execute("ALTER TABLE solicitudes DROP COLUMN "+columna);
            st.execute("ALTER TABLE tutores DROP COLUMN multiplicador_beca");
            st.execute("ALTER TABLE tutores DROP COLUMN enlace_permanente");
            st.execute("ALTER TABLE sesiones DROP COLUMN duracion");
            st.execute("ALTER TABLE sesiones DROP COLUMN ausente");
        }
        db.inicializar(); db.inicializar();
        var s=repo.cargar().sistema();
        verificar(s.getSolicitudes().stream().allMatch(sol -> sol.getMultiplicadorBeca()==1 && sol.getModalidad()==null && sol.getUbicacion()==null), "Migración usa ×1 y no inventa condiciones");
        verificar(s.getTutores().stream().flatMap(t->t.getDisponibilidad().stream()).noneMatch(Horario::estaCompleto), "Horarios antiguos pendientes");
        verificar(new EstudianteController(s).consultarHorarios(ServicioWeb.tutor(s,"T001")).isEmpty(), "Bloques antiguos no se ofrecen");
        var libre=ServicioWeb.tutor(s,"T001").getDisponibilidad().stream().filter(Horario::isDisponible).findFirst().orElseThrow();
        String id=ServicioWeb.horarioId(libre);
        rechazar(() -> reservar(ANA,"T001","CC2006",id), "Reserva directa tampoco acepta bloque incompleto");
        accion(MARIA,"completar-horario","horario",id,"modalidad","PRESENCIAL","ubicacion","B-300");
        verificar(ServicioWeb.horario(tutor("T001"),id).estaCompleto(), "Tutor completa bloque antiguo libre");
        verificar(solicitud(1).getUbicacion()==null, "Completar horario no inventa ubicación histórica");
        repo.guardar(repo.cargar(),null);
        verificar(repo.cargar().sistema().getSesiones().stream().filter(SesionTutoria::isFinalizada).count()==8, "Migración y guardado conservan sesiones finalizadas");
    }
}
