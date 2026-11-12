import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Configuración JDBC. H2 guarda un archivo; PostgreSQL se elige con DB_URL. */
public class BaseDatos {
    private final String url;
    private final String usuario;
    private final String clave;

    public BaseDatos(String url, String usuario, String clave) {
        this.url = url; this.usuario = usuario; this.clave = clave;
    }
    public static BaseDatos configurar() throws Exception {
        Files.createDirectories(Path.of("data"));
        return new BaseDatos(env("DB_URL", "jdbc:h2:./data/tutorias;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE"),
                env("DB_USER", "sa"), env("DB_PASSWORD", ""));
    }
    public static String env(String nombre, String defecto) {
        String valor = System.getenv(nombre);
        return valor == null ? defecto : valor;
    }
    public Connection conectar() throws SQLException {
        return DriverManager.getConnection(url, usuario, clave);
    }
    public void inicializar() throws Exception {
        String sql = Files.readString(Path.of("sql/schema.sql"), StandardCharsets.UTF_8);
        sql = sql.replaceAll("(?m)^--.*$", "");
        try (Connection c = conectar(); Statement st = c.createStatement()) {
            for (String sentencia : sql.split(";")) {
                if (!sentencia.isBlank()) { st.execute(sentencia); }
            }
            migrarInasistencias(c);
            st.executeUpdate("INSERT INTO app_version(id,revision) SELECT 1,0 WHERE NOT EXISTS (SELECT 1 FROM app_version WHERE id=1)");
        }
    }
    private static void migrarInasistencias(Connection c) throws SQLException {
        // Los nombres de CHECK de versiones antiguas los generó cada motor.
        // Se localiza solo la restricción de estado, sin tocar precios ni otras validaciones.
        java.util.List<String> antiguas = new java.util.ArrayList<>();
        String consulta = "SELECT tc.constraint_name, cc.check_clause FROM information_schema.table_constraints tc "
                + "JOIN information_schema.check_constraints cc ON cc.constraint_name=tc.constraint_name "
                + "AND cc.constraint_schema=tc.constraint_schema WHERE tc.table_name='solicitudes' "
                + "AND tc.table_schema=current_schema() AND tc.constraint_type='CHECK'";
        try (var st = c.createStatement(); var r = st.executeQuery(consulta)) {
            while (r.next()) {
                String regla = r.getString(2).toUpperCase(java.util.Locale.ROOT);
                if (regla.contains("ESTADO") && regla.contains("PENDIENTE") && !regla.contains("INASISTENCIA"))
                    antiguas.add(r.getString(1));
            }
        }
        try (var st = c.createStatement()) {
            for (String nombre : antiguas)
                st.execute("ALTER TABLE solicitudes DROP CONSTRAINT \"" + nombre.replace("\"", "\"\"") + "\"");
            if (!antiguas.isEmpty()) st.execute("ALTER TABLE solicitudes ADD CONSTRAINT solicitudes_estado_v78 "
                    + "CHECK(estado IN ('PENDIENTE','CONFIRMADA','CANCELADA','FINALIZADA','INASISTENCIA'))");
            st.executeUpdate("UPDATE solicitudes SET estado='INASISTENCIA' WHERE estado='CANCELADA' "
                    + "AND EXISTS (SELECT 1 FROM sesiones se WHERE se.solicitud=solicitudes.id AND se.ausente=TRUE)");
        }
    }
    public String motor() { return url.startsWith("jdbc:postgresql:") ? "PostgreSQL" : "H2 persistente"; }
}
