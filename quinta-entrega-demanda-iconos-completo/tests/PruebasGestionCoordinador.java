import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Cambios de Caleb: gestión, compensación histórica y actualización de bases existentes. */
public class PruebasGestionCoordinador {
    private static int comprobaciones;
    private static final Seguridad.Identidad COORD = new Seguridad.Identidad("coordinador", "P001");
    private static final Seguridad.Identidad ANA = new Seguridad.Identidad("estudiante", "E001");
    private static final Seguridad.Identidad MARIA = new Seguridad.Identidad("tutor", "T001");

    private static void verificar(boolean valor, String mensaje) {
        if (!valor) { throw new AssertionError(mensaje); }
        comprobaciones++;
    }
    private static void rechazar(Runnable operacion, String mensaje) {
        boolean rechazado = false;
        try { operacion.run(); } catch (IllegalArgumentException | IllegalStateException e) { rechazado = true; }
        verificar(rechazado, mensaje);
    }
    private static BaseDatos nuevaBase() throws Exception {
        var carpeta = Files.createTempDirectory("tutorias-gestion-caleb-");
        BaseDatos db = new BaseDatos("jdbc:h2:" + carpeta.resolve("db") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        db.inicializar();
        new ServicioWeb(new RepositorioTutorias(db)).inicializar(true);
        return db;
    }
    private static Map<String,String> datos(String id, boolean beca, String tarifa) {
        Map<String,String> datos = new LinkedHashMap<>(Map.of("tutor", id, "nombre", "Nombre actualizado",
                "correo", "actualizado@example.com", "anio", "4", "tarifa", tarifa));
        if (beca) { datos.put("beca", "on"); }
        return datos;
    }
    private static void reservar(ServicioWeb servicio, RepositorioTutorias repo, String tutor, String curso, int bloque) throws Exception {
        var t = ServicioWeb.tutor(repo.cargar().sistema(), tutor);
        servicio.ejecutar(ANA, "solicitar", Map.of("tutor", tutor, "curso", curso,
                "horario", ServicioWeb.horarioId(t.getDisponibilidad().get(bloque)), "tema", "Tema de prueba"));
    }
    private static void finalizar(ServicioWeb servicio, Seguridad.Identidad tutor, String solicitud, String sesion) throws Exception {
        servicio.ejecutar(tutor, "aceptar", Map.of("solicitud", solicitud));
        servicio.ejecutar(tutor, "finalizar", Map.of("sesion", sesion, "temas", "POO", "notas", "Completada"));
    }

    private static void probarModelo() {
        var s = Main.crearDemostracion();
        var cc = new CoordinadorController(s);
        var ec = new EstudianteController(s);
        Tutor t = s.getTutores().get(1);
        Curso curso = s.getCursos().get(0);
        var ana = s.getEstudiantes().get(0);
        var solicitud = ec.solicitarTutoria(ana, curso, t, "Tema", t.getDisponibilidad().get(0));
        rechazar(() -> cc.eliminarCurso(curso), "No eliminar curso con solicitud pendiente");
        rechazar(() -> cc.eliminarTutor(t), "No quitar tutor con solicitud pendiente");
        var tc = new TutorController(s);
        var sesion = tc.aceptarTutoria(t, solicitud);
        rechazar(() -> cc.eliminarCurso(curso), "No eliminar curso con solicitud confirmada");
        rechazar(() -> cc.eliminarTutor(t), "No quitar tutor con solicitud confirmada");
        String anterior = t.getNombre();
        rechazar(() -> cc.modificarTutor(t, "No guardar", "x@example.com", 2, true, 50), "Beca exige tarifa cero");
        verificar(t.getNombre().equals(anterior) && t.getTarifaHora() == 0, "Edición inválida es atómica");
        cc.modificarTutor(t, "Actualizado", "nuevo@example.com", 4, false, 75);
        tc.finalizarSesion(t, sesion, "Clases", "Terminada", "");
        verificar(t.getRegistroHorasBeca().getHorasAcumuladas() == 1, "Reserva de beca mantiene sus horas al cambiar a pago");
        verificar(t.getRemuneracionAcumulada() == 0, "No convertir la beca anterior en pago");
        cc.eliminarCurso(curso);
        verificar(!curso.isActivo() && !cc.consultarCursos().contains(curso), "Curso fuera de la oferta");
        verificar(ec.consultarTutores(curso).isEmpty(), "Curso retirado sin tutores disponibles");
        rechazar(() -> ec.solicitarTutoria(ana, curso, t, "Otro", t.getDisponibilidad().get(1)), "Bloquear reserva directa de curso retirado");
        rechazar(() -> cc.aprobarTutor(t, curso), "No aprobar curso retirado");
        verificar(cc.consultarHistorial().size() == 1, "Retirar curso conserva su historial");
        cc.eliminarTutor(t);
        verificar(!t.isActivo() && !cc.consultarTutores().contains(t), "Tutor fuera de la oferta");
        verificar(ec.consultarHorarios(t).isEmpty(), "Tutor retirado sin horarios disponibles");
        rechazar(() -> tc.agregarHorario(t, "2027-01-01", "09:00", "10:00"), "Tutor retirado no publica horarios");
        rechazar(() -> cc.modificarTutor(t, "Otro", "otro@example.com", 2, false, 10), "No modificar perfil retirado");
        verificar(t.getHistorialTutorias().size() == 1, "Retirar tutor conserva su historial");
    }

    private static void probarCompensacionYPersistencia() throws Exception {
        BaseDatos db = nuevaBase();
        var repo = new RepositorioTutorias(db);
        var servicio = new ServicioWeb(repo);
        reservar(servicio, repo, "T001", "CC2006", 0); // Originalmente beca.
        servicio.ejecutar(COORD, "editar-tutor", datos("T001", false, "60"));
        var original = ServicioWeb.solicitud(repo.cargar().sistema(), "1");
        verificar(original.isRecibeHorasBeca() && original.getPrecio() == 0, "Reserva conserva beca y precio después de recargar");
        reservar(servicio, repo, "T001", "CC2006", 1); // Ahora remunerada.
        var pagada = ServicioWeb.solicitud(repo.cargar().sistema(), "2");
        verificar(!pagada.isRecibeHorasBeca() && pagada.getPrecio() == 60, "Nueva reserva usa tarifa actual");
        finalizar(servicio, MARIA, "1", "1");
        servicio.ejecutar(COORD, "editar-tutor", datos("T001", true, "0"));
        finalizar(servicio, MARIA, "2", "2");
        verificar(ServicioWeb.tutor(repo.cargar().sistema(), "T001").getRemuneracionAcumulada() == 60, "Pago pendiente sigue siendo pago al pasar a beca");
        verificar(ServicioWeb.tutor(repo.cargar().sistema(), "T001").getRegistroHorasBeca().getHorasAcumuladas() == 1, "No convertir pago previo en horas beca");
        servicio.ejecutar(COORD, "editar-tutor", datos("T001", false, "85"));
        servicio.ejecutar(COORD, "eliminar-curso", Map.of("curso", "CC2006"));
        servicio.ejecutar(COORD, "eliminar-tutor", Map.of("tutor", "T001"));
        db.inicializar(); // Reiniciar/migrar es idempotente.
        var restaurado = new RepositorioTutorias(db).cargar().sistema();
        Tutor t = ServicioWeb.tutor(restaurado, "T001");
        verificar(!t.isActivo() && !ServicioWeb.curso(restaurado, "CC2006").isActivo(), "Retiros persisten al reiniciar");
        verificar(t.getNombre().equals("Nombre actualizado") && t.getTarifaHora() == 85, "Datos y tarifa actual persisten");
        verificar(t.getRemuneracionAcumulada() == 60 && t.getRegistroHorasBeca().getHorasAcumuladas() == 1, "Ambos acumulados permanecen tras reiniciar y retirar");
        verificar(restaurado.getSesiones().size() == 2 && restaurado.getSolicitudes().size() == 2, "Retiro no elimina solicitudes ni sesiones");
        var oferta = Json.escribir(servicio.datos(ANA));
        verificar(!oferta.contains("\"codigo\":\"CC2006\"") && !oferta.contains("\"carnet\":\"T001\""), "Oferta del estudiante excluye curso y tutor retirados");
        verificar(oferta.contains("\"nombreCurso\":\"Programación Orientada a Objetos\""), "Estudiante conserva el historial del curso retirado");

        servicio.ejecutar(COORD, "editar-tutor", datos("P001", false, "25"));
        var p = repo.cargar().sistema().getCoordinador();
        verificar(p.getNombre().equals("Nombre actualizado") && p.getPerfilTutor().getNombre().equals(p.getNombre()), "Nombre del profesor sincronizado con coordinación");
        verificar(p.getCorreo().equals("actualizado@example.com") && p.getPerfilTutor().getTarifaHora() == 25, "Profesor conserva correo y tarifa al recargar");
        reservar(servicio, repo, "P001", "CC5001", 0);
        servicio.ejecutar(COORD, "editar-tutor", datos("P001", true, "0"));
        finalizar(servicio, COORD, "3", "3");
        verificar(repo.cargar().sistema().getCoordinador().getPerfilTutor().getRemuneracionAcumulada() == 25, "Profesor conserva la remuneración pactada");
        servicio.ejecutar(COORD, "eliminar-tutor", Map.of("tutor", "P001"));
        verificar(!repo.cargar().sistema().getCoordinador().getPerfilTutor().isActivo(), "Retirar profesor persiste");
        verificar(Json.escribir(servicio.datos(COORD)).contains("\"rol\":\"coordinador\""), "Retirar perfil docente conserva administración");
        servicio.ejecutar(COORD, "curso", Map.of("codigo", "NUEVO", "nombre", "Curso nuevo", "icono", "book"));
        verificar(ServicioWeb.curso(repo.cargar().sistema(), "NUEVO").isActivo(), "Profesor retirado sigue administrando cursos");
    }

    private static void probarMigracion() throws Exception {
        BaseDatos db = nuevaBase();
        var repo = new RepositorioTutorias(db);
        var servicio = new ServicioWeb(repo);
        reservar(servicio, repo, "T001", "CC2006", 0);
        reservar(servicio, repo, "T002", "MM101", 0);
        try (var c = db.conectar(); var st = c.createStatement()) {
            st.execute("ALTER TABLE solicitudes DROP COLUMN beca");
            st.execute("ALTER TABLE tutores DROP COLUMN activo");
            st.execute("ALTER TABLE cursos DROP COLUMN activo");
        }
        db.inicializar();
        db.inicializar();
        var s = repo.cargar().sistema();
        verificar(ServicioWeb.solicitud(s, "1").isRecibeHorasBeca(), "Base anterior recupera reserva de beca");
        verificar(!ServicioWeb.solicitud(s, "2").isRecibeHorasBeca() && ServicioWeb.solicitud(s, "2").getPrecio() == 60, "Base anterior recupera reserva remunerada");
        verificar(s.getCursos().stream().allMatch(Curso::isActivo) && s.getTutores().stream().allMatch(Tutor::isActivo), "Datos anteriores permanecen activos");
        verificar(s.getSolicitudes().size() == 2, "Migración conserva las reservas existentes");
    }

    private static class Cliente {
        private final String base;
        private final HttpClient http = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        private String csrf;
        Cliente(int puerto) throws Exception {
            base = "http://127.0.0.1:" + puerto;
            csrf = campo(get("/api/sesion").body(), "csrf");
        }
        HttpResponse<String> get(String ruta) throws Exception {
            return http.send(HttpRequest.newBuilder(URI.create(base + ruta)).GET().build(), HttpResponse.BodyHandlers.ofString());
        }
        HttpResponse<String> post(String ruta, Map<String,String> datos) throws Exception {
            Map<String,String> f = new LinkedHashMap<>(datos);
            f.putIfAbsent("csrf", csrf);
            StringBuilder cuerpo = new StringBuilder();
            for (var e : f.entrySet()) {
                if (!cuerpo.isEmpty()) { cuerpo.append('&'); }
                cuerpo.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8)).append('=')
                        .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
            return http.send(HttpRequest.newBuilder(URI.create(base + ruta)).header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString())).build(), HttpResponse.BodyHandlers.ofString());
        }
        int login(String rol, String id) throws Exception {
            var r = post("/api/login", Map.of("rol", rol, "carnet", id, "clave", "Tutor2026!"));
            if (r.statusCode() == 200) { csrf = campo(r.body(), "csrf"); }
            return r.statusCode();
        }
        void accion(String accion, Map<String,String> datos, int esperado) throws Exception {
            var r = post("/api/accion/" + accion, datos);
            verificar(r.statusCode() == esperado, accion + " esperaba " + esperado + ", recibió " + r.statusCode() + ": " + r.body());
        }
    }
    private static String campo(String json, String campo) {
        var m = Pattern.compile("\"" + campo + "\":\"([^\"]+)\"").matcher(json);
        if (!m.find()) { throw new AssertionError("Falta " + campo); }
        return m.group(1);
    }
    private static void probarHttp() throws Exception {
        BaseDatos db = nuevaBase();
        try (var servidor = new WebMain(db, 0, true)) {
            servidor.iniciar();
            var coordinador = new Cliente(servidor.puerto());
            var estudiante = new Cliente(servidor.puerto());
            var tutor = new Cliente(servidor.puerto());
            verificar(coordinador.login("coordinador", "P001") == 200, "Acceso coordinador");
            verificar(estudiante.login("estudiante", "E001") == 200, "Acceso estudiante");
            verificar(tutor.login("tutor", "T001") == 200, "Acceso tutor antes de retirarlo");
            for (String accion : new String[]{"editar-tutor", "eliminar-tutor", "eliminar-curso"}) {
                estudiante.accion(accion, Map.of(), 403);
                tutor.accion(accion, Map.of(), 403);
            }
            var antes = new RepositorioTutorias(db).cargar().revision();
            var invalido = datos("T001", true, "30");
            coordinador.accion("editar-tutor", invalido, 400);
            invalido = datos("T001", false, "NaN");
            coordinador.accion("editar-tutor", invalido, 400);
            invalido = datos("T001", false, "40");
            invalido.put("correo", "no-es-correo");
            coordinador.accion("editar-tutor", invalido, 400);
            invalido = datos("T001", false, "40");
            invalido.put("anio", "11");
            coordinador.accion("editar-tutor", invalido, 400);
            invalido = datos("T001", false, "40");
            invalido.put("csrf", "incorrecto");
            coordinador.accion("editar-tutor", invalido, 403);
            verificar(new RepositorioTutorias(db).cargar().revision() == antes, "Errores y permisos no modifican la base");
            coordinador.accion("editar-tutor", datos("T001", false, "45.50"), 200);
            verificar(tutor.get("/api/estado").body().contains("\"tarifa\":45.5"), "Tutor ve los datos editados por coordinación");
            coordinador.accion("eliminar-curso", Map.of("curso", "CC2006"), 200);
            coordinador.accion("eliminar-tutor", Map.of("tutor", "T001"), 200);
            verificar(tutor.get("/api/estado").statusCode() == 403, "Sesión anterior del tutor retirado no accede a datos");
            tutor.accion("horario", Map.of("fecha", "2027-01-01", "inicio", "08:00", "fin", "09:00"), 403);
            var nuevo = new Cliente(servidor.puerto());
            verificar(nuevo.login("tutor", "T001") == 401, "Tutor retirado no inicia una nueva sesión");
            coordinador.accion("eliminar-tutor", Map.of("tutor", "P001"), 200);
            verificar(coordinador.get("/api/estado").statusCode() == 200, "Coordinador conserva sesión al retirar perfil docente");
            verificar(nuevo.login("coordinador", "P001") == 200, "Coordinador conserva acceso después del retiro docente");
            coordinador.accion("horario", Map.of("fecha", "2027-01-01", "inicio", "08:00", "fin", "09:00"), 400);
        }
    }
    public static void main(String[] args) throws Exception {
        probarModelo();
        probarCompensacionYPersistencia();
        probarMigracion();
        probarHttp();
        System.out.println("CORRECTO: " + comprobaciones + " comprobaciones de gestión del coordinador superadas.");
    }
}
