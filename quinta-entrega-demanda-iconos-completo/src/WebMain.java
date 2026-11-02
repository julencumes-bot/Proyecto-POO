import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/** Servidor local del proyecto. Las sesiones expiran y todos los cambios requieren CSRF. */
public class WebMain implements AutoCloseable {
    private record Sesion(Seguridad.Identidad identidad,String csrf,long vence) { }
    private record Intentos(int cantidad,long vence) { }
    private final Map<String,Sesion> sesiones=new ConcurrentHashMap<>();
    private final Map<String,Intentos> intentos=new ConcurrentHashMap<>();
    private final HttpServer servidor;
    private final java.util.concurrent.ExecutorService ejecutor=Executors.newFixedThreadPool(8);
    private final ServicioWeb servicio;
    private final Seguridad seguridad;
    private final boolean demo;
    public WebMain(BaseDatos db,int puerto,boolean demo) throws Exception {
        this.demo=demo;
        if(!demo && BaseDatos.env("ADMIN_PASSWORD","").length()<8) { throw new IllegalArgumentException("Define ADMIN_PASSWORD con al menos 8 caracteres al iniciar sin demostración."); }
        db.inicializar();seguridad=new Seguridad(db);servicio=new ServicioWeb(new RepositorioTutorias(db));servicio.inicializar(demo);
        if(demo) {
            for(String id:new String[]{"E001","E002"}) { seguridad.crearSiNoExiste("estudiante",id,"Tutor2026!"); }
            for(String id:new String[]{"T001","T002"}) { seguridad.crearSiNoExiste("tutor",id,"Tutor2026!"); }
            seguridad.crearSiNoExiste("coordinador","P001","Tutor2026!");
        } else { seguridad.crearSiNoExiste("coordinador",BaseDatos.env("ADMIN_ID","P001"),BaseDatos.env("ADMIN_PASSWORD","")); }
        servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",puerto),0);
        servidor.createContext("/",this::atender);servidor.setExecutor(ejecutor);
    }
    public int puerto() { return servidor.getAddress().getPort(); }
    public void iniciar() { servidor.start(); }
    @Override public void close() { servidor.stop(0);ejecutor.shutdownNow(); }
    public static void main(String[] args) throws Exception {
        BaseDatos db=BaseDatos.configurar();WebMain app=new WebMain(db,Integer.parseInt(BaseDatos.env("PORT","8080")),Boolean.parseBoolean(BaseDatos.env("DEMO_DATA","true")));
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));app.iniciar();
        System.out.println("Tutorías disponible en http://127.0.0.1:"+app.puerto()+" | "+db.motor());
    }
    private static Map<String,String> formulario(HttpExchange x) throws IOException {
        if(!x.getRequestHeaders().getFirst("Content-Type").startsWith("application/x-www-form-urlencoded")) { throw new IllegalArgumentException("Formato de formulario no admitido."); }
        byte[] bytes=x.getRequestBody().readNBytes(16385);
        if(bytes.length>16384) { throw new IllegalArgumentException("El formulario es demasiado grande."); }
        Map<String,String> result=new HashMap<>();
        for(String par:new String(bytes,StandardCharsets.UTF_8).split("&")) {
            String[] partes=par.split("=",2);String key=URLDecoder.decode(partes[0],StandardCharsets.UTF_8);
            if(result.put(key,URLDecoder.decode(partes.length==2 ? partes[1] : "",StandardCharsets.UTF_8))!=null) { throw new IllegalArgumentException("Campo repetido."); }
        }return result;
    }
    private String cookie(HttpExchange x) {
        String cab=x.getRequestHeaders().getFirst("Cookie");if(cab!=null) { for(String parte:cab.split(";")) { String[] kv=parte.trim().split("=",2);if(kv.length==2 && kv[0].equals("tutorias")) { return kv[1].replaceAll("^\"|\"$",""); } } }return "";
    }
    private Sesion nueva(HttpExchange x,Seguridad.Identidad id) {
        String token=Seguridad.token();Sesion s=new Sesion(id,Seguridad.token(),System.currentTimeMillis()+8*60*60*1000L);sesiones.put(token,s);
        x.getResponseHeaders().set("Set-Cookie","tutorias="+token+"; HttpOnly; SameSite=Strict; Path=/; Max-Age=28800");return s;
    }
    private void atender(HttpExchange x) throws IOException {
        try {
            var headers=x.getResponseHeaders();headers.set("X-Content-Type-Options","nosniff");headers.set("Referrer-Policy","same-origin");headers.set("X-Frame-Options","DENY");
            headers.set("Content-Security-Policy","default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'");
            String ruta=x.getRequestURI().getPath(),metodo=x.getRequestMethod();
            if(!ruta.startsWith("/api/")) {
                if(!metodo.equals("GET") && !metodo.equals("HEAD")) { responder(x,405,Map.of("error","Método no permitido."));return; }
                String archivo=switch(ruta) { case "/","/index.html" -> "index.html";case "/app.js" -> "app.js";case "/styles.css" -> "styles.css";default -> ""; };
                if(archivo.isEmpty()) { responder(x,404,Map.of("error","Página no encontrada."));return; }
                byte[] cuerpo=Files.readAllBytes(Path.of("web",archivo));headers.set("Content-Type",archivo.endsWith("css") ? "text/css; charset=utf-8" : archivo.endsWith("js") ? "text/javascript; charset=utf-8" : "text/html; charset=utf-8");
                if(metodo.equals("HEAD")) { x.sendResponseHeaders(200,-1); }else { x.sendResponseHeaders(200,cuerpo.length);x.getResponseBody().write(cuerpo); }return;
            }
            headers.set("Cache-Control","no-store");long ahora=System.currentTimeMillis();
            sesiones.entrySet().removeIf(e->e.getValue().vence()<ahora);intentos.entrySet().removeIf(e->e.getValue().vence()<ahora);
            Sesion s=sesiones.get(cookie(x));
            if(ruta.equals("/api/sesion") && metodo.equals("GET")) {
                if(s==null) { s=nueva(x,null); }
                responder(x,200,ServicioWeb.obj("csrf",s.csrf(),"autenticado",s.identidad()!=null,"demo",demo));return;
            }
            if(!metodo.equals("GET") && !metodo.equals("POST")) { responder(x,405,Map.of("error","Método no permitido."));return; }
            Map<String,String> f=Map.of();
            if(metodo.equals("POST")) {
                String tipo=x.getRequestHeaders().getFirst("Content-Type");
                if(tipo==null) { throw new IllegalArgumentException("Falta el tipo de formulario."); }
                f=formulario(x);
                if(s==null || !s.csrf().equals(f.get("csrf"))) { responder(x,403,Map.of("error","La sesión del formulario venció. Recarga la página."));return; }
            }
            if(ruta.equals("/api/login") && metodo.equals("POST")) {
                String ip=x.getRemoteAddress().getAddress().getHostAddress();Intentos it=intentos.getOrDefault(ip,new Intentos(0,ahora+60000));
                if(it.cantidad()>=20) { responder(x,429,Map.of("error","Demasiados intentos. Espera un minuto."));return; }
                intentos.put(ip,new Intentos(it.cantidad()+1,it.vence()));
                var id=seguridad.autenticar(f.get("rol"),f.getOrDefault("carnet","").trim(),f.get("clave"));
                if(id==null) { responder(x,401,Map.of("error","Revisa el perfil, el carné y la contraseña."));return; }
                intentos.remove(ip);sesiones.remove(cookie(x));s=nueva(x,id);responder(x,200,Map.of("csrf",s.csrf()));return;
            }
            if(ruta.equals("/api/registro-estudiante")) {
                if(!metodo.equals("POST")) { responder(x,405,Map.of("error","Método no permitido."));return; }
                if(s.identidad()!=null) { responder(x,403,Map.of("error","Cierra sesión para registrar una cuenta de estudiante."));return; }
                servicio.registrarEstudiante(f);responder(x,201,Map.of("ok",true));return;
            }
            if(s==null || s.identidad()==null) { responder(x,401,Map.of("error","Inicia sesión para continuar."));return; }
            if(ruta.equals("/api/logout") && metodo.equals("POST")) { sesiones.remove(cookie(x));s=nueva(x,null);responder(x,200,Map.of("csrf",s.csrf()));return; }
            if(ruta.equals("/api/estado") && metodo.equals("GET")) { responder(x,200,servicio.datos(s.identidad()));return; }
            if(ruta.equals("/api/clave") && metodo.equals("POST")) {
                seguridad.cambiarClave(s.identidad(),f.get("actual"),f.get("nueva"));var id=s.identidad();sesiones.entrySet().removeIf(e->id.equals(e.getValue().identidad()));s=nueva(x,id);responder(x,200,Map.of("csrf",s.csrf(),"ok",true));return;
            }
            if(ruta.startsWith("/api/accion/") && metodo.equals("POST")) { servicio.ejecutar(s.identidad(),ruta.substring("/api/accion/".length()),f);responder(x,200,Map.of("ok",true));return; }
            responder(x,404,Map.of("error","Ruta no encontrada."));
        } catch(SecurityException e) { responder(x,403,Map.of("error",e.getMessage())); }
        catch(IllegalArgumentException|IllegalStateException e) { responder(x,400,Map.of("error",e instanceof NumberFormatException ? "Revisa los campos numéricos." : e.getMessage())); }
        catch(SQLException e) {
            System.err.println("Error de base de datos: "+e.getSQLState());
            responder(x,"40001".equals(e.getSQLState()) ? 409 : 503,Map.of("error","No se guardó el cambio. Revisa la conexión y actualiza la página antes de intentarlo de nuevo."));
        } catch(Exception e) { e.printStackTrace();responder(x,500,Map.of("error","No pudimos completar la operación. Inténtalo de nuevo.")); }
        finally { x.close(); }
    }
    private static void responder(HttpExchange x,int codigo,Object datos) throws IOException {
        byte[] cuerpo=Json.escribir(datos).getBytes(StandardCharsets.UTF_8);x.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");x.sendResponseHeaders(codigo,cuerpo.length);x.getResponseBody().write(cuerpo);
    }
}
