import java.nio.file.Files;
import java.util.List;
import java.util.Map;

/** Regresión de demanda, iconos y actualización de una base anterior. */
public class PruebasCoordinacion {
    private static int comprobaciones;
    private static void comprobar(boolean valor, String mensaje) {
        if (!valor) { throw new AssertionError(mensaje); }
        comprobaciones++;
    }
    private static void probarDemanda() {
        SistemaTutorias sistema = new SistemaTutorias(new ProfesorCoordinador("Coordinación", "P1", "p@example.com"));
        CoordinadorController cc = new CoordinadorController(sistema);
        comprobar(cc.consultarDemandaPorCurso().isEmpty(), "Sin cursos la demanda está vacía");
        Curso primero = new Curso("M1", "Matemáticas"), segundo = new Curso("M2", "Matemáticas");
        sistema.agregarCurso(primero); sistema.agregarCurso(segundo);
        Estudiante alumno = new Estudiante("Ana", "E1", "ana@example.com", 2);
        Tutor tutor = new Tutor("María", "T1", "maria@example.com", 3, true, 0);
        String[] temas = {"Integrales", "INTEGRALES", "Matrices", "Solo cancelada"};
        for (int i = 0; i < temas.length; i++) {
            SolicitudTutoria solicitud = new SolicitudTutoria(i + 1, alumno, tutor, primero, temas[i],
                    new Horario("2026-12-0" + (i + 1), "10:00", "11:00"));
            if (i == 1 || i == 2) { solicitud.confirmar(); }
            if (i == 2) { solicitud.finalizar(); }
            if (i == 3) { solicitud.cancelar(); }
            sistema.agregarSolicitud(solicitud);
        }
        var filas = cc.consultarDemandaPorCurso();
        comprobar(filas.size() == 2, "Se incluyen los cursos sin solicitudes");
        comprobar(filas.get(0).cantidad() == 3, "Pendientes, confirmadas y finalizadas suman; canceladas no");
        comprobar(filas.get(0).temas().get("integrales") == 2, "Temas iguales se agrupan sin distinguir mayúsculas");
        comprobar(filas.get(0).temas().get("matrices") == 1, "Se cuenta cada tema");
        comprobar(!filas.get(0).temas().containsKey("solo cancelada"), "El tema cancelado queda excluido");
        comprobar(filas.get(1).cantidad() == 0 && filas.get(1).temas().isEmpty(), "Cursos con el mismo nombre no se mezclan");
        comprobar(filas.get(0).temas().values().stream().mapToInt(Integer::intValue).sum() == filas.get(0).cantidad(), "El desglose coincide con el total");
        comprobar(cc.consultarDemanda().contains("integrales: 2"), "La consola conserva el informe de texto");
    }
    private static void probarIconos() {
        for (String icono : List.of("book", "code", "math", "nodes", "atom", "flask", "leaf", "globe", "landmark", "palette", "music", "money")) {
            comprobar(new Curso("C1", "Clase", icono).getIcono().equals(icono), "Icono admitido: " + icono);
        }
        comprobar(new Curso("C1", "Matemáticas", "palette").getIcono().equals("palette"), "La selección prevalece sobre el nombre");
        comprobar(new Curso("C1", "Programación").getIcono().equals("code"), "Compatibilidad con constructor anterior");
        for (String icono : List.of("", "desconocido", "<svg onload=alert(1)>", "x".repeat(100))) {
            boolean rechazado = false;
            try { new Curso("C1", "Clase", icono); } catch (IllegalArgumentException e) { rechazado = true; }
            comprobar(rechazado, "Se rechaza un icono fuera del catálogo");
        }
    }
    private static void probarMigracion() throws Exception {
        var carpeta = Files.createTempDirectory("tutorias-migracion-iconos-");
        BaseDatos db = new BaseDatos("jdbc:h2:" + carpeta.resolve("db") + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE", "sa", "");
        db.inicializar();
        RepositorioTutorias repo = new RepositorioTutorias(db);
        ServicioWeb servicio = new ServicioWeb(repo);
        servicio.inicializar(true);
        var horario = ServicioWeb.tutor(repo.cargar().sistema(), "T001").getDisponibilidad().get(0);
        servicio.ejecutar(new Seguridad.Identidad("estudiante", "E001"), "solicitar",
                Map.of("curso", "CC2006", "tutor", "T001", "horario", ServicioWeb.horarioId(horario), "tema", "Tema anterior"));
        // Reproduce el esquema entregado originalmente, con datos y sin columna icono.
        try (var c = db.conectar(); var st = c.createStatement()) { st.execute("ALTER TABLE cursos DROP COLUMN icono"); }
        db.inicializar(); db.inicializar();
        var s = repo.cargar().sistema();
        comprobar(s.getCursos().size() == 3 && s.getEstudiantes().size() == 2, "Migración repetible conserva cursos y estudiantes");
        comprobar(s.getSolicitudes().size() == 1 && s.getSolicitudes().get(0).getTema().equals("Tema anterior"), "Migración conserva solicitudes");
        comprobar(ServicioWeb.curso(s, "CC2006").getIcono().equals("code"), "Curso anterior de programación conserva su icono");
        comprobar(ServicioWeb.curso(s, "MM101").getIcono().equals("math"), "Curso anterior de matemáticas conserva su icono");
        comprobar(ServicioWeb.curso(s, "CC5001").getIcono().equals("nodes"), "Curso anterior de algoritmos conserva su icono");
        var coordinador = new Seguridad.Identidad("coordinador", "P001");
        servicio.ejecutar(coordinador, "curso", Map.of("codigo", "BIO1", "nombre", "Biología", "icono", "leaf"));
        servicio.ejecutar(coordinador, "curso", Map.of("codigo", "OLD1", "nombre", "Cálculo"));
        db.inicializar();
        var reinicio = new ServicioWeb(new RepositorioTutorias(db));
        reinicio.inicializar(true);
        s = repo.cargar().sistema();
        comprobar(ServicioWeb.curso(s, "BIO1").getIcono().equals("leaf"), "El icono seleccionado persiste al reconstruir servicio y repositorio");
        comprobar(ServicioWeb.curso(s, "OLD1").getIcono().equals("math"), "Clientes anteriores pueden omitir el icono");
        comprobar(s.getCursos().size() == 5 && s.getSolicitudes().size() == 1, "Guardar después de migrar conserva los registros");
        var demanda = (List<?>) reinicio.datos(coordinador).get("demanda");
        comprobar(demanda.size() == 5 && demanda.get(0) instanceof Map<?, ?>, "La API entrega filas estructuradas");
        var fila = (Map<?, ?>) demanda.stream().filter(d -> ((Map<?, ?>) d).get("codigo").equals("CC2006")).findFirst().orElseThrow();
        comprobar(fila.get("cantidad").equals(1) && ((List<?>) fila.get("temas")).size() == 1, "La API mantiene conteos y temas");
        comprobar(((List<?>) reinicio.datos(new Seguridad.Identidad("estudiante", "E001")).get("demanda")).isEmpty(), "El estudiante no recibe la demanda global");
        comprobar(((List<?>) reinicio.datos(new Seguridad.Identidad("tutor", "T001")).get("demanda")).isEmpty(), "El tutor no recibe la demanda global");
    }
    public static void main(String[] args) throws Exception {
        probarDemanda(); probarIconos(); probarMigracion();
        System.out.println("CORRECTO: " + comprobaciones + " comprobaciones de demanda, iconos y migración superadas.");
    }
}
