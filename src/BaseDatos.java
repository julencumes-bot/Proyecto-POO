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
            st.executeUpdate("INSERT INTO app_version(id,revision) SELECT 1,0 WHERE NOT EXISTS (SELECT 1 FROM app_version WHERE id=1)");
        }
    }
    public String motor() { return url.startsWith("jdbc:postgresql:") ? "PostgreSQL" : "H2 persistente"; }
}
