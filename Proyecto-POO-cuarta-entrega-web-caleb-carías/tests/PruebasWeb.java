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

/** Pruebas reales por HTTP sobre una base SQL temporal. No toca data/tutorias. */
public class PruebasWeb {
    private static int comprobaciones;
    private static void comprobar(boolean valor,String mensaje) {
        if(!valor) { throw new AssertionError(mensaje); }comprobaciones++;
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
    private static Map<String,String> reserva(String curso,String tutor,Horario h,String tema) {
        return Map.of("curso",curso,"tutor",tutor,"horario",ServicioWeb.horarioId(h),"tema",tema);
    }
    private static void probarRegistro() throws Exception {
        var carpeta=Files.createTempDirectory("tutorias-registro-");
        BaseDatos db=new BaseDatos("jdbc:h2:"+carpeta.resolve("db")+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE","sa","");
        RepositorioTutorias repo=new RepositorioTutorias(db);
        Map<String,String> datos=Map.of("nombre","Nueva estudiante","carnet","E100","correo","nueva@example.com","anio","2","clave","Registro2026!","confirmacion","Registro2026!");
        try(WebMain app=new WebMain(db,0,true)) {
            app.iniciar();Cliente c=new Cliente(app.puerto());
            comprobar(c.get("/api/registro-estudiante").statusCode()==405,"Registro solo admite POST");
            var invalido=new LinkedHashMap<>(datos);invalido.put("csrf","incorrecto");
            comprobar(c.post("/api/registro-estudiante",invalido).statusCode()==403,"Registro exige CSRF válido");
            for(var cambio:new String[][]{{"nombre"," "},{"nombre","x".repeat(201)},{"carnet"," "},{"carnet","E 100"},{"carnet","E".repeat(81)},{"correo","invalido"},{"correo","a".repeat(201)+"@example.com"},{"anio","0"},{"anio","11"},{"anio","1.5"},{"confirmacion","otra"},{"carnet","P001"}}) {
                invalido=new LinkedHashMap<>(datos);invalido.put(cambio[0],cambio[1]);
                comprobar(c.post("/api/registro-estudiante",invalido).statusCode()==400,"Registro rechaza campo inválido: "+cambio[0]);
            }
            for(String clave:new String[]{"corta","x".repeat(129)}) {
                invalido=new LinkedHashMap<>(datos);invalido.put("clave",clave);invalido.put("confirmacion",clave);
                comprobar(c.post("/api/registro-estudiante",invalido).statusCode()==400,"Registro valida longitud de contraseña");
            }
            invalido=new LinkedHashMap<>(datos);invalido.remove("confirmacion");
            comprobar(c.post("/api/registro-estudiante",invalido).statusCode()==400,"Registro requiere confirmar la contraseña");
            comprobar(repo.cargar().sistema().getEstudiantes().size()==2,"Errores no crean perfiles parciales");
            comprobar(new Seguridad(db).autenticar("estudiante","E100","Registro2026!")==null,"Errores no crean cuentas parciales");
            var valido=new LinkedHashMap<>(datos);valido.put("rol","coordinador");valido.put("carnet"," E100 ");
            comprobar(c.post("/api/registro-estudiante",valido).statusCode()==201,"Estudiante se registra sin coordinación");
            comprobar(c.get("/api/estado").statusCode()==401,"Registro permite iniciar sesión por el formulario habitual");
            comprobar(new Seguridad(db).autenticar("coordinador","E100","Registro2026!")==null && new Seguridad(db).autenticar("tutor","E100","Registro2026!")==null,"El registro público nunca crea perfiles privilegiados");
            invalido=new LinkedHashMap<>(datos);invalido.put("nombre","Sobrescribir");invalido.put("clave","OtraClave2026!");invalido.put("confirmacion","OtraClave2026!");
            comprobar(c.post("/api/registro-estudiante",invalido).statusCode()==400,"Carné duplicado no reemplaza la cuenta");
            comprobar(ServicioWeb.estudiante(repo.cargar().sistema(),"E100").getNombre().equals("Nueva estudiante"),"Duplicado conserva el perfil original");
            var acceso=c.post("/api/login",Map.of("rol","estudiante","carnet","E100","clave","Registro2026!"));
            comprobar(acceso.statusCode()==200,"Nueva cuenta inicia sesión con la contraseña elegida");c.csrf=campo(acceso.body(),"csrf");
            comprobar(c.get("/api/estado").body().contains("Nueva estudiante"),"Nueva cuenta accede a su espacio");
            comprobar(c.post("/api/registro-estudiante",datos).statusCode()==403,"El registro público requiere sesión anónima");
            c.accion("curso",403,Map.of("codigo","X","nombre","Sin privilegios"));
            var horario=ServicioWeb.tutor(repo.cargar().sistema(),"T001").getDisponibilidad().get(0);
            c.accion("solicitar",200,reserva("CC2006","T001",horario,"Primera tutoría de cuenta nueva"));
            Cliente coor=new Cliente(app.puerto());coor.login("coordinador","P001");
            comprobar(coor.get("/api/estado").body().contains("Nueva estudiante"),"Coordinación puede consultar al estudiante registrado");
            coor.accion("estudiante",200,Map.of("carnet","E101","nombre","Alta por coordinación","correo","coord@example.com","anio","1","clave","Tutor2026!"));
            Cliente publico=new Cliente(app.puerto());invalido=new LinkedHashMap<>(datos);invalido.put("carnet","E101");
            comprobar(publico.post("/api/registro-estudiante",invalido).statusCode()==400,"Registro no reemplaza un alta de coordinación");
            // Dos envíos simultáneos del mismo carné producen una sola cuenta completa.
            Cliente otro=new Cliente(app.puerto());var simultaneo=new LinkedHashMap<>(datos);simultaneo.put("carnet","E102");
            var a=CompletableFuture.supplyAsync(()->{try{return publico.post("/api/registro-estudiante",simultaneo).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
            var b=CompletableFuture.supplyAsync(()->{try{return otro.post("/api/registro-estudiante",simultaneo).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
            int ra=a.join(),rb=b.join();comprobar((ra==201&&rb==400)||(ra==400&&rb==201),"Registro simultáneo no duplica estudiantes");
            try(var conexion=db.conectar();var consulta=conexion.createStatement();var filas=consulta.executeQuery("SELECT hash,salt FROM cuentas WHERE carnet='E100' AND rol='estudiante'")) {
                comprobar(filas.next() && !filas.getString("hash").equals("Registro2026!") && !filas.getString("salt").isBlank(),"Registro conserva contraseñas protegidas");
            }
        }
        try(WebMain app=new WebMain(db,0,true)) {
            app.iniciar();Cliente c=new Cliente(app.puerto());
            comprobar(c.post("/api/login",Map.of("rol","estudiante","carnet","E100","clave","Registro2026!")).statusCode()==200,"Registro persiste después de reiniciar");
            comprobar(repo.cargar().sistema().getEstudiantes().size()==5,"Reinicio no duplica altas ni ejemplos");
            for(var cuenta:new String[][]{{"estudiante","E001"},{"estudiante","E002"},{"tutor","T001"},{"tutor","T002"},{"coordinador","P001"}}) {
                new Cliente(app.puerto()).login(cuenta[0],cuenta[1]);
            }
        }
    }
    public static void main(String[] args) throws Exception {
        var carpeta=Files.createTempDirectory("tutorias-pruebas-");
        BaseDatos db=new BaseDatos("jdbc:h2:"+carpeta.resolve("pruebas").toAbsolutePath()+";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE","sa","");
        RepositorioTutorias repo=new RepositorioTutorias(db);
        try(WebMain app=new WebMain(db,0,true)) {
            app.iniciar();int port=app.puerto();Cliente anon=new Cliente(port);
            comprobar(anon.get("/api/estado").statusCode()==401,"Estado privado requiere login");
            comprobar(anon.get("/sql/schema.sql").statusCode()==404,"Archivos internos no son públicos");
            comprobar(anon.get("/").headers().firstValue("Content-Security-Policy").orElse("").contains("script-src 'self'"),"CSP activa");
            comprobar(anon.post("/api/login",Map.of("rol","estudiante","carnet","E001","clave","incorrecta")).statusCode()==401,"Clave incorrecta rechazada");
            Cliente ana=new Cliente(port),luis=new Cliente(port),maria=new Cliente(port),diego=new Cliente(port),coor=new Cliente(port);
            ana.login("estudiante","E001");luis.login("estudiante","E002");maria.login("tutor","T001");diego.login("tutor","T002");coor.login("coordinador","P001");
            var s=repo.cargar().sistema();var hm=ServicioWeb.tutor(s,"T001").getDisponibilidad();var hd=ServicioWeb.tutor(s,"T002").getDisponibilidad();
            comprobar(ana.get("/api/estado").body().contains("Programación Orientada a Objetos"),"Curso visible");
            ana.accion("curso",403,Map.of("codigo","X","nombre","No autorizado"));
            comprobar(ana.post("/api/accion/solicitar",Map.of("csrf","alterado")).statusCode()==403,"CSRF rechazado");
            ana.accion("solicitar",200,reserva("CC2006","T001",hm.get(0),"Herencia <script>alert(1)</script>"));
            comprobar(!luis.get("/api/estado").body().contains("Herencia <script>"),"Otro estudiante no lee datos privados");
            comprobar(!diego.get("/api/estado").body().contains("Herencia <script>"),"Otro tutor no lee datos privados");
            luis.accion("cancelar",400,Map.of("solicitud","1"));diego.accion("aceptar",400,Map.of("solicitud","1"));
            ana.accion("preguntar",200,Map.of("solicitud","1","contenido","¿Cómo se usa super?"));
            comprobar(maria.get("/api/estado").body().contains("¿Cómo se usa super?"),"Tutor lee preguntas");
            maria.accion("aceptar",200,Map.of("solicitud","1"));maria.accion("aceptar",400,Map.of("solicitud","1"));
            maria.accion("finalizar",400,Map.of("sesion","1","temas","Herencia","notas",""));
            comprobar(repo.cargar().sistema().getSolicitudes().get(0).getEstado()==EstadoSolicitud.CONFIRMADA,"Error no guarda estado parcial");
            maria.accion("finalizar",200,Map.of("sesion","1","temas","Herencia y constructores","notas","Resolvió el ejercicio","proximo","Polimorfismo"));
            maria.accion("finalizar",400,Map.of("sesion","1","temas","x","notas","x"));
            s=repo.cargar().sistema();comprobar(ServicioWeb.tutor(s,"T001").getRegistroHorasBeca().getHorasAcumuladas()==1,"Una hora beca persistida");
            comprobar(ServicioWeb.estudiante(s,"E001").getHistorialTutorias().size()==1,"Historial del estudiante persistido");
            comprobar(coor.get("/api/estado").body().contains("Polimorfismo"),"Coordinación ve progreso y próximo tema");
            ana.accion("preguntar",400,Map.of("solicitud","1","contenido","Tarde"));
            luis.accion("solicitar",200,reserva("MM101","T002",hd.get(0),"Integrales"));diego.accion("aceptar",200,Map.of("solicitud","2"));
            diego.accion("finalizar",200,Map.of("sesion","2","temas","Integración","notas","Completado"));
            s=repo.cargar().sistema();comprobar(ServicioWeb.tutor(s,"T002").getRemuneracionAcumulada()==60,"Q60 guardados por 90 minutos");
            comprobar(ServicioWeb.tutor(s,"T002").getRegistroHorasBeca().getHorasAcumuladas()==0,"Tutor remunerado no suma beca");
            ana.accion("solicitar",200,reserva("CC2006","T001",hm.get(1),"Cancelación"));maria.accion("aceptar",200,Map.of("solicitud","3"));ana.accion("cancelar",200,Map.of("solicitud","3"));
            luis.accion("solicitar",200,reserva("CC2006","T001",hm.get(1),"Nueva reserva"));
            s=repo.cargar().sistema();comprobar(s.getSolicitudes().get(2).getEstado()==EstadoSolicitud.CANCELADA,"Cancelada conserva estado al recargar");
            comprobar(!s.getSolicitudes().get(3).getHorario().isDisponible(),"La reserva posterior no queda liberada por la cancelada");
            maria.accion("finalizar",400,Map.of("sesion","3","temas","x","notas","x"));
            var concurrente=reserva("CC2006","T001",hm.get(2),"Concurrencia");
            var a=CompletableFuture.supplyAsync(()->{try{return ana.post("/api/accion/solicitar",concurrente).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
            var b=CompletableFuture.supplyAsync(()->{try{return luis.post("/api/accion/solicitar",concurrente).statusCode();}catch(Exception e){throw new RuntimeException(e);}});
            int ra=a.join(),rb=b.join();comprobar((ra==200&&rb==400)||(ra==400&&rb==200),"Solo una reserva simultánea obtiene el horario");
            coor.accion("curso",200,Map.of("codigo","CC999","nombre","Pruebas web"));
            coor.accion("tutor",200,Map.of("carnet","T009","nombre","Tutor nuevo","correo","nuevo@example.com","anio","4","tarifa","0","beca","on","clave","Tutor2026!"));
            coor.accion("estudiante",200,Map.of("carnet","E009","nombre","Estudiante nuevo","correo","alumno@example.com","anio","2","clave","Tutor2026!"));
            coor.accion("aprobar",200,Map.of("tutor","T009","curso","CC999"));
            Cliente nuevo=new Cliente(port);nuevo.login("tutor","T009");
            nuevo.accion("horario",400,Map.of("fecha","2026-02-29","inicio","10:00","fin","11:00"));
            nuevo.accion("horario",200,Map.of("fecha","2026-12-01","inicio","10:00","fin","11:00"));
            nuevo.accion("horario",400,Map.of("fecha","2026-12-01","inicio","10:30","fin","12:00"));
            comprobar(ana.get("/api/estado").body().contains("Tutor nuevo"),"Aprobación y disponibilidad aparecen en oferta");
            // Profesor imparte usando su cuenta de coordinación, con el mismo flujo de tutor.
            var hp=repo.cargar().sistema().getCoordinador().getPerfilTutor().getDisponibilidad().get(0);
            int next=repo.cargar().sistema().getSolicitudes().size()+1;
            luis.accion("solicitar",200,reserva("CC5001","P001",hp,"Consulta avanzada"));coor.accion("aceptar",200,Map.of("solicitud",""+next));
            int ses=repo.cargar().sistema().getSesiones().size();
            coor.accion("finalizar",200,Map.of("sesion",""+ses,"temas","Grafos","notas","Buen avance"));
            comprobar(repo.cargar().sistema().getCoordinador().getTutoriasImpartidas().size()==1,"Historial del profesor recuperado");
            comprobar(coor.get("/api/estado").body().contains("Consulta avanzada"),"Demanda conserva temas");
            var viejo=repo.cargar();repo.guardar(repo.cargar(),null);
            boolean conflicto=false;try{repo.guardar(viejo,null);}catch(java.sql.SQLException e){conflicto="40001".equals(e.getSQLState());}comprobar(conflicto,"Revisión evita sobreescritura entre procesos");
            String before=ana.get("/api/estado").body();var cambio=ana.post("/api/clave",Map.of("actual","Tutor2026!","nueva","MiNuevaClave2026"));comprobar(cambio.statusCode()==200,"Cambio de contraseña");
            ana.csrf=campo(cambio.body(),"csrf");comprobar(ana.get("/api/estado").body().equals(before),"Cambio de clave mantiene datos");
            comprobar(ana.post("/api/logout",Map.of()).statusCode()==200,"Cierre de sesión");comprobar(ana.get("/api/estado").statusCode()==401,"Cookie cerrada no accede");
        }
        // Nuevo servidor y nueva conexión: el archivo SQL conserva el trabajo.
        try(WebMain reinicio=new WebMain(db,0,true)) {
            reinicio.iniciar();Cliente c=new Cliente(reinicio.puerto());c.login("coordinador","P001");
            String datos=c.get("/api/estado").body();comprobar(datos.contains("Resolvió el ejercicio")&&datos.contains("Tutor nuevo")&&datos.contains("¿Cómo se usa super?"),"Reinicio conserva registros, notas y preguntas");
            comprobar(repo.cargar().sistema().getSesiones().size()==4,"No se duplican datos iniciales al reiniciar");
            Cliente alumno=new Cliente(reinicio.puerto());var r=alumno.post("/api/login",Map.of("rol","estudiante","carnet","E001","clave","MiNuevaClave2026"));comprobar(r.statusCode()==200,"Contraseña modificada persiste al reiniciar");
        }
        probarRegistro();
        System.out.println("CORRECTO: "+comprobaciones+" comprobaciones web, seguridad y persistencia superadas.");
        System.out.println("Base aislada de pruebas: "+carpeta);
    }
}
