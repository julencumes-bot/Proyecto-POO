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
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/** Verificación HTTP de permisos, validación y reinicio de los cambios 7 y 8. */
public class PruebasHttp78 {
    private static int comprobaciones;
    private static void comprobar(boolean valor,String mensaje) {
        if(!valor)throw new AssertionError(mensaje);comprobaciones++;
    }
    private static class Cliente {
        private final String base;
        private final HttpClient http=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        private String csrf;
        Cliente(int puerto) throws Exception { base="http://127.0.0.1:"+puerto;csrf=campo(get("/api/sesion").body(),"csrf"); }
        HttpResponse<String> get(String ruta) throws Exception { return http.send(HttpRequest.newBuilder(URI.create(base+ruta)).GET().build(),HttpResponse.BodyHandlers.ofString()); }
        HttpResponse<String> post(String ruta,Map<String,String> valores) throws Exception {
            Map<String,String> v=new LinkedHashMap<>(valores);v.putIfAbsent("csrf",csrf);
            StringBuilder cuerpo=new StringBuilder();
            for(var e:v.entrySet()) { if(!cuerpo.isEmpty())cuerpo.append('&');cuerpo.append(URLEncoder.encode(e.getKey(),StandardCharsets.UTF_8)).append('=').append(URLEncoder.encode(e.getValue(),StandardCharsets.UTF_8)); }
            return http.send(HttpRequest.newBuilder(URI.create(base+ruta)).header("Content-Type","application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(cuerpo.toString())).build(),HttpResponse.BodyHandlers.ofString());
        }
        void login(String rol,String id) throws Exception {
            var r=post("/api/login",Map.of("rol",rol,"carnet",id,"clave","Tutor2026!"));comprobar(r.statusCode()==200,"Login "+id+": "+r.body());csrf=campo(r.body(),"csrf");
        }
        void accion(String accion,int codigo,Map<String,String> datos) throws Exception {
            var r=post("/api/accion/"+accion,datos);comprobar(r.statusCode()==codigo,accion+" esperaba "+codigo+" recibió "+r.statusCode()+": "+r.body());
        }
    }
    private static String campo(String json,String campo) {
        var m=Pattern.compile("\""+campo+"\":\"([^\"]+)\"").matcher(json);if(!m.find())throw new AssertionError("Falta "+campo);return m.group(1);
    }

    public static void main(String[] args) throws Exception {
        var carpeta=Files.createTempDirectory("http-78-");
        BaseDatos db=new BaseDatos("jdbc:h2:"+carpeta.resolve("db")+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE","sa","");
        String hoy=java.time.LocalDate.now(Seguimiento.ZONA).toString();
        String pasada=java.time.LocalDate.parse(hoy).minusDays(2).toString();
        try(WebMain app=new WebMain(db,0,true)) {
            app.iniciar();
            Cliente ana=new Cliente(app.puerto()),maria=new Cliente(app.puerto()),coord=new Cliente(app.puerto()),luis=new Cliente(app.puerto());
            ana.login("estudiante","E001");maria.login("tutor","T001");coord.login("coordinador","P001");luis.login("estudiante","E002");
            coord.accion("periodo-academico",400,Map.of("periodo","X"));
            coord.accion("periodo-academico",400,Map.of("periodo","X","inicio","2026-99-01","fin",hoy));
            coord.accion("politica-sanciones",400,Map.of("acuerdo","on"));
            coord.accion("resolver-ausencia",400,Map.of("reporte","1"));
            var periodo=Map.of("periodo","TEST-A","inicio",java.time.LocalDate.parse(hoy).minusDays(30).toString(),"fin",java.time.LocalDate.parse(hoy).plusDays(30).toString());
            ana.accion("periodo-academico",403,periodo);coord.accion("periodo-academico",200,periodo);
            coord.accion("periodo-academico",200,Map.of("periodo","TEST-B","inicio",java.time.LocalDate.parse(hoy).plusDays(31).toString(),"fin",java.time.LocalDate.parse(hoy).plusDays(150).toString()));
            coord.accion("politica-sanciones",200,Map.of("estudiante","SIGUIENTE_PERIODO","tutor","DESDE_CONFIRMACION","acuerdo","on"));
            maria.accion("horario",200,Map.of("fecha",pasada,"inicio","08:00","fin","09:00","modalidad","PRESENCIAL","ubicacion","Aula de prueba"));
            ana.accion("solicitar",200,Map.of("curso","CC2006","tutor","T001","horario",pasada+"|08:00|09:00","tema","Herencia"));
            maria.accion("aceptar",200,Map.of("solicitud","1"));
            maria.accion("finalizar",200,Map.of("sesion","1","temas","Herencia","notas","Avanza","duracion","1"));
            luis.accion("valorar",403,Map.of("sesion","1","puntuacion","5"));
            ana.accion("valorar",400,Map.of("sesion","1","puntuacion","6"));
            ana.accion("valorar",403,Map.of("sesion","1","puntuacion","5","csrf","incorrecto"));
            ana.accion("valorar",200,Map.of("sesion","1","puntuacion","4","comentario","Bien"));
            ana.accion("valorar",400,Map.of("sesion","1","puntuacion","5"));
            maria.accion("evaluacion-academica",403,Map.of("tutor","T001","curso","CC2006","dominio","5","claridad","5","fundamento","Propio"));
            coord.accion("evaluacion-academica",200,Map.of("tutor","T001","curso","CC2006","dominio","5","claridad","3","fundamento","Evaluación independiente"));
            String ayer=java.time.LocalDate.parse(hoy).minusDays(1).toString();
            maria.accion("horario",200,Map.of("fecha",ayer,"inicio","08:00","fin","09:00","modalidad","PRESENCIAL","ubicacion","A"));
            ana.accion("solicitar",200,Map.of("curso","CC2006","tutor","T001","horario",ayer+"|08:00|09:00","tema","Ausencias"));
            maria.accion("aceptar",200,Map.of("solicitud","2"));
            luis.accion("reportar-ausencia",403,Map.of("solicitud","2","motivo","No participo"));
            ana.accion("reportar-ausencia",200,Map.of("solicitud","2","motivo","Tutor no llegó","evidencia","EVIDENCIA_PRIVADA_78"));
            comprobar(!luis.get("/api/estado").body().contains("EVIDENCIA_PRIVADA_78"),"HTTP no expone expediente a terceros");
            maria.accion("explicar-ausencia",200,Map.of("reporte","1","contenido","Hubo un imprevisto"));
            ana.accion("resolver-ausencia",403,Map.of("reporte","1","decision","CONFIRMADO","motivo","No soy coordinación"));
            coord.accion("resolver-ausencia",200,Map.of("reporte","1","decision","CONFIRMADO","motivo","Verificado"));
            coord.accion("resolver-ausencia",400,Map.of("reporte","1","decision","CONFIRMADO","motivo","Repetido"));
            comprobar(maria.get("/api/estado").statusCode()==200,"HTTP conserva acceso del suspendido");
            comprobar(maria.get("/api/estado").body().contains("INASISTENCIA"),"HTTP distingue cierre");
            ana.accion("solicitar",400,Map.of("curso","CC2006","tutor","T001","horario",pasada+"|08:00|09:00","tema","Bloqueado"));
            coord.accion("corregir-ausencia",400,Map.of("reporte","1","decision","DESCARTADO","motivo"," "));
            coord.accion("corregir-ausencia",200,Map.of("reporte","1","decision","DESCARTADO","motivo","Justificación verificada"));
        }
        try(WebMain app=new WebMain(db,0,true)) {
            app.iniciar();Cliente c=new Cliente(app.puerto());c.login("coordinador","P001");
            String datos=c.get("/api/estado").body();
            comprobar(datos.contains("Evaluación independiente")&&datos.contains("Justificación verificada"),"Reinicio HTTP conserva evaluaciones y correcciones");
            comprobar(datos.contains("\"promedioValoraciones\":4.0"),"Reinicio HTTP conserva promedio");
        }
        System.out.println("CORRECTO: "+comprobaciones+" comprobaciones HTTP de valoraciones y sanciones superadas.");
    }
}
