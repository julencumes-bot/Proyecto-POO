import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Contraseñas con sal aleatoria y PBKDF2; nunca se almacenan en texto. */
public class Seguridad {
    public record Cuenta(String rol,String carnet,String salt,String hash) { }
    public record Identidad(String rol,String carnet) { }
    private static final SecureRandom RANDOM=new SecureRandom();
    private final BaseDatos db;
    public Seguridad(BaseDatos db) { this.db=db; }
    public static String token() { byte[] bytes=new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static String hash(String clave,String salt) {
        PBEKeySpec spec=new PBEKeySpec(clave.toCharArray(),Base64.getDecoder().decode(salt),210000,256);
        try { return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded()); }
        catch(Exception e) { throw new IllegalStateException("No se pudo proteger la contraseña.",e); }
        finally { spec.clearPassword(); }
    }
    public static Cuenta crear(String rol,String carnet,String clave) {
        if(clave==null || clave.length()<8 || clave.length()>128) { throw new IllegalArgumentException("La contraseña debe tener entre 8 y 128 caracteres."); }
        byte[] bytes=new byte[16]; RANDOM.nextBytes(bytes); String salt=Base64.getEncoder().encodeToString(bytes);
        return new Cuenta(rol,carnet,salt,hash(clave,salt));
    }
    static void insertar(Connection c,Cuenta cuenta) throws SQLException {
        RepositorioTutorias.ejecutar(c,"INSERT INTO cuentas VALUES(?,?,?,?)",cuenta.rol(),cuenta.carnet(),cuenta.salt(),cuenta.hash());
    }
    public void crearSiNoExiste(String rol,String carnet,String clave) throws SQLException {
        try(Connection c=db.conectar(); var p=c.prepareStatement("SELECT carnet FROM cuentas WHERE rol=? AND carnet=?")) {
            p.setString(1,rol); p.setString(2,carnet);
            try(var r=p.executeQuery()) { if(r.next()) { return; } }
            insertar(c,crear(rol,carnet,clave));
        }
    }
    public Identidad autenticar(String rol,String carnet,String clave) throws SQLException {
        if(clave==null || clave.length()>128) { return null; }
        try(Connection c=db.conectar();var p=c.prepareStatement("SELECT salt,hash FROM cuentas WHERE rol=? AND carnet=?")) {
            p.setString(1,rol);p.setString(2,carnet);
            try(var r=p.executeQuery()) {
                if(r.next() && MessageDigest.isEqual(hash(clave,r.getString("salt")).getBytes(java.nio.charset.StandardCharsets.UTF_8),r.getString("hash").getBytes(java.nio.charset.StandardCharsets.UTF_8))) { return new Identidad(rol,carnet); }
            }
        }
        return null;
    }
    public void cambiarClave(Identidad id,String actual,String nueva) throws SQLException {
        if(autenticar(id.rol(),id.carnet(),actual)==null) { throw new IllegalArgumentException("La contraseña actual no es correcta."); }
        Cuenta cuenta=crear(id.rol(),id.carnet(),nueva);
        try(Connection c=db.conectar()) { RepositorioTutorias.ejecutar(c,"UPDATE cuentas SET salt=?,hash=? WHERE rol=? AND carnet=?",cuenta.salt(),cuenta.hash(),id.rol(),id.carnet()); }
    }
}
